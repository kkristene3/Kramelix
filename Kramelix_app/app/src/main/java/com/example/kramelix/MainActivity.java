package com.example.kramelix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kramelix.chatgpt.LlmClient;
import com.example.kramelix.feature.chat.controller.ChatController;
import com.example.kramelix.feature.record.controller.RecordController;
import com.example.kramelix.feature.transcribe.controller.TranscriptionController;
import com.example.kramelix.feature.tts.controller.TTSController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;

/**
 * This class acts as the app's entry point: binds chat UI, handles mic permission, & delegates
 * recording, playback, transcription, LLM, and TTS to feature controllers.
 *
 * <p>MainActivity intentionally stays thin: it wires buttons & forwards
 * actions to the appropriate controllers.</p>
 *
 * @author Kristen Duong, Amy Huang, Alex Oprea
 * @noinspection FieldCanBeLocal, ClassWithTooManyFields, PublicConstructor
 * @since 1.0
 */
public final class MainActivity extends AppCompatActivity {

    // TODO: consider implementing better error-handling everywhere...

    // -------------------- CONFIGURATION --------------------
    /**
     * Logcat tag for diagnostics.
     */
    private static final String TAG = "MainActivity";

    /**
     * Permission request code for microphone recording.
     */
    private static final int REQ_AUDIO = 1001;

    /**
     * Asset path to the Whisper model bundled with the app.
     */
    private static final String MODEL_ASSET_PATH = "models/ggml-tiny.en.bin";

    /**
     * Destination filename for the Whisper model under app-internal storage.
     */
    private static final String MODEL_DEST_NAME = "ggml-tiny.en.bin";

    // -------------------- UI --------------------
    private ToggleButton recordButton;
    private TextView recordText;
    private RecyclerView chatRecycler;

    // -------------------- CONTROLLERS --------------------
    private ChatController chatController;
    private RecordController recordController;
    private TranscriptionController transcriptionController;
    private TTSController ttsController;
    private LlmClient llmClient;

    // -------------------- PATHS --------------------
    private File wavPath;
    @Nullable
    private File modelFile;

    private boolean recordPendingAfterPermission;

    // -------------------- LIFECYCLE --------------------

    /**
     * Activity creation hook: binds UI, initializes controllers, prepares model file, and wires buttons.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // INITIALIZATION:
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bindUi();
        setupControllers();
        initModelOnce();
        setupButtons();

    }

    /**
     * Activity teardown: shuts down TTS resources.
     */
    @Override
    protected void onDestroy() {
        if (null != ttsController) ttsController.shutdown();
        super.onDestroy();
    }

    // -------------------- UI WIRING --------------------

    /**
     * This function binds view references & computes the session WAV output path.
     */
    private void bindUi() {

        // VARIABLE DECLARATION: view bindings
        recordButton = findViewById(R.id.recordButton);
        recordText = findViewById(R.id.recordText);
        chatRecycler = findViewById(R.id.chatRecycler);

        // VARIABLE DECLARATION: prepare the session's WAV path (e.g. <app>/files/Music/recording.wav)
        wavPath = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.wav");

    }

    // -------------------- CONTROLLER SETUP --------------------

    /**
     * This function instantiates feature controllers & binds the chat view.
     */
    private void setupControllers() {

        // INITIALIZATION: binding chat RecyclerView to LiveData
        chatController = new ChatController();
        chatController.bind(chatRecycler, this);

        // INITIALIZATION: creating remaining controllers
        recordController = RecordController.getInstance(this);
        llmClient = LlmClient.createLlmClient(this);
        ttsController = TTSController.getInstance(this);
        transcriptionController = TranscriptionController.getInstance(this, chatController, llmClient);

    }

    // -------------------- MODEL INIT --------------------

    /**
     * This function copies the Whisper model from assets into internal storage (if needed) & initializes JNI once.
     */
    @SuppressLint("LogConditional")
    private void initModelOnce() {

        // PROCESS: ensure model file exists in internal storage
        modelFile = ensureModelCopiedOnce();

        if (null == modelFile) {

            // LOG OUTPUT:
            Log.e(TAG, "Model copy failed (ensureModelCopiedOnce returned null)");

            // OUTPUT:
            Toast.makeText(this, "Model copy failed", Toast.LENGTH_LONG).show();
            return;

        }

        // PROCESS: initializing native Whisper model
        boolean ok = transcriptionController.isModelInitialized(modelFile.getAbsolutePath());

        // LOG OUTPUT:
        Log.i(TAG, "TranscriptionController.isModelInitialized -> " + ok);

    }

    // -------------------- BUTTON HANDLERS --------------------

    /**
     * This function wires click listeners for record/play buttons.
     */
    private void setupButtons() {
        recordButton.setOnClickListener(v -> onRecordToggled(recordButton.isChecked()));
    }

    /**
     * This function handles record button toggles, manages permission flow, starts/stops capture,
     * & triggers the pipeline.
     *
     * @param on whether the record toggle is on
     */
    private void onRecordToggled(boolean on) {

        if (on) { // start recording

            if (hasRecordPermission()) {

                try {

                    // PROCESS: begin capture
                    recordController.startRecording(wavPath);
                    recordText.setText(R.string.stop_recording);

                } catch (IOException e) { // error-handling

                    // LOG OUTPUT:
                    Log.e(TAG, "startRecording IO failure", e);
                    recordButton.setChecked(false); // updating UI

                    // OUTPUT: UX feedback
                    Toast.makeText(this, "Record failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

                } catch (RuntimeException e) { // error-handling

                    // LOG OUTPUT:
                    Log.e(TAG, "startRecording runtime failure", e);
                    recordButton.setChecked(false); // updating UI

                    // OUTPUT: UX feedback
                    Toast.makeText(this, "Record failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

                }
            } else {

                // PROCESS: requesting permission & remembering to record
                recordPendingAfterPermission = true;

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_AUDIO
                );

                recordButton.setChecked(false);

            }

        } else { // stop recording

            try {

                // PROCESS: finalizing WAV
                recordController.stopRecording(wavPath);
                recordText.setText(R.string.start_recording);

            } catch (IOException e) { // error-handling

                // LOG OUTPUT:
                Log.e(TAG, "stopRecording IO failure", e);

                // OUTPUT: UX feedback
                Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

            } catch (InterruptedException e) {

                // LOG OUTPUT:
                Log.e(TAG, "stopRecording interrupted", e);

                // OUTPUT: UX feedback
                Toast.makeText(this, "Stop interrupted", Toast.LENGTH_LONG).show();
                Thread.currentThread().interrupt();

            } catch (RuntimeException e) {

                // LOG OUTPUT:
                Log.e(TAG, "stopRecording runtime failure", e);

                // OUTPUT: UX feedback
                Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_LONG).show();

            }

            // PROCESS: starting transcription pipeline
            transcriptionController.transcribeAndReply(
                    wavPath,
                    ttsController::speak
            );

        }

    }

    // -------------------- PERMISSIONS --------------------

    /**
     * This helper function checks whether microphone record permission has been granted.
     *
     * @return {@code true} if permission has been granted; {@code false} otherwise.
     */
    private boolean hasRecordPermission() {
        return PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
    }

    /**
     * Permission callback: if granted and a record was pending, re-trigger the record button.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQ_AUDIO == requestCode) {

            boolean granted = 0 < grantResults.length && PackageManager.PERMISSION_GRANTED == grantResults[0];

            if (granted && recordPendingAfterPermission) {

                // PROCESS: starting recording now that we have permission
                recordPendingAfterPermission = false;
                recordButton.performClick(); // re-triggering start flow

            } else if (!granted) { // error-handling

                // LOG OUTPUT:
                Log.w(TAG, "Microphone permission denied by user.");

                // OUTPUT: UX feedback
                Toast.makeText(this, "Mic permission is required to record", Toast.LENGTH_LONG).show();

            }

        }

    }

    // -------------------- ASSET COPY --------------------

    /**
     * This helper function copies {@code assets/models/ggml-tiny.en.bin} to {@code files/models/ggml-tiny.en.bin} once.
     *
     * @return Destination file on success; {@code null} if copy failed/
     */
    @Nullable
    private File ensureModelCopiedOnce() {

        try {

            // PROCESS: creating destination dir if needed
            File outDir = new File(getFilesDir(), "models");

            if (!outDir.exists() && !outDir.mkdirs()) {

                // LOG OUTPUT:
                Log.e(TAG, "Failed to create model output directory: " + outDir);

                // OUTPUT:
                return null;

            }

            // VARIABLE DECLARATION: output file
            File out = new File(outDir, MODEL_DEST_NAME);

            if (out.exists()) {
                // LOG OUTPUT:
                Log.i(TAG, "Model already present at: " + out.getAbsolutePath());
            } else { // need to copy model

                // PROCESS: copying asset to internal files
                try (InputStream in = getAssets().open(MODEL_ASSET_PATH);

                     FileOutputStream os = new FileOutputStream(out)) {

                    // VARIABLE DECLARATION: copy buffer
                    //noinspection CheckForOutOfMemoryOnLargeArrayAllocation
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while (0 < (n = in.read(buf))) {
                        os.write(buf, 0, n);
                    }

                    try {
                        os.getFD().sync(); // fsync to avoid partial copies
                    } catch (SyncFailedException s) {
                        // LOG OUTPUT:
                        Log.w(TAG, "Best-effort fsync failed (continuing): " + s.getMessage());
                    }

                }

                // LOG OUTPUT:
                Log.i(TAG, "Model copied to: " + out.getAbsolutePath());

            }

            // OUTPUT:
            return out;

        } catch (FileNotFoundException e) { // error-handling
            // LOG OUTPUT:
            Log.e(TAG, "Model copy failed (file not found)", e);
        } catch (IOException e) {
            // LOG OUTPUT:
            Log.e(TAG, "Reading model failed (I/O error)", e);
        } catch (RuntimeException e) {
            // LOG OUTPUT:
            Log.e(TAG, "Model copy failed (runtime exception)", e);
        }

        // OUTPUT: no model
        return null;

    }

}
