#include <windows.h>
#include <stdio.h>
#include <jni.h>
#include <thread>
#include <atomic>
#include <boost/lockfree/queue.hpp>
#include "com_example_globalkey_NativeKeyboardHook.h"

JavaVM* jvm = nullptr;
jobject globalObject = nullptr;
HHOOK hHook = NULL;

struct KeyEventData {
    DWORD vkCode;
    jint eventType;
    jboolean shift;
    jboolean ctrl;
    jboolean alt;
};

boost::lockfree::queue<KeyEventData> eventQueue(1024);
std::atomic<bool> running(true);
std::atomic<int> droppedEvents = 0;

void dispatchEvents() {
    JNIEnv* env;
    jvm->AttachCurrentThread((void**)&env, NULL);
    jclass cls = env->FindClass("com/example/globalkey/NativeKeyboardBridge");
    if (cls == NULL) return;
    jmethodID method = env->GetStaticMethodID(cls, "dispatchFromNative", "(IIZZZ)V");
    if (method == NULL) return;

    KeyEventData data;
    while (running.load()) {
        while (eventQueue.pop(data)) {
            env->CallStaticVoidMethod(cls, method, data.vkCode, data.eventType, data.shift, data.ctrl, data.alt);
        }
        Sleep(1);
    }
}

LRESULT CALLBACK KeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
    if (nCode == HC_ACTION) {
        KBDLLHOOKSTRUCT* keyInfo = (KBDLLHOOKSTRUCT*)lParam;
        DWORD vkCode = keyInfo->vkCode;

        KeyEventData event;
        event.vkCode = vkCode;
        event.eventType = (wParam == WM_KEYUP) ? 1 : 0;
        event.shift = (GetAsyncKeyState(VK_SHIFT) & 0x8000) != 0;
        event.ctrl  = (GetAsyncKeyState(VK_CONTROL) & 0x8000) != 0;
        event.alt   = (GetAsyncKeyState(VK_MENU) & 0x8000) != 0;

        if (!eventQueue.push(event)) {
            droppedEvents++;
        }
    }
    return CallNextHookEx(hHook, nCode, wParam, lParam);
}

JNIEXPORT void JNICALL Java_com_example_globalkey_NativeKeyboardHook_startHook(JNIEnv* env, jobject obj) {
    printf("🟢 Native method startHook() called\n");
    fflush(stdout);

    env->GetJavaVM(&jvm);
    globalObject = env->NewGlobalRef(obj);

    HMODULE hInstance = GetModuleHandle(NULL);
    hHook = SetWindowsHookEx(WH_KEYBOARD_LL, KeyboardProc, hInstance, 0);
    if (hHook == NULL) {
        printf("❌ Failed to install keyboard hook! Error: %lu\n", GetLastError());
        fflush(stdout);
        return;
    }

    printf("✅ Keyboard hook installed successfully\n");
    fflush(stdout);

    std::thread(dispatchEvents).detach();

    MSG msg;
    while (running.load()) {
        while (PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        Sleep(1);
    }

    UnhookWindowsHookEx(hHook);
    running.store(false);
}

JNIEXPORT jint JNICALL Java_com_example_globalkey_NativeKeyboardHook_getDroppedEventsNative(JNIEnv*, jobject) {
    return droppedEvents.load();
}