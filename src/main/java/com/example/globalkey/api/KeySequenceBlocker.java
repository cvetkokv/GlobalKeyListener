package com.example.globalkey.api;

@FunctionalInterface
public interface KeySequenceBlocker {
    boolean shouldBlock();
}
