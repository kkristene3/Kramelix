/**
 * Application shell & feature entrypoints for Kramelix.
 *
 * <p>This top-level package wires together the app’s core features:
 * <ul>
 *     <li><b>Chat UI</b> — message models, repository, controller, and RecyclerView adapter
 *     (see {@link com.example.kramelix.feature.chat}).</li>
 *     <li><b>Recording &amp; Playback</b> — mic capture (PCM→WAV) and local audio playback
 *     (see {@link com.example.kramelix.feature.record}).</li>
 *     <li><b>Transcription Pipeline</b> — Whisper model init, transcription, chat updates, LLM call,
 *     and TTS orchestration (see {@link com.example.kramelix.feature.transcribe}).</li>
 *     <li><b>TTS</b> — Android {@link android.speech.tts.TextToSpeech} wrapper
 *     (see {@link com.example.kramelix.feature.tts}).</li>
 *     <li><b>LLM Bridge</b> — thin Java wrapper around Chaquopy/Python for {@code whisper.py}
 *     (see {@link com.example.kramelix.chatgpt}).</li>
 *     <li><b>Whisper JNI</b> — static JNI surface for native Whisper functions
 *     (see {@link com.example.kramelix.whisperjni}).</li>
 * </ul>
 *
 * <br>
 * <strong>Main Entrypoint</strong>
 * <ul>
 *     <li>{@link com.example.kramelix.MainActivity} binds UI widgets, requests microphone permission,
 *     delegates to feature controllers, and performs one-time model copy/init.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Separation of concerns:</b> Activities/Fragments stay thin; controllers own feature logic.</li>
 *     <li><b>Threading:</b> audio capture runs on a background writer thread; Whisper/LLM on worker threads;
 *     UI feedback is posted to the main thread.</li>
 *     <li><b>Data flow:</b> the chat feature exposes {@link androidx.lifecycle.LiveData} snapshots for reactive updates.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Microphone capture requires {@code android.permission.RECORD_AUDIO}.</li>
 *     <li>The Whisper model is copied from assets to app-internal storage on first run and initialized once per session.</li>
 *     <li>Controllers are not re-entrant unless explicitly documented; stop/finish before restarting.</li>
 * </ul>
 *
 * @author Amy Huang, Kristen Duong, Alex Oprea
 * @since 1.0
 */
package com.example.kramelix;
