package com.example.kramelix.whisperjni;

/**
 * This class acts as a thin Java wrapper over our native Whisper JNI bridge.
 * It contains functions whose calls are forwarded by Java to our native code.
 */
public final class Whisper {

    // Loading our native shared library exactly once when the class is first referenced
    static {
        System.loadLibrary("whisper_jni");
    } // throws UnsatisfiedLinkError if .so is missing

    // -------------------- Native methods implemented in whisper_jni.cpp --------------------
    // AMY'S NOTE: Don't rename these functions without updating the C++ JNI symbol names!!!

    /**
     * Loads the model into native memory.
     * @param modelPath a string pointing to the model file
     * @return whether the model successfully loads
     */
    public static native boolean initModel(String modelPath);

    /**
     * Transcribes a WAV file.
     * @param wavPath a string pointing to the WAV audio file
     * @return the transcribed text
     */
    public static native String transcribeWav(String wavPath);
}
