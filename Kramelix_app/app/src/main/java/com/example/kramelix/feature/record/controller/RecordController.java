package com.example.kramelix.feature.record.controller;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * This controller owns the microphone recording & audio playback lifecycles (no UI widgets).
 *
 * <p>This class composes {@link PcmRecordController} for PCM to WAV capture & wraps {@link MediaPlayer} for playback.</p>
 * <p>AMY'S NOTE: Callers (Activities/Fragments) can delegate recording/playback concerns here
 * so that UI code remains focused on rendering and input handling.</p>
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Starts/stops WAV recording via {@link PcmRecordController}.</li>
 *     <li>Starts/stops local WAV playback via {@link MediaPlayer}.</li>
 * </ul>
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Caller must ensure RECORD_AUDIO permission before starting recording.</li>
 *     <li>Recording and playback are independent (but users generally shouldn't play while recording).</li>
 * </ul>
 *
 * @author Kristen Duong, Amy Huang
 * @since 1.0
 */
public final class RecordController {

    // -------------------- CONFIGURATION --------------------

    /**
     * Small heuristic to consider a WAV 'usable' (avoids zero-length/headers-only files).
     */
    private static final long MIN_WAV_BYTES = 2000L;

    // -------------------- STATE --------------------

    /**
     * Application context used for {@link Toast}s and MediaPlayer data source resolution.
     */
    @NonNull
    private final Context ctx;

    /**
     * Delegated PCM to WAV capture controller.
     */
    @NonNull
    private final PcmRecordController recorder = new PcmRecordController();

    /**
     * MediaPlayer instance for playback; non-null only while playing or prepared.
     */
    @Nullable
    private MediaPlayer mediaPlayer;

    // -------------------- LIFECYCLE --------------------

    /**
     * Constructor.
     *
     * @param ctx Any context; its application context will be retained.
     */
    private RecordController(@NonNull Context ctx) {
        // INITIALIZATION: avoiding Activity leak by storing application context.
        this.ctx = ctx.getApplicationContext();
    }

    /**
     * Factory instance.
     *
     * @param ctx Any context.
     * @return The controller instance.
     */
    public static RecordController getInstance(@NonNull Context ctx) {
        // OUTPUT:
        return new RecordController(ctx);
    }

    // -------------------- RECORDING --------------------

    /**
     * This function begin a new recording into the specified WAV path.
     *
     * @param wavOut The destination WAV file. Parent directory must exist.
     * @throws IOException If audio capture can't start or temp file can't be opened.
     */
    public void startRecording(@NonNull File wavOut) throws IOException {

        // PROCESS: having PCM record run writher thread internally
        recorder.start(wavOut);

        // OUTPUT: UX feedback
        Toast.makeText(ctx, "Recording...", Toast.LENGTH_SHORT).show();

    }

    /**
     * This function stops the active recording & finalizes the WAV file.
     *
     * @param wavOut The same path passed to {@link #startRecording(File)}.
     * @throws IOException          If WAV finalization fails.
     * @throws InterruptedException If the writer thread join is interrupted.
     */
    public void stopRecording(@NonNull File wavOut) throws IOException, InterruptedException {

        // PROCESS: finalizing PCM to WAV & cleaning the temp file
        recorder.stop(wavOut);

        long size = wavOut.length(); // final file size for UX/debug

        // OUTPUT: UX feedback
        Toast.makeText(ctx, "Saved: " + size + " bytes\n" + wavOut.getAbsolutePath(),
                Toast.LENGTH_SHORT).show();

    }

    // -------------------- PLAYBACK --------------------

    /**
     * This helper function checks if a usable recording exists at the provided path.
     *
     * @param wavOut The WAV file to inspect.
     * @return {@code true} if the file exists and looks non-trivial.
     */
    private static boolean hasUsableRecording(@NonNull File wavOut) {
        // OUTPUT:
        return wavOut.exists() && MIN_WAV_BYTES < wavOut.length();
    }

    /**
     * This function starts playing the provided WAV file through {@link MediaPlayer}.
     *
     * @param wavOut The WAV file to play.
     * @throws IOException If the data source can't be opened or prepared.
     */
    public void startPlayback(@NonNull File wavOut) throws IOException {

        // ERROR-HANDLING: guarding against missing/too-short recording
        if (!hasUsableRecording(wavOut)) {

            // FIXME OPTIMIZE: 2025-10-13 we should probably display a better msg?
            // OUTPUT:
            Toast.makeText(ctx, "No/short recording. Did you stop recording?",
                    Toast.LENGTH_SHORT).show();
            return;

        }

        // PROCESS: if already playing, stop & release first
        if (null != mediaPlayer) {
            stopPlayback();
        }

        // VARIABLE DECLARATION: fresh MediaPlayer instance
        mediaPlayer = new MediaPlayer();

        // PROCESS: wiring the media source, then prepping & starting
        mediaPlayer.setDataSource(wavOut.getAbsolutePath());
        mediaPlayer.prepare(); // sync prepare; file is local
        mediaPlayer.start();

        // OUTPUT: UX feedback
        Toast.makeText(ctx, "Playing...", Toast.LENGTH_SHORT).show();

        // PROCESS: auto-releasing when playback completes
        mediaPlayer.setOnCompletionListener(mp -> stopPlayback());

    }

    /**
     * This function stops playback if active & releases the {@link MediaPlayer}.
     * Safe to call multiple times.
     */
    public void stopPlayback() {

        // PROCESS: stopping may throw if not in a started state; guarding with try/catch
        try {

            if (null != mediaPlayer) { // exists

                mediaPlayer.stop();
                mediaPlayer.release();

            }

        } catch (IllegalStateException ignore) {
        } // error-handling: ignoring irrelevant state errors (e.g. double-stop)
        finally {
            // PROCESS: clearing reference to reflect 'not playing' state
            mediaPlayer = null;
        }

        // OUTPUT: UX feedback
        Toast.makeText(ctx, "Stopped", Toast.LENGTH_SHORT).show();

    }

}
