package com.example.globalkey;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static javafx.scene.input.KeyEvent.*;

public class NativeKeyboardBridge {

    private static final Set<Consumer<KeyEvent>> allSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> pressedSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> releasedSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> typedSubscribers = new CopyOnWriteArraySet<>();

    private static final BlockingQueue<KeyEvent> eventQueue = new LinkedBlockingQueue<>();

    static {
        Thread dispatcherThread = new Thread(() -> {
            while (true) {
                try {
                    KeyEvent fxEvent = eventQueue.take();
                    allSubscribers.forEach(sub -> sub.accept(fxEvent));
                    switch (fxEvent.getEventType().getName()) {
                        case "KEY_PRESSED" -> pressedSubscribers.forEach(sub -> sub.accept(fxEvent));
                        case "KEY_RELEASED" -> releasedSubscribers.forEach(sub -> sub.accept(fxEvent));
                        case "KEY_TYPED" -> typedSubscribers.forEach(sub -> sub.accept(fxEvent));
                    }
                    KeySequenceManager.getInstance().process(fxEvent);
                } catch (InterruptedException ignored) {
                }
            }
        });
        dispatcherThread.setDaemon(true);
        dispatcherThread.start();

        // JVM shutdown cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            releasedSubscribers.clear();
            pressedSubscribers.clear();
            typedSubscribers.clear();
            allSubscribers.clear();
        }));
    }

    public static void subscribe(Consumer<KeyEvent> listener) {
        allSubscribers.add(listener);
    }

    public static void unsubscribe(Consumer<KeyEvent> listener) {
        allSubscribers.remove(listener);
    }

    public static void subscribePressed(Consumer<KeyEvent> listener) {
        pressedSubscribers.add(listener);
    }

    public static void unsubscribePressed(Consumer<KeyEvent> listener) {
        pressedSubscribers.remove(listener);
    }

    public static void subscribeReleased(Consumer<KeyEvent> listener) {
        releasedSubscribers.add(listener);
    }

    public static void unsubscribeReleased(Consumer<KeyEvent> listener) {
        releasedSubscribers.remove(listener);
    }

    public static void subscribeTyped(Consumer<KeyEvent> listener) {
        typedSubscribers.add(listener);
    }

    public static void unsubscribeTyped(Consumer<KeyEvent> listener) {
        typedSubscribers.remove(listener);
    }

    public static void setSequenceRules(List<Predicate<KeyEvent>> rules) {
        KeySequenceManager.getInstance().setMatchRules(rules);
    }

    public static void setDoneRule(Predicate<KeyEvent> doneRule) {
        KeySequenceManager.getInstance().setDonePredicate(doneRule);
    }

    public static void setSequenceValidation(Predicate<String> validator) {
        KeySequenceManager.getInstance().setSequenceValidation(validator);
    }

    public static void listenForFinalizedSequence(Consumer<String> onComplete) {
        KeySequenceManager.getInstance().setOnSequenceComplete(onComplete);
    }

    public static void setBlocker(boolean blocker) {
        KeySequenceManager.getInstance().setBlocker(() -> blocker);
    }

    public static void setSequenceTimeoutMillis(long millis) {
        KeySequenceManager.getInstance().setTimeoutMillis(millis);
    }

    public static void dispatchFromNative(int vkCode, int eventType, boolean shift, boolean ctrl, boolean alt) {
        KeyCode keyCode = VK_MAP.get(vkCode);
        if (keyCode == null) {
            System.out.println("❌ Unmapped VK code: " + vkCode);
            return;
        }

        javafx.event.EventType<KeyEvent> fxType = switch (eventType) {
            case 0 -> KEY_PRESSED;
            case 1 -> KEY_RELEASED;
            default -> KEY_TYPED;
        };

        String text = fxType == KEY_TYPED ? keyCode.getChar() : "";

        KeyEvent fxEvent = new KeyEvent(
                fxType,
                text,
                text,
                keyCode,
                shift, ctrl, alt, false
        );

        boolean ignored = eventQueue.offer(fxEvent);
    }

    private static final Map<Integer, KeyCode> VK_MAP = new HashMap<>();

    static {
        for (KeyCode kc : KeyCode.values()) {
            VK_MAP.putIfAbsent(kc.getCode(), kc);
        }

        VK_MAP.put(13, KeyCode.ENTER);
        VK_MAP.put(108, KeyCode.ENTER);
        VK_MAP.put(8, KeyCode.BACK_SPACE);
        VK_MAP.put(9, KeyCode.TAB);
        VK_MAP.put(20, KeyCode.CAPS);
        VK_MAP.put(27, KeyCode.ESCAPE);
        VK_MAP.put(32, KeyCode.SPACE);
        VK_MAP.put(37, KeyCode.LEFT);
        VK_MAP.put(38, KeyCode.UP);
        VK_MAP.put(39, KeyCode.RIGHT);
        VK_MAP.put(40, KeyCode.DOWN);
        VK_MAP.put(45, KeyCode.INSERT);
        VK_MAP.put(46, KeyCode.DELETE);
        VK_MAP.put(36, KeyCode.HOME);
        VK_MAP.put(35, KeyCode.END);
        VK_MAP.put(33, KeyCode.PAGE_UP);
        VK_MAP.put(34, KeyCode.PAGE_DOWN);
        VK_MAP.put(144, KeyCode.NUM_LOCK);
        VK_MAP.put(145, KeyCode.SCROLL_LOCK);
        VK_MAP.put(16, KeyCode.SHIFT);
        VK_MAP.put(162, KeyCode.CONTROL);
        VK_MAP.put(163, KeyCode.CONTROL);
        VK_MAP.put(17, KeyCode.CONTROL);
        VK_MAP.put(164, KeyCode.ALT);
        VK_MAP.put(165, KeyCode.ALT);
        VK_MAP.put(18, KeyCode.ALT);
        VK_MAP.put(186, KeyCode.SEMICOLON);
        VK_MAP.put(187, KeyCode.EQUALS);
        VK_MAP.put(188, KeyCode.COMMA);
        VK_MAP.put(189, KeyCode.MINUS);
        VK_MAP.put(190, KeyCode.PERIOD);
        VK_MAP.put(191, KeyCode.SLASH);
        VK_MAP.put(192, KeyCode.BACK_QUOTE);
        VK_MAP.put(219, KeyCode.OPEN_BRACKET);
        VK_MAP.put(220, KeyCode.BACK_SLASH);
        VK_MAP.put(221, KeyCode.CLOSE_BRACKET);
        VK_MAP.put(222, KeyCode.QUOTE);

        VK_MAP.put(96, KeyCode.NUMPAD0);
        VK_MAP.put(97, KeyCode.NUMPAD1);
        VK_MAP.put(98, KeyCode.NUMPAD2);
        VK_MAP.put(99, KeyCode.NUMPAD3);
        VK_MAP.put(100, KeyCode.NUMPAD4);
        VK_MAP.put(101, KeyCode.NUMPAD5);
        VK_MAP.put(102, KeyCode.NUMPAD6);
        VK_MAP.put(103, KeyCode.NUMPAD7);
        VK_MAP.put(104, KeyCode.NUMPAD8);
        VK_MAP.put(105, KeyCode.NUMPAD9);
        VK_MAP.put(106, KeyCode.MULTIPLY);
        VK_MAP.put(107, KeyCode.PLUS);
        VK_MAP.put(109, KeyCode.MINUS);
        VK_MAP.put(110, KeyCode.DECIMAL);
        VK_MAP.put(111, KeyCode.DIVIDE);

        for (int i = 1; i <= 12; i++) {
            VK_MAP.put(111 + i, KeyCode.valueOf("F" + i));
        }
    }
}