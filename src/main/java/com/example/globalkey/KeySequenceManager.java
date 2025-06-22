// KeySequenceManager.java
package com.example.globalkey;

import com.example.globalkey.api.KeySequenceBlocker;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class KeySequenceManager {

    public static final Map<KeyCode, String> KEY_TO_CHAR_MAP = Map.<KeyCode, String>ofEntries(
            Map.entry(KeyCode.DIGIT0, "0"), Map.entry(KeyCode.DIGIT1, "1"),
            Map.entry(KeyCode.DIGIT2, "2"), Map.entry(KeyCode.DIGIT3, "3"),
            Map.entry(KeyCode.DIGIT4, "4"), Map.entry(KeyCode.DIGIT5, "5"),
            Map.entry(KeyCode.DIGIT6, "6"), Map.entry(KeyCode.DIGIT7, "7"),
            Map.entry(KeyCode.DIGIT8, "8"), Map.entry(KeyCode.DIGIT9, "9"),

            Map.entry(KeyCode.NUMPAD0, "0"), Map.entry(KeyCode.NUMPAD1, "1"),
            Map.entry(KeyCode.NUMPAD2, "2"), Map.entry(KeyCode.NUMPAD3, "3"),
            Map.entry(KeyCode.NUMPAD4, "4"), Map.entry(KeyCode.NUMPAD5, "5"),
            Map.entry(KeyCode.NUMPAD6, "6"), Map.entry(KeyCode.NUMPAD7, "7"),
            Map.entry(KeyCode.NUMPAD8, "8"), Map.entry(KeyCode.NUMPAD9, "9"),

            Map.entry(KeyCode.A, "a"), Map.entry(KeyCode.B, "b"), Map.entry(KeyCode.C, "c"),
            Map.entry(KeyCode.D, "d"), Map.entry(KeyCode.E, "e"), Map.entry(KeyCode.F, "f"),
            Map.entry(KeyCode.G, "g"), Map.entry(KeyCode.H, "h"), Map.entry(KeyCode.I, "i"),
            Map.entry(KeyCode.J, "j"), Map.entry(KeyCode.K, "k"), Map.entry(KeyCode.L, "l"),
            Map.entry(KeyCode.M, "m"), Map.entry(KeyCode.N, "n"), Map.entry(KeyCode.O, "o"),
            Map.entry(KeyCode.P, "p"), Map.entry(KeyCode.Q, "q"), Map.entry(KeyCode.R, "r"),
            Map.entry(KeyCode.S, "s"), Map.entry(KeyCode.T, "t"), Map.entry(KeyCode.U, "u"),
            Map.entry(KeyCode.V, "v"), Map.entry(KeyCode.W, "w"), Map.entry(KeyCode.X, "x"),
            Map.entry(KeyCode.Y, "y"), Map.entry(KeyCode.Z, "z"),

            Map.entry(KeyCode.SPACE, " "), Map.entry(KeyCode.MINUS, "-"),
            Map.entry(KeyCode.EQUALS, "="), Map.entry(KeyCode.BACK_QUOTE, "`"),
            Map.entry(KeyCode.BACK_SLASH, "\\"), Map.entry(KeyCode.SEMICOLON, ";"),
            Map.entry(KeyCode.QUOTE, "'"), Map.entry(KeyCode.COMMA, ","),
            Map.entry(KeyCode.PERIOD, "."), Map.entry(KeyCode.SLASH, "/"),
            Map.entry(KeyCode.OPEN_BRACKET, "["), Map.entry(KeyCode.CLOSE_BRACKET, "]"),

            Map.entry(KeyCode.MULTIPLY, "*"), Map.entry(KeyCode.DIVIDE, "/"),
            Map.entry(KeyCode.ADD, "+"), Map.entry(KeyCode.SUBTRACT, "-"),
            Map.entry(KeyCode.DECIMAL, ".")
    );

    private static final KeySequenceManager INSTANCE = new KeySequenceManager();

    private List<Predicate<KeyEvent>> matchRules = new ArrayList<>();
    private Predicate<KeyEvent> donePredicate = evt -> evt.getCode() == KeyCode.ENTER;
    private Consumer<String> onSequenceComplete;
    private Predicate<String> sequenceValidation = null;
    private final StringBuilder buffer = new StringBuilder();
    private KeySequenceBlocker blocker;

    private long lastKeyTime = 0;
    private long timeoutMillis = 250;

    public static KeySequenceManager getInstance() {
        return INSTANCE;
    }

    public void setMatchRules(List<Predicate<KeyEvent>> matchRules) {
        this.matchRules = new ArrayList<>(matchRules);
        reset();
    }

    public void setDonePredicate(Predicate<KeyEvent> donePredicate) {
        this.donePredicate = donePredicate;
    }

    public void setOnSequenceComplete(Consumer<String> listener) {
        this.onSequenceComplete = listener;
    }

    public void setBlocker(KeySequenceBlocker blocker) {
        this.blocker = blocker;
    }

    public void setSequenceValidation(Predicate<String> validator) {
        this.sequenceValidation = validator;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void process(KeyEvent evt) {
        if (blocker != null && blocker.shouldBlock()) {
            buffer.setLength(0);
            return;
        }

        if (evt.getEventType() != KeyEvent.KEY_RELEASED) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastKeyTime > timeoutMillis) {
            buffer.setLength(0);
        }
        lastKeyTime = now;

        if (donePredicate != null && donePredicate.test(evt)) {
            String sequence = buffer.toString();
            buffer.setLength(0);
            if ((sequenceValidation == null || sequenceValidation.test(sequence)) && onSequenceComplete != null) {
                onSequenceComplete.accept(sequence);
            }
            return;
        }

        boolean matched = false;
        for (Predicate<KeyEvent> rule : matchRules) {
            if (rule.test(evt)) {
                String charPart = KEY_TO_CHAR_MAP.get(evt.getCode());
                if (charPart != null) {
                    buffer.append(charPart);
                }
                matched = true;
                break;
            }
        }

        if (!matched) {
            buffer.setLength(0);
        }
    }

    private void reset() {
        buffer.setLength(0);
    }
}