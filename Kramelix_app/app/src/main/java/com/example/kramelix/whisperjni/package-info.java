/**
 * Provides the Java–native (JNI) bridge layer for the Whisper speech-to-text engine.
 *
 * <p>This package defines the minimal API surface required for Java/Kotlin code to
 * interact with the native Whisper runtime implemented in C++ and distributed as
 * {@code libwhisper_jni.so}. It enables on-device model loading and transcription
 * through static native methods, abstracting all JNI details from higher layers.</p>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Static utility:</b> all methods are {@code static}; no instances are required.</li>
 *     <li><b>JNI linkage:</b> method names must exactly match their native counterparts in
 *     {@code whisper_jni.cpp}. Renaming requires corresponding C++ symbol updates.</li>
 *     <li><b>Error handling:</b> if {@code libwhisper_jni.so} is missing or fails to load,
 *     a {@link java.lang.UnsatisfiedLinkError} is thrown at class load time.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Call {@link com.example.kramelix.whisperjni.Whisper#initModel(String)} once before
 *     {@link com.example.kramelix.whisperjni.Whisper#transcribeWav(String)}.</li>
 *     <li>Model and audio paths must be absolute and accessible by the app process.</li>
 *     <li>All native resources are managed internally by the C++ layer; this package is stateless.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * boolean ok = Whisper.initModel(modelPath);
 * if (ok) {
 *     String transcript = Whisper.transcribeWav(wavFilePath);
 * }
 * }</pre>
 *
 * @author Amy Huang
 * @since 1.0
 */
package com.example.kramelix.whisperjni;
