package com.example.kramelix.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;
import com.chaquo.python.android.AndroidPlatform;
import com.example.kramelix.R;
import com.example.kramelix.model.Role;
import com.example.kramelix.whisperjni.Whisper;
import com.example.kramelix.BuildConfig;
import com.example.kramelix.model.ConversationRepository;
import com.example.kramelix.model.Message;
import com.example.kramelix.view.ChatAdapter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_AUDIO = 1001;

    private ToggleButton recordButton;
    private ToggleButton playRecButton;
    private TextView recordText;

    @Nullable
    private MediaPlayer mediaPlayer;
    private final PcmRecorder pcmRecorder = new PcmRecorder();

    private File wavPath; // recording.wav in app's music dir
    private File modelFile; // copied from assets/models/ggml-base.en.bin

    private boolean recordPendingAfterPermission;

    // Chat UI
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private ConversationRepository convoRepo; // in-memory per-session log

    // TTS Controller
    private TTSController ttsController;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Binding UI first
        recordButton = findViewById(R.id.recordButton);
        playRecButton = findViewById(R.id.playRecButton);
        recordText = findViewById(R.id.recordText);
        chatRecycler = findViewById(R.id.chatRecycler);

        // Wiring chat list for view of conversation history
        chatAdapter = new ChatAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true); // newest messages at bottom
        chatRecycler.setLayoutManager(lm);
        chatRecycler.setAdapter(chatAdapter);

        // Setting up conversation repo
        convoRepo = ConversationRepository.get();
        convoRepo.getMessages().observe(this, msgs -> {
            chatAdapter.submit(msgs);
            chatRecycler.scrollToPosition(Math.max(0, chatAdapter.getItemCount() - 1));
        });

        // PROCESS: preparing output paths
        wavPath = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.wav");

        // AMY'S NOTE: we're currently shipping the model in app assets (read-only), which means that
        // on our first run, we first COPY it to app-internal storage (/data/.../files/models) bc:
        // (1) our native code needs a filesystem path (and assets aren't considered regular files apparently...) &
        // (2) assets are compressed; Whisper wants a REAL file path that it can map/read quickly.
        // Then, we initialize the native model once, and reuse that context for all transcriptions.

        // Copying model once from assets to files, then initializing JNI
        modelFile = ensureModelCopiedOnce();

        if (null == modelFile) { // error-handling
            // FIXME OPTIMIZE: consider updating this error toast to a log msg instead
            // TODO: display a better msg for the user
            // OUTPUT:
            Toast.makeText(this, "Model copy failed", Toast.LENGTH_LONG).show();
        } else {
            // FIXME OPTIMIZE: might have to move this to the background thread if we ever switch to a bigger model in the future bc the ops. are heavy
            boolean success = Whisper.initModel(modelFile.getAbsolutePath());
            Log.i(TAG, "Whisper.initModel = " + success);

            // TODO: consider updating this error toast to a log msg instead
            if (!success) Toast.makeText(this, "Whisper init failed", Toast.LENGTH_LONG).show();
        }

        // RECORD toggle
        recordButton.setOnClickListener(v -> {
            if (recordButton.isChecked()) {
                if (hasRecordPermission()) {
                    startRecording();
                    recordText.setText(R.string.stop_recording); // change text when recorder is clicked
                } else {
                    recordPendingAfterPermission = true;
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
                    recordButton.setChecked(false);
                }
            } else {
                stopRecording();
                recordText.setText(R.string.start_recording); // change text when recorder is turned off
            }
        });

        // PLAY toggle
        playRecButton.setOnClickListener(v -> {
            if (playRecButton.isChecked()) {
                startPlayback();
            } else {
                stopPlayback();
            }
        });

        // TTS Controller
        ttsController = new TTSController(this);

    }

    // -------------------- Recording --------------------

    private void startRecording() {
        try {
            pcmRecorder.start(wavPath);
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
            recordButton.setChecked(true);
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
            recordButton.setChecked(false);
            Toast.makeText(this, "Record failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        try {
            pcmRecorder.stop(wavPath);
            long size = wavPath.length();
            Toast.makeText(this, "Saved: " + size + " bytes\n" + wavPath.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.i(TAG, "WAV saved, size=" + size + " path=" + wavPath);
            recordButton.setChecked(false);
        } catch (Exception e) {
            Log.e(TAG, "stopRecording failed", e);
            Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        doTranscribe();
    }

    // -------------------- Playback --------------------

    private void startPlayback() {
        try {
            if (!wavPath.exists() || 2000 > wavPath.length()) {
                Toast.makeText(this, "No/short recording. Did you stop recording?", Toast.LENGTH_SHORT).show();
                playRecButton.setChecked(false);
                return;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(wavPath.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "Playing...", Toast.LENGTH_SHORT).show();
            mediaPlayer.setOnCompletionListener(mp -> {
                stopPlayback();
                playRecButton.setChecked(false);
            });
        } catch (IOException e) {
            Log.e(TAG, "startPlayback failed", e);
            Toast.makeText(this, "Play failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            playRecButton.setChecked(false);
        }
    }

    private void stopPlayback() {
        try {
            if (null != mediaPlayer) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (IllegalStateException ignored) {
            // TODO: add error-handling here
        } finally {
            mediaPlayer = null;
        }
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }

    // -------------------- Transcription --------------------

    /**
     * This helper function transcribes a WAV file and updates the UI accordingly.
     */
    private void doTranscribe() {

        // PROCESS: checking for loaded model
        if (null == modelFile || !modelFile.exists()) {
            // TODO: consider updating this error toast to a log msg instead
            Toast.makeText(this, "Model missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wavPath.exists() || 2000L > wavPath.length()) { // audio too short; treating as empty
            // TODO: display a better msg for the user
            Toast.makeText(this, "No/short recording", Toast.LENGTH_SHORT).show();
            return;
        }

        // PROCESS: creating background thread for native calls to avoid blocking main UI thread
        new Thread(() -> {

            try {

                // UX: adding USER pending bubble (pulses + dots)
                Message userPending = convoRepo.addPendingMessage(Role.USER, "…");

                // VARIABLE DECLARATION: JNI call on worker thread to avoid janking UI
                String text = Whisper.transcribeWav(wavPath.getAbsolutePath());
                Log.i(TAG, "TRANSCRIPT: " + text);

                // PROCESS: updating the USER pending bubble w/ the real transcript
                String safeUserText = (null == text || text.isBlank()) ? "[empty transcript]" : text;
                convoRepo.updateMessage(userPending.getId(), safeUserText, false);

                // UX: adding ASSISTANT pending bubble (pulses + dots)
                Message assistantPending = convoRepo.addPendingMessage(Role.ASSISTANT, "…");

                // PROCESS: retrieving the LLM response off main thread
                String llm;

                try {
                    llm = getResponse(safeUserText);
                } catch (RuntimeException e) { // error-handling
                    // TODO: display a better error msg for the user
                    Log.e(TAG, "LLM call failed", e);
                    llm = "[llm error: " + e.getClass().getSimpleName() + "]";
                }

                String safeResp = (null == llm || llm.isBlank()) ? "[no response given]" : llm;

                // PROCESS: updating ASSISTANT pending bubble w/ the final response
                convoRepo.updateMessage(assistantPending.getId(), safeResp, false);

                // PROCESS: provide text-to-speech for the LLM response
                runOnUiThread(() -> {
                    if (!"[no response given]".equals(safeResp)) {
                        ttsController.speak(safeResp);
                    }
                });

            } catch (RuntimeException e) { // error-handling

                // TODO: display a better error msg for the user & consider running again
                Log.e(TAG, "Transcription pipeline failed", e);
                runOnUiThread(() -> Toast.makeText(this, "Transcription failed: " + e.getMessage(), Toast.LENGTH_LONG).show());

            }

        }, "whisper-transcribe").start();

    }

    // -------------------- Calling LLM ---------------------
    @Nullable
    private String getResponse(String prompt) {

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        PyObject mod = py.getModule("whisper");

        String apiKey = BuildConfig.OPENAI_API_KEY;

        PyObject response = mod.callAttr("chat", apiKey, null == prompt ? "" : prompt);
        return null != response ? response.toString() : null;
    }

    // ------------------- Shutdown TTS --------------------
    @Override
    protected final void onDestroy() {
        if (null != ttsController) {
            ttsController.shutdown();
        }
        super.onDestroy();
    }

    // -------------------- Permissions --------------------
    private boolean hasRecordPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQ_AUDIO == requestCode) {
            boolean granted = 0 < grantResults.length && PackageManager.PERMISSION_GRANTED == grantResults[0];
            if (granted && recordPendingAfterPermission) {
                recordPendingAfterPermission = false;
                startRecording();
            } else if (!granted) {
                Toast.makeText(this, "Mic permission is required to record", Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------- Asset copy --------------------

    /** Copy assets/models/ggml-tiny.en.bin to files/models/ggml-tiny.en.bin once, return the out File. */
    @Nullable
    private File ensureModelCopiedOnce() {
        try {
            File outDir = new File(getFilesDir(), "models");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir, "ggml-tiny.en.bin");
            if (!out.exists()) {
                try (InputStream in = getAssets().open("models/ggml-tiny.en.bin");
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while (0 < (n = in.read(buf))) os.write(buf, 0, n);
                }
            }
            return out;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Model copy failed", e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Reading model failed", e);
            return null;
        }
    }

    // -------------------- PCM -> WAV recorder (uses ACTUAL device sample rate) --------------------

    static final class PcmRecorder {
        private static final int REQUESTED_SAMPLE_RATE = 16000; // request 16k, device may choose differently
        private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
        private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

        @Nullable
        private AudioRecord audioRecord;
        @Nullable
        private Thread thread;
        private volatile boolean running;
        private long bytesWritten;
        private int actualSampleRate = REQUESTED_SAMPLE_RATE;

        /** Start capturing to temp PCM; later wrapped into WAV at stop(). */
        @SuppressLint("MissingPermission")
        void start(File wavOut) throws IOException {
            int minBuf = AudioRecord.getMinBufferSize(REQUESTED_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int bufSize = Math.max(minBuf, 4096);

            // VOICE_RECOGNITION often gives 16k on Samsung; fallback to MIC if needed.
            int source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            audioRecord = new AudioRecord(
                    source,
                    REQUESTED_SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufSize);

            if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
                // Fallback to MIC
                audioRecord.release();
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        REQUESTED_SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufSize);
            }

            if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
                throw new IOException("AudioRecord init failed (state=" + audioRecord.getState() + ")");
            }

            actualSampleRate = audioRecord.getSampleRate();
            Log.i("PcmRecorder", "Recording started, requested=" + REQUESTED_SAMPLE_RATE +
                    " actual=" + actualSampleRate + ", bufSize=" + bufSize);

            File pcm = new File(wavOut.getParentFile(), "tmp_recording.pcm");
            FileOutputStream fos = new FileOutputStream(pcm);
            bytesWritten = 0L;

            running = true;
            audioRecord.startRecording();

            thread = new Thread(() -> {
                byte[] buf = new byte[4096];
                try (FileOutputStream out = fos) {
                    while (running) {
                        int n = audioRecord.read(buf, 0, buf.length);
                        if (0 < n) {
                            out.write(buf, 0, n);
                            bytesWritten += n;
                        } else {
                            Log.w("PcmRecorder", "audioRecord.read=" + n);
                        }
                    }
                } catch (IOException e) {
                    Log.e("PcmRecorder", "writer error", e);
                }
            }, "pcm-writer");

            thread.start();
        }

        /** Stop capture, join thread, wrap PCM into a WAV with the ACTUAL sample rate. */
        void stop(File wavOut) throws IOException, InterruptedException {
            running = false;
            if (null != audioRecord) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException ignore) {
                    // TODO: add error-handling here
                }
                audioRecord.release();
                audioRecord = null;
            }
            if (null != thread) {
                thread.join();
                thread = null;
            }

            File pcm = new File(wavOut.getParentFile(), "tmp_recording.pcm");
            if (!pcm.exists()) throw new IOException("No PCM file written");

            writeWavFromPcm16Mono(pcm, wavOut, actualSampleRate);

            long wavLen = wavOut.length();
            //noinspection ResultOfMethodCallIgnored
            pcm.delete();

            Log.i("PcmRecorder", "Recording stopped. bytesWritten=" + bytesWritten +
                    ", wavSize=" + wavLen + ", sr=" + actualSampleRate);
        }

        // --- WAV writer helpers ---
        private static void writeWavFromPcm16Mono(File pcm, File wav, int sampleRate) throws IOException {
            byte[] pcmBytes = readAll(pcm);
            int dataLen = pcmBytes.length;
            int byteRate = sampleRate << 1; // mono, 16-bit
            int blockAlign = 2;

            try (FileOutputStream out = new FileOutputStream(wav)) {
                out.write(new byte[]{'R', 'I', 'F', 'F'});
                writeLE32(out, 36 + dataLen);
                out.write(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
                writeLE32(out, 16);
                writeLE16(out, 1);   // PCM
                writeLE16(out, 1);   // mono
                writeLE32(out, sampleRate);
                writeLE32(out, byteRate);
                writeLE16(out, blockAlign);
                writeLE16(out, 16);  // bits
                out.write(new byte[]{'d', 'a', 't', 'a'});
                writeLE32(out, dataLen);
                out.write(pcmBytes);
            }
        }

        private static byte[] readAll(File f) throws IOException {
            if (26 <= Build.VERSION.SDK_INT) {
                return Files.readAllBytes(f.toPath());
            } else {
                byte[] buf = new byte[(int) f.length()];
                try (InputStream in = new java.io.FileInputStream(f)) {
                    int off = 0, n;
                    while (0 < (n = in.read(buf, off, buf.length - off))) off += n;
                    return buf;
                }
            }
        }

        private static void writeLE16(OutputStream os, int v) throws IOException {
            os.write(v & 0xff);
            os.write((v >> 8) & 0xff);
        }

        private static void writeLE32(OutputStream os, int v) throws IOException {
            os.write(v & 0xff);
            os.write((v >> 8) & 0xff);
            os.write((v >> 16) & 0xff);
            os.write((v >> 24) & 0xff);
        }
    }
}
