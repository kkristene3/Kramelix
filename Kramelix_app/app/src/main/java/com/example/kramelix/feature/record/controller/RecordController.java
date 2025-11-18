package com.example.kramelix.feature.record.controller;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.kramelix.feature.taskexe.controller.TaskController;

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
     * TaskController instance to control music when user gives a new task to llm
     */
    @NonNull
    private final TaskController taskExe;


    // -------------------- LIFECYCLE --------------------

    /**
     * Constructor.
     *
     * @param ctx Any context; its application context will be retained.
     */
    private RecordController(@NonNull Context ctx) {
        // INITIALIZATION: avoiding Activity leak by storing application context.
        this.ctx = ctx.getApplicationContext();
        taskExe = TaskController.getInstance(ctx);
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

        // PROCESS: stop any music if playing
        taskExe.stopMusic();

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

}
