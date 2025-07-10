package com.example.globalkey;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NativeKeyboardHook is a utility class that loads a native library for keyboard hooking
 * and provides methods to start the hook and retrieve dropped events.
 * It supports both Windows and Linux platforms.
 */
public class NativeKeyboardHook {
    static {
        NativeLibraryLoader.load("keyboard_hook");
    }

    private static final NativeKeyboardHook instance = new NativeKeyboardHook();
    private static boolean hookStarted = false;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private NativeKeyboardHook() {
    }

    /**
     * Starts the keyboard hook in a separate thread if it hasn't been started yet.
     * This method ensures that the hook is only started once, even if called multiple times.
     */
    public static void startHookOnce() {
        if (!hookStarted) {
            hookStarted = true;
            executor.submit(instance::startHook);
        }
    }

    /**
     * Retrieves the number of dropped events since the last call.
     * This method is thread-safe and can be called at any time.
     *
     * @return the number of dropped events
     */
    public static int getDroppedEvents() {
        return instance.getDroppedEventsNative();
    }

    public native void startHook();

    public native int getDroppedEventsNative();
}