/**
 * Owns microphone recording and local audio playback lifecycles (no UI widgets).
 *
 * <p>This package contains small controllers used by Activities/Fragments to:
 * <ul>
 *     <li>Capture microphone audio as PCM and finalize it as a WAV file
 *     (see {@link com.example.kramelix.feature.record.controller.PcmRecordController}).</li>
 *     <li>Start/stop playback of a WAV file using {@link android.media.MediaPlayer}
 *     (see {@link com.example.kramelix.feature.record.controller.RecordController}).</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Separation of concerns:</b> no UI elements; callers provide UX (toasts, buttons, etc.).</li>
 *     <li><b>Threading:</b> recording writes PCM on a background thread; WAV is finalized on stop().</li>
 *     <li><b>Audio config:</b> requests 16 kHz, mono, 16-bit; uses the device's <i>actual</i> input
 *     sample rate from {@link android.media.AudioRecord#getSampleRate()}.</li>
 *     <li><b>Temp file:</b> raw PCM is written to {@code tmp_recording.pcm} next to the target WAV, then deleted.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Caller must ensure {@code android.permission.RECORD_AUDIO} is granted before starting capture.</li>
 *     <li>Controllers are not re-entrant: call {@code stop()} before starting again.</li>
 *     <li>The destination directory for WAV output must already exist.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * RecordController rc = RecordController.getInstance(context);
 * File wav = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.wav");
 *
 * // Start
 * rc.startRecording(wav);
 *
 * // ... later ...
 * rc.stopRecording(wav);
 *
 * // Play
 * rc.startPlayback(wav);
 *
 * // Stop
 * rc.stopPlayback();
 * }</pre>
 *
 * @author Kristen Duong, Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.record.controller;
