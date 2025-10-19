/**
 * Owns the text-to-speech (TTS) playback layer for assistant responses and other synthesized audio.
 *
 * <p>This package provides a controller around Android's {@link android.speech.tts.TextToSpeech}
 * engine, allowing the app to convert text (such as LLM replies) into spoken output with minimal setup.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Initializes & configures the {@link android.speech.tts.TextToSpeech} engine once per session.</li>
 *     <li>Speaks arbitrary text, flushing any queued utterances when requested.</li>
 *     <li>Stops ongoing playback & releases resources when the host component is destroyed.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Separation of concerns:</b> no UI logic; Activities or higher-level controllers decide when to speak.</li>
 *     <li><b>Threading:</b> all engine work runs on the internal TTS service thread; callers may safely invoke from any thread.</li>
 *     <li><b>Lifecycle:</b> the controller retains a single {@link android.content.Context#getApplicationContext() application context}
 *     to avoid leaks, and must be shut down explicitly.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Create one {@code TTSController} instance per app/session using {@link com.example.kramelix.feature.tts.controller.TTSController#getInstance(android.content.Context)}.</li>
 *     <li>Always call {@link com.example.kramelix.feature.tts.controller.TTSController#shutdown()} in {@code onDestroy()} or equivalent.</li>
 *     <li>Language defaults to US English; missing or unsupported locales are logged via {@link android.util.Log}.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * TTSController tts = TTSController.getInstance(context);
 *
 * // Speak any assistant reply
 * tts.speak("Hello! How can I help you today?");
 *
 * // ... later ...
 * tts.shutdown(); // free system resources
 * }</pre>
 *
 * @author Kristen Duong, Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.tts.controller;
