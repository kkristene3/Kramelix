/**
 * Owns the end-to-end transcription and language-model orchestration pipeline.
 *
 * <p>This package provides controllers that coordinate:
 * <ul>
 *     <li>Initialization of the Whisper speech-to-text model (via JNI binding).</li>
 *     <li>Transcription of recorded WAV files into text on a background thread.</li>
 *     <li>Chat UI updates for pending and finalized user/assistant messages.</li>
 *     <li>Invocation of the LLM to generate contextual assistant replies.</li>
 *     <li>Automatic text-to-speech (TTS) playback of the assistant's response.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Threading:</b> heavy work (Whisper + LLM) runs on a worker thread;
 *     UI feedback (toasts, LiveData updates) is posted via {@link android.os.Handler}.</li>
 *     <li><b>Separation of concerns:</b> no UI elements; Activities/Fragments handle visuals.</li>
 *     <li><b>Composition:</b> depends on {@link com.example.kramelix.feature.chat.controller.ChatController},
 *     {@link com.example.kramelix.chatgpt.LlmClient}, and native {@link com.example.kramelix.whisperjni.Whisper} bindings.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>The Whisper model must be initialized once per session before transcription.</li>
 *     <li>Recorded WAV files must exist and exceed a minimal byte length.</li>
 *     <li>All audio, LLM, and UI calls are managed asynchronously.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * TranscriptionController tc =
 *     TranscriptionController.getInstance(context, chatController, llmClient);
 *
 * tc.isModelInitialized(modelPath); // initialize Whisper
 * tc.transcribeAndReply(wavFile, text -> { // transcribe + LLM + TTS
 *     ttsController.speak(text);
 * });
 * }</pre>
 *
 * @author Amy Huang, Alex Oprea, Kristen Duong
 * @since 1.0
 */
package com.example.kramelix.feature.transcribe.controller;
