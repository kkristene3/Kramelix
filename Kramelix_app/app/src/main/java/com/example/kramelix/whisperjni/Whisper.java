package com.example.kramelix.whisperjni;

/**
 * This class provides a static Java–native interface (JNI) bridge to the Whisper speech-to-text engine.
 *
 * <p>It exposes a minimal set of native methods backed by a compiled C++ library
 * ({@code libwhisper_jni.so}), enabling Java code to load a Whisper model & transcribe
 * WAV audio files entirely on the device.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Loads the native Whisper JNI library once when first referenced.</li>
 *     <li>Exposes native entry points for:
 *         <ul>
 *             <li>{@link #initModel(String)} — initialize a Whisper model into native memory.</li>
 *             <li>{@link #transcribeWav(String)} — perform speech-to-text transcription on a WAV file.</li>
 *         </ul>
 *     </li>
 *     <li>Serves as the stable Java-side API used by
 *         {@link com.example.kramelix.feature.transcribe.controller.TranscriptionController}.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Call {@link #initModel(String)} once per session before invoking {@link #transcribeWav(String)}.</li>
 *     <li>Paths passed to native methods must be absolute and readable by the app process.</li>
 *     <li>All native resources are managed by the C++ layer; this class holds no persistent state.</li>
 * </ul>
 *
 * @author Amy Huang
 * @noinspection UtilityClassWithoutPrivateConstructor, PublicConstructor, UtilityClassCanBeEnum, NativeMethod
 * @since 1.0
 */

public final class Whisper {

    // PROCESS: loading our native shared library exactly once when the class is first referenced
    static {
        System.loadLibrary("whisper_jni");
    } // throws UnsatisfiedLinkError if .so is missing

    // -------------------- NATIVE METHODS --------------------

    /**
     * Loads the model into native memory.
     *
     * @param modelPath A string pointing to the model file.
     * @return {@code true} if the model successfully loads; {@code false} otherwise.
     * @noinspection BooleanMethodNameMustStartWithQuestion
     */
    public static native boolean initModel(String modelPath);

    /**
     * Transcribes a WAV file.
     *
     * @param wavPath A string pointing to the WAV audio file.
     * @return The transcribed text.
     */
    public static native String transcribeWav(String wavPath);
}
