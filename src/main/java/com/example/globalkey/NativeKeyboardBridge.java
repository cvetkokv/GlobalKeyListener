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

/** * NativeKeyboardBridge is a bridge for handling native keyboard events in a JavaFX application.
 * It allows subscribing to key events and dispatches them to registered listeners.
 * It supports key pressed, released, and typed events, and can handle custom key sequences.
 * * The bridge uses a background thread to process events from a blocking queue, ensuring
 * that key events are handled asynchronously without blocking the JavaFX application thread.
 * * It also provides methods to set sequence rules, validation, and completion actions for key sequences.
 * * The VK_MAP is initialized based on the operating system to map native key codes to JavaFX KeyCode.
 * * This class is designed to be used in a JavaFX application to handle global keyboard events
 * and manage key sequences effectively.
 */
import static javafx.scene.input.KeyEvent.*;

public class NativeKeyboardBridge {

    private static final Set<Consumer<KeyEvent>> allSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> pressedSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> releasedSubscribers = new CopyOnWriteArraySet<>();
    private static final Set<Consumer<KeyEvent>> typedSubscribers = new CopyOnWriteArraySet<>();

    private static final BlockingQueue<KeyEvent> eventQueue = new LinkedBlockingQueue<>();

    /**
     * Initializes the NativeKeyboardBridge and starts the event dispatcher thread.
     * This static block runs once when the class is loaded, setting up the necessary
     * infrastructure to handle key events asynchronously.
     */
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

    /**
     * Subscribes a listener to receive all key events.
     *
     * @param listener the listener to subscribe
     */
    public static void subscribe(Consumer<KeyEvent> listener) {
        allSubscribers.add(listener);
    }

    /**
     * Unsubscribes a listener from receiving all key events.
     *
     * @param listener the listener to unsubscribe
     */
    public static void unsubscribe(Consumer<KeyEvent> listener) {
        allSubscribers.remove(listener);
    }

    /**
     * Subscribes a listener to receive key pressed events.
     *
     * @param listener the listener to subscribe
     */
    public static void subscribePressed(Consumer<KeyEvent> listener) {
        pressedSubscribers.add(listener);
    }

    /**
     * Unsubscribes a listener from receiving key pressed events.
     *
     * @param listener the listener to unsubscribe
     */
    public static void unsubscribePressed(Consumer<KeyEvent> listener) {
        pressedSubscribers.remove(listener);
    }

    /**
     * Subscribes a listener to receive key released events.
     *
     * @param listener the listener to subscribe
     */
    public static void subscribeReleased(Consumer<KeyEvent> listener) {
        releasedSubscribers.add(listener);
    }

    /**
     * Unsubscribes a listener from receiving key released events.
     *
     * @param listener the listener to unsubscribe
     */
    public static void unsubscribeReleased(Consumer<KeyEvent> listener) {
        releasedSubscribers.remove(listener);
    }

    /**
     * Subscribes a listener to receive key typed events.
     *
     * @param listener the listener to subscribe
     */
    public static void subscribeTyped(Consumer<KeyEvent> listener) {
        typedSubscribers.add(listener);
    }

    /**
     * Unsubscribes a listener from receiving key typed events.
     *
     * @param listener the listener to unsubscribe
     */
    public static void unsubscribeTyped(Consumer<KeyEvent> listener) {
        typedSubscribers.remove(listener);
    }

    /**
     * Sets the sequence rules for matching key sequences.
     *
     * @param rules the list of predicates to match key events against
     */
    public static void setSequenceRules(List<Predicate<KeyEvent>> rules) {
        KeySequenceManager.getInstance().setMatchRules(rules);
    }

    /**
     * Sets the rule for determining when a key sequence is done.
     *
     * @param doneRule the predicate to determine if a key event indicates completion of a sequence
     */
    public static void setDoneRule(Predicate<KeyEvent> doneRule) {
        KeySequenceManager.getInstance().setDonePredicate(doneRule);
    }

    /**
     * Sets a validation predicate for key sequences.
     *
     * @param validator the predicate to validate completed key sequences
     */
    public static void setSequenceValidation(Predicate<String> validator) {
        KeySequenceManager.getInstance().setSequenceValidation(validator);
    }

    /**
     * Sets a listener that will be called when a key sequence is finalized.
     *
     * @param onComplete the consumer to call with the completed key sequence
     */
    public static void listenForFinalizedSequence(Consumer<String> onComplete) {
        KeySequenceManager.getInstance().setOnSequenceComplete(onComplete);
    }

    /**
     * Sets whether the key sequence manager should block processing of key events.
     *
     * @param blocker true to block key event processing, false to allow it
     */
    public static void setBlocker(boolean blocker) {
        KeySequenceManager.getInstance().setBlocker(() -> blocker);
    }

    /**
     * Sets the timeout in milliseconds for key sequences.
     *
     * @param millis the timeout duration in milliseconds
     */
    public static void setSequenceTimeoutMillis(long millis) {
        KeySequenceManager.getInstance().setTimeoutMillis(millis);
    }

    /**
     * Dispatches a key event from native code to the JavaFX application.
     *
     * @param vkCode the virtual key code of the key event
     * @param eventType the type of the key event (0 for pressed, 1 for released, 2 for typed)
     * @param shift true if the Shift key is pressed, false otherwise
     * @param ctrl true if the Control key is pressed, false otherwise
     * @param alt true if the Alt key is pressed, false otherwise
     */
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

    private static void initWindowsVKMap() {
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
        VK_MAP.put(160, KeyCode.SHIFT);
        VK_MAP.put(161, KeyCode.SHIFT);
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

    private static void initLinuxVKMap() {
        // Uppercase letters A-Z
        for (char c = 'A'; c <= 'Z'; c++) {
            VK_MAP.put((int) c, KeyCode.valueOf(String.valueOf(c)));
        }

        // Lowercase letters a-z (useful for barcode scanners)
        for (char c = 'a'; c <= 'z'; c++) {
            VK_MAP.put((int) c, KeyCode.valueOf(String.valueOf(Character.toUpperCase(c))));
        }

        // Digits 0–9
        for (char c = '0'; c <= '9'; c++) {
            VK_MAP.put((int) c, KeyCode.getKeyCode(String.valueOf(c)));
        }

        // Function keys F1–F12
        for (int i = 1; i <= 12; i++) {
            VK_MAP.put(0xFFBE + (i - 1), KeyCode.valueOf("F" + i));
        }

        // Common symbols and punctuation
        VK_MAP.put(0x0020, KeyCode.SPACE);
        VK_MAP.put(0x002C, KeyCode.COMMA);
        VK_MAP.put(0x002E, KeyCode.PERIOD);
        VK_MAP.put(0x002D, KeyCode.MINUS);
        VK_MAP.put(0x003D, KeyCode.EQUALS);
        VK_MAP.put(0x005B, KeyCode.OPEN_BRACKET);
        VK_MAP.put(0x005D, KeyCode.CLOSE_BRACKET);
        VK_MAP.put(0x005C, KeyCode.BACK_SLASH);
        VK_MAP.put(0x003B, KeyCode.SEMICOLON);
        VK_MAP.put(0x0027, KeyCode.QUOTE);
        VK_MAP.put(0x002F, KeyCode.SLASH);
        VK_MAP.put(0x0060, KeyCode.BACK_QUOTE);

        // Navigation and editing
        VK_MAP.put(0xFF0D, KeyCode.ENTER);
        VK_MAP.put(0xFF08, KeyCode.BACK_SPACE);
        VK_MAP.put(0xFF09, KeyCode.TAB);
        VK_MAP.put(0xFF1B, KeyCode.ESCAPE);
        VK_MAP.put(0xFF63, KeyCode.INSERT);
        VK_MAP.put(0xFFFF, KeyCode.DELETE);
        VK_MAP.put(0xFF50, KeyCode.HOME);
        VK_MAP.put(0xFF57, KeyCode.END);
        VK_MAP.put(0xFF55, KeyCode.PAGE_UP);
        VK_MAP.put(0xFF56, KeyCode.PAGE_DOWN);
        VK_MAP.put(0xFF51, KeyCode.LEFT);
        VK_MAP.put(0xFF52, KeyCode.UP);
        VK_MAP.put(0xFF53, KeyCode.RIGHT);
        VK_MAP.put(0xFF54, KeyCode.DOWN);

        // Modifiers
        VK_MAP.put(0xFFE1, KeyCode.SHIFT);      // Shift_L
        VK_MAP.put(0xFFE2, KeyCode.SHIFT);      // Shift_R
        VK_MAP.put(0xFFE3, KeyCode.CONTROL);    // Control_L
        VK_MAP.put(0xFFE4, KeyCode.CONTROL);    // Control_R
        VK_MAP.put(0xFFE9, KeyCode.ALT);        // Alt_L
        VK_MAP.put(0xFFEA, KeyCode.ALT);        // Alt_R

        // Numpad keys
        VK_MAP.put(0xFFB0, KeyCode.NUMPAD0);
        VK_MAP.put(0xFFB1, KeyCode.NUMPAD1);
        VK_MAP.put(0xFFB2, KeyCode.NUMPAD2);
        VK_MAP.put(0xFFB3, KeyCode.NUMPAD3);
        VK_MAP.put(0xFFB4, KeyCode.NUMPAD4);
        VK_MAP.put(0xFFB5, KeyCode.NUMPAD5);
        VK_MAP.put(0xFFB6, KeyCode.NUMPAD6);
        VK_MAP.put(0xFFB7, KeyCode.NUMPAD7);
        VK_MAP.put(0xFFB8, KeyCode.NUMPAD8);
        VK_MAP.put(0xFFB9, KeyCode.NUMPAD9);
        VK_MAP.put(0xFFAA, KeyCode.MULTIPLY);
        VK_MAP.put(0xFFAB, KeyCode.PLUS);
        VK_MAP.put(0xFFAD, KeyCode.MINUS);
        VK_MAP.put(0xFFAE, KeyCode.DECIMAL);
        VK_MAP.put(0xFFAF, KeyCode.DIVIDE);

        // Lock keys
        VK_MAP.put(0xFFE5, KeyCode.CAPS);
        VK_MAP.put(0xFF7F, KeyCode.NUM_LOCK);
        VK_MAP.put(0xFF14, KeyCode.SCROLL_LOCK);
    }

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.out.println("Initializing VK_MAP for Windows");
            initWindowsVKMap();
        } else if (os.contains("linux")) {
            System.out.println("Initializing VK_MAP for Linux");
            initLinuxVKMap();
        } else {
            System.err.println("❌ Unsupported OS for VK_MAP: " + os);
        }
    }
}