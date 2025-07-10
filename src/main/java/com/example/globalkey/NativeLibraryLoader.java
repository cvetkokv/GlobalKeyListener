package com.example.globalkey;

public class NativeLibraryLoader {
    public static void load(String libName) {
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load native library: " + libName, e);
        }
    }
}
