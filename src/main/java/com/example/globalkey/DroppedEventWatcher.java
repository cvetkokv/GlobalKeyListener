package com.example.globalkey;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

public class DroppedEventWatcher {

    private static int lastKnownValue = -1;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void start(IntConsumer onDropChanged) {
        executor.scheduleAtFixedRate(() -> {
            int current = NativeKeyboardHook.getDroppedEvents();
            if (current != lastKnownValue) {
                lastKnownValue = current;
                onDropChanged.accept(current);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        executor.shutdownNow();
    }

    public static void reset() {
        lastKnownValue = -1;
    }
}