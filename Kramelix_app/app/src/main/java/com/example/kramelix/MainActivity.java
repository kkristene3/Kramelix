package com.example.kramelix;

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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Minimal demo:
 * - Record (AudioRecord -> PCM -> WAV, uses device's ACTUAL sample rate)
 * - Play (MediaPlayer)
 * - Transcribe (Whisper JNI) with resampling in native code
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQ_AUDIO = 1001;

    private ToggleButton recordButton;
    private ToggleButton playRecButton;
    private Button transcribeButton;
    private TextView recordText;

    private MediaPlayer mediaPlayer;
    private PcmRecorder pcmRecorder = new PcmRecorder();

    private File wavPath;              // recording.wav in app's music dir
    private File modelFile;            // copied from assets/models/ggml-base.en.bin

    private TextView transcriptionText;

    private boolean recordPendingAfterPermission = false; // if user tapped record before granting permission

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // PROCESS: preparing output paths
        wavPath = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.wav");

        // AMY'S NOTE: we're currently shipping the model in app assets (read-only), which means that
        // on our first run, we first COPY it to app-internal storage (/data/.../files/models) bc:
        // (1) our native code needs a filesystem path (and assets aren't considered regular files apparently...) &
        // (2) assets are compressed; Whisper wants a REAL file path that it can map/read quickly.
        // Then, we initialize the native model once, and reuse that context for all transcriptions.

        // Copying model once from assets to files, then initializing JNI
        modelFile = ensureModelCopiedOnce("ggml-tiny.en.bin");

        if (modelFile == null) { // error-handling
            // TODO: consider updating this error toast to a log msg instead
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

        // Bind UI
        recordButton = findViewById(R.id.recordButton);
        playRecButton = findViewById(R.id.playRecButton);
        transcribeButton = findViewById(R.id.transcribeButton);
        recordText = findViewById(R.id.recordText);
        transcriptionText = findViewById(R.id.transcriptionOutput);

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

        // TRANSCRIBE button click
        transcribeButton.setOnClickListener(v -> doTranscribe()); // running in a background thread inside doTranscribe()
    }

    // -------------------- Recording --------------------

    private void startRecording() {
        try {
            pcmRecorder.start(wavPath);
            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();
            recordButton.setChecked(true);
        } catch (Exception e) {
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
            //give the user transcription instructions
            transcriptionText.setText("New audio recorded. Please press the \"Transcribe\" button to see the transcription");
            recordButton.setChecked(false);
        } catch (Exception e) {
            Log.e(TAG, "stopRecording failed", e);
            Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // -------------------- Playback --------------------

    private void startPlayback() {
        try {
            if (!wavPath.exists() || wavPath.length() < 2000) {
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
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception ignored) {
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
        if (modelFile == null || !modelFile.exists()) {
            // TODO: consider updating this error toast to a log msg instead
            Toast.makeText(this, "Model missing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!wavPath.exists() || wavPath.length() < 2000) { // audio too short; treating as empty
            // TODO: display a better msg for the user
            Toast.makeText(this, "No/short recording", Toast.LENGTH_SHORT).show();
            return;
        }

        // PROCESS: setting UI state to lock buttons while transcription in progress
        transcribeButton.setEnabled(false);
        transcribeButton.setText("Transcribing...");

        // PROCESS: creating background thread for native calls to avoid blocking main UI thread
        new Thread(() -> {

            // VARIABLE DECLARATION: JNI call
            String text = Whisper.transcribeWav(wavPath.getAbsolutePath());
            Log.i(TAG, "TRANSCRIPT: " + text);

            // PROCESS: switching back to main thread to update UI
            runOnUiThread(() -> {
                transcribeButton.setEnabled(true);
                transcribeButton.setText("Transcribe");

                // TODO: if the native returns a bracketed error, we currently just.. show it as-is,
                //  so we should consider updating them for better display to the user (or maybe re-try the logic?)
                transcriptionText.setText(text);
            });
        }, "whisper-transcribe").start();

    }

    // -------------------- Permissions --------------------

    private boolean hasRecordPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && recordPendingAfterPermission) {
                recordPendingAfterPermission = false;
                startRecording();
            } else if (!granted) {
                Toast.makeText(this, "Mic permission is required to record", Toast.LENGTH_LONG).show();
            }
        }
    }

    // -------------------- Asset copy --------------------

    /** Copy assets/models/<filename> to files/models/<filename> once, return the out File. */
    private File ensureModelCopiedOnce(String filename) {
        try {
            File outDir = new File(getFilesDir(), "models");
            if (!outDir.exists()) outDir.mkdirs();
            File out = new File(outDir, filename);
            if (!out.exists()) {
                try (InputStream in = getAssets().open("models/" + filename);
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[1 << 16];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
            return out;
        } catch (Exception e) {
            Log.e(TAG, "Model copy failed", e);
            return null;
        }
    }

    // -------------------- PCM -> WAV recorder (uses ACTUAL device sample rate) --------------------

    private static final class PcmRecorder {
        private static final int REQUESTED_SAMPLE_RATE = 16000; // request 16k, device may choose differently
        private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
        private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

        private AudioRecord audioRecord;
        private Thread thread;
        private volatile boolean running;
        private long bytesWritten = 0;
        private int actualSampleRate = REQUESTED_SAMPLE_RATE;

        /** Start capturing to temp PCM; later wrapped into WAV at stop(). */
        @SuppressLint("MissingPermission")
        public void start(File wavOut) throws IOException {
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

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC
                audioRecord.release();
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        REQUESTED_SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufSize);
            }

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IOException("AudioRecord init failed (state=" + audioRecord.getState() + ")");
            }

            actualSampleRate = audioRecord.getSampleRate();
            Log.i("PcmRecorder", "Recording started, requested=" + REQUESTED_SAMPLE_RATE +
                    " actual=" + actualSampleRate + ", bufSize=" + bufSize);

            File pcm = new File(wavOut.getParentFile(), "tmp_recording.pcm");
            FileOutputStream fos = new FileOutputStream(pcm);
            bytesWritten = 0;

            running = true;
            audioRecord.startRecording();

            thread = new Thread(() -> {
                byte[] buf = new byte[4096];
                try (FileOutputStream out = fos) {
                    while (running) {
                        int n = audioRecord.read(buf, 0, buf.length);
                        if (n > 0) {
                            out.write(buf, 0, n);
                            bytesWritten += n;
                        } else {
                            Log.w("PcmRecorder", "audioRecord.read=" + n);
                        }
                    }
                } catch (Exception e) {
                    Log.e("PcmRecorder", "writer error", e);
                }
            }, "pcm-writer");

            thread.start();
        }

        /** Stop capture, join thread, wrap PCM into a WAV with the ACTUAL sample rate. */
        public void stop(File wavOut) throws IOException, InterruptedException {
            running = false;
            if (audioRecord != null) {
                try { audioRecord.stop(); } catch (Exception ignore) {}
                audioRecord.release();
                audioRecord = null;
            }
            if (thread != null) {
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
            int byteRate = sampleRate * 2; // mono, 16-bit
            int blockAlign = 2;

            try (FileOutputStream out = new FileOutputStream(wav)) {
                out.write(new byte[]{'R','I','F','F'});
                writeLE32(out, 36 + dataLen);
                out.write(new byte[]{'W','A','V','E','f','m','t',' '});
                writeLE32(out, 16);
                writeLE16(out, 1);   // PCM
                writeLE16(out, 1);   // mono
                writeLE32(out, sampleRate);
                writeLE32(out, byteRate);
                writeLE16(out, blockAlign);
                writeLE16(out, 16);  // bits
                out.write(new byte[]{'d','a','t','a'});
                writeLE32(out, dataLen);
                out.write(pcmBytes);
            }
        }

        private static byte[] readAll(File f) throws IOException {
            if (Build.VERSION.SDK_INT >= 26) {
                return Files.readAllBytes(f.toPath());
            } else {
                byte[] buf = new byte[(int) f.length()];
                try (InputStream in = new java.io.FileInputStream(f)) {
                    int off = 0, n;
                    while ((n = in.read(buf, off, buf.length - off)) > 0) off += n;
                    return buf;
                }
            }
        }

        private static void writeLE16(OutputStream os, int v) throws IOException {
            os.write(v & 0xff); os.write((v >> 8) & 0xff);
        }
        private static void writeLE32(OutputStream os, int v) throws IOException {
            os.write(v & 0xff); os.write((v >> 8) & 0xff); os.write((v >> 16) & 0xff); os.write((v >> 24) & 0xff);
        }
    }
}
