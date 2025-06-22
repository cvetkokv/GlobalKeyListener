package com.example.globalkey;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NativeKeyboardHook {
    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            boolean isWindows = os.contains("win");
            boolean isLinux = os.contains("linux");

            String extension = isWindows ? ".dll" : isLinux ? ".so" : "";
            String fileName = isWindows ? "keyboard_hook.dll" : isLinux ? "libkeyboard_hook.so" : null;

            if (fileName == null) {
                throw new UnsupportedOperationException("Unsupported OS: " + os);
            }

            String resourcePath = String.format("/native/%s/%s", os.contains("win") ? "windows" : "linux", fileName);
            System.out.println("Loading native lib from: " + resourcePath);

            var in = NativeKeyboardHook.class.getResourceAsStream(resourcePath);
            if (in == null) {
                throw new IllegalStateException("Native lib not found in resources: " + resourcePath);
            }

            var tempLib = Files.createTempFile("keyboard_hook", extension);
            Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);
            in.close();

            System.load(tempLib.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load native hook library", e);
        }
    }


    private static final NativeKeyboardHook instance = new NativeKeyboardHook();
    private static boolean hookStarted = false;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private NativeKeyboardHook() {
    }

    public static void startHookOnce() {
        if (!hookStarted) {
            hookStarted = true;
            executor.submit(instance::startHook);
        }
    }

    public static int getDroppedEvents() {
        return instance.getDroppedEventsNative();
    }

    public native void startHook();

    public native int getDroppedEventsNative();
}