#include <jni.h>
#include <thread>
#include <atomic>
#include <chrono>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>
#include <boost/lockfree/queue.hpp>
#include <iostream>
#include "com_example_globalkey_NativeKeyboardHook.h"

JavaVM* jvm = nullptr;
jobject globalObject = nullptr;
std::atomic<int> droppedEvents = 0;
std::atomic<bool> running(true);

struct KeyEventData {
    int keycode;
    jint eventType; // 0 = press, 1 = release
};

boost::lockfree::queue<KeyEventData> eventQueue(1024);

Display* display = nullptr;
Window root;

void captureKeys() {
    display = XOpenDisplay(nullptr);
    if (!display) {
        std::cerr << "❌ Unable to open X display" << std::endl;
        return;
    }

    root = DefaultRootWindow(display);
    XSelectInput(display, root, KeyPressMask | KeyReleaseMask);

    while (running.load()) {
        while (XPending(display)) {
            XEvent ev;
            XNextEvent(display, &ev);
            if (ev.type == KeyPress || ev.type == KeyRelease) {
                KeyEventData data;
                data.keycode = ev.xkey.keycode;
                data.eventType = (ev.type == KeyRelease) ? 1 : 0;
                if (!eventQueue.push(data)) {
                    droppedEvents++;
                }
            }
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
    XCloseDisplay(display);
}

void dispatchEvents() {
    JNIEnv* env;
    if (jvm->AttachCurrentThread((void**)&env, nullptr) != 0) return;
    jclass cls = env->FindClass("com/example/globalkey/NativeKeyboardBridge");
    if (!cls) return;
    jmethodID method = env->GetStaticMethodID(cls, "dispatchFromNative", "(IIZZZ)V");
    if (!method) return;

    KeyEventData data;
    while (running.load()) {
        while (eventQueue.pop(data)) {
            env->CallStaticVoidMethod(cls, method, data.keycode, data.eventType, false, false, false);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_example_globalkey_NativeKeyboardHook_startHook(JNIEnv* env, jobject obj) {
    std::cout << "🟢 Linux startHook() called" << std::endl;
    env->GetJavaVM(&jvm);
    globalObject = env->NewGlobalRef(obj);

    std::thread(captureKeys).detach();
    std::thread(dispatchEvents).detach();
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_globalkey_NativeKeyboardHook_getDroppedEventsNative(JNIEnv*, jobject) {
    return droppedEvents.load();
}
