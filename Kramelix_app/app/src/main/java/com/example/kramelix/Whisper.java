package com.example.kramelix;

public class Whisper {
    static { System.loadLibrary("whisper_jni"); }
    public static native boolean initModel(String modelPath);
    public static native String  transcribeWav(String wavPath);
}
