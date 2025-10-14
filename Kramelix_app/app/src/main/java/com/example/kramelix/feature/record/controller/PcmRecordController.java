package com.example.kramelix.feature.record.controller;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * This controller class records raw PCM from the microphone on a background thread & finalizes it as a WAV file.
 *
 * <p>It requests {@code 16 kHz}, mono, 16-bit PCM and then uses the device's
 * <em>actual</em> input sample rate reported by {@link AudioRecord#getSampleRate()}. Audio is
 * written to a temporary {@code tmp_recording.pcm} file during capture, and on {@link #stop(File)},
 * it is wrapped into a standard RIFF/WAVE container (mono, 16-bit) at the observed sample rate.</p>
 *
 * <br>
 * <strong>Lifecycle</strong>
 * <ol>
 *     <li>{@link #start(File)} — initializes {@link AudioRecord}, spawns a writer thread,
 *     & streams PCM frames into a temp file next to the destination WAV.</li>
 *     <li>{@link #stop(File)} — stops capture, joins the writer thread, converts the temp PCM into a WAV file,
 *     & deletes the temp file.</li>
 * </ol>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *   <li>Caller must have {@code android.permission.RECORD_AUDIO} granted before calling {@link #start(File)}.</li>
 *   <li>This class is not re-entrant. Call {@link #stop(File)} before {@link #start(File)} again.</li>
 *   <li>The destination directory for the WAV file must exist.</li>
 * </ul>
 *
 * @author Kristen Duong, Amy Huang
 * @noinspection OverlyBroadThrowsClause, OverlyLongLambda
 * @since 1.0
 */
final class PcmRecordController {

    /**
     * Logcat tag used by this class.
     */
    private static final String TAG = "PcmRecordController";

    // -------------------- CONFIGURATION --------------------

    /**
     * Whisper's requested sample rate in Hz.
     */
    private static final int REQUESTED_SAMPLE_RATE = 16000; // device may choose a diff rate

    /**
     * Audio channel configuration (mono input).
     *
     * @see AudioFormat#CHANNEL_IN_MONO
     */
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    /**
     * PCM sample format (16-bit).
     *
     * @see AudioFormat#ENCODING_PCM_16BIT
     */
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    /**
     * Temporary filename (raw PCM) created next to the destination WAV during recording.
     */
    private static final String TEMP_PCM_NAME = "tmp_recording.pcm";

    // -------------------- STATE --------------------

    /**
     * Low-level audio capture object; non-null only while recording.
     *
     * @see AudioRecord
     */
    @Nullable
    private AudioRecord audioRecord;

    /**
     * Background thread that pulls PCM from {@link #audioRecord} & writes to the temp file.
     */
    @Nullable
    private Thread thread;

    /**
     * Flag polled by the writer thread; set to {@code false} to stop capture.
     */
    private volatile boolean running;

    /**
     * Number of bytes written to the temporary PCM file so far.
     */
    private long bytesWritten;

    /**
     * Actual input sample rate observed from {@link AudioRecord#getSampleRate()} (Hz).
     */
    private int actualSampleRate = REQUESTED_SAMPLE_RATE;

    // -------------------- API --------------------

    /**
     * This function begins capturing microphone PCM into a temporary raw file.
     * The temporary file is stored in the same directory as the destination WAV
     * and is converted on {@link #stop(File)}.
     * Permissions: caller must have {@code RECORD_AUDIO} granted.
     *
     * @param wavOut The destination WAV file path.
     * @throws IOException If {@link AudioRecord} can't be initialized or the temp file can't be created.
     * @implNote This method doesn't create the WAV file yet; it only opens the temp PCM & starts the writer thread.
     */
    @SuppressLint({"MissingPermission", "LogConditional"})
    void start(File wavOut) throws IOException {

        // VARIABLE DECLARATION:
        int minBuf = AudioRecord.getMinBufferSize(REQUESTED_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int bufSize = Math.max(minBuf, 4096);

        // PROCESS: creating new audio record
        int source = MediaRecorder.AudioSource.VOICE_RECOGNITION; // prefer VOICE_RECOGNITION (often yields 16 kHz on some OEMs)
        audioRecord = new AudioRecord(
                source,
                REQUESTED_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufSize
        );

        // PROCESS: verifying initialization
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) { // fallback to MIC if needed

            // PROCESS: releasing the failed AudioRecord instance before retrying
            audioRecord.release();

            // PROCESS: retrying with MIC source (more universally supported)
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    REQUESTED_SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufSize
            );

        }

        // ERROR-HANDLING: bailing if AudioRecord still failed to initialize
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            // TODO: handle this exception better; display a msg to the user maybe?
            throw new IOException(TAG + ": AudioRecord init failed (state=" + audioRecord.getState() + ")");
        }

        // VARIABLE DECLARATION: capturing the device's actual sample rate
        actualSampleRate = audioRecord.getSampleRate();

        // LOG OUTPUT: diagnostics
        Log.i(TAG, "Recording started, requested=" + REQUESTED_SAMPLE_RATE +
                " actual=" + actualSampleRate + ", bufSize=" + bufSize);

        bytesWritten = 0L; // resetting counter
        running = true; // updating flag

        // PROCESS: starting the capture device
        audioRecord.startRecording();

        // PROCESS: spawning background writer loop (avoids blocking main UI thread)
        thread = new Thread(() -> {

            // VARIABLE DECLARATION: local write buffer for PCM pulls
            //noinspection CheckForOutOfMemoryOnLargeArrayAllocation
            byte[] buf = new byte[4096]; // suppressed warning bc small buffer reused within thread

            // VARIABLE DECLARATION:
            File pcm = new File(wavOut.getParentFile(), TEMP_PCM_NAME); // temp raw PCM file path

            // PROCESS: use try-with-resources so stream is closed even on exceptions
            try (FileOutputStream out = new FileOutputStream(pcm)) {

                // PROCESS: loop until stop() clears `running`
                while (running) {

                    // VARIABLE DECLARATION: pulling chunk of PCM from mic
                    int n = audioRecord.read(buf, 0, buf.length);

                    if (0 < n) {
                        // PROCESS: append to temp PCM file
                        out.write(buf, 0, n);
                        bytesWritten += n; // incrementing counter
                    } else {
                        // LOG OUTPUT:
                        Log.w(TAG, "audioRecord.read=" + n);
                    }

                }

            } catch (FileNotFoundException e) { // error-handling
                // TODO: display a msg to the user here
                // LOG OUTPUT:
                Log.e(TAG, "file not found error", e);
            } catch (IOException e) { // error-handling
                // TODO: display a msg to the user here
                // LOG OUTPUT:
                Log.e(TAG, "writer error", e);
            }

        }, "pcm-writer");

        // PROCESS: launching the background writer
        thread.start();

    }

    /**
     * This function stops the audio capture, joins the writer thread,
     * & convert the temp PCM file into a final WAV file,
     * using the observed {@link #actualSampleRate}.
     *
     * @param wavOut Destination WAV file to write (overwritten if it already exists).
     * @throws IOException          If the temp PCM file is missing or I/O fails while writing the WAV.
     * @throws InterruptedException If the writer thread join is interrupted.
     * @implNote After successful conversion, the temporary PCM file is deleted.
     */
    @SuppressLint("LogConditional")
    void stop(File wavOut) throws IOException, InterruptedException {

        // PROCESS: signalling the writer thread to exit its read/write loop
        running = false;

        // PROCESS: stopping & releasing AudioRecord
        if (null != audioRecord) { // active

            try {
                audioRecord.stop(); // attempting to stop
            } catch (IllegalStateException ignore) {
            } // no op. needed; already stopped or not started

            // PROCESS: freeing the native audio resources
            audioRecord.release();
            audioRecord = null;

        }

        // PROCESS: ensuring the background writer thread fully terminates
        if (null != thread) {
            thread.join(); // may throw InterruptedException
            thread = null;
        }

        // VARIABLE DECLARATION: path to the temp PCM file that the writer thread produced
        File pcm = new File(wavOut.getParentFile(), TEMP_PCM_NAME);

        // ERROR-HANDLING: ensuring we actually captured PCM before attempting to wrap WAV
        if (!pcm.exists()) throw new IOException(TAG + ": No PCM file written");

        // PROCESS: wrapping raw PCM into a proper WAV container using the actual sample rate
        writeWavFromPcm16Mono(pcm, wavOut, actualSampleRate);

        // VARIABLE DECLARATION: final WAV size (for diagnostics)
        long wavLen = wavOut.length();

        // PROCESS: cleaning up the temp PCM file
        //noinspection ResultOfMethodCallIgnored
        pcm.delete();

        // LOG OUTPUT: diagnostics
        Log.i(TAG, "Recording stopped. bytesWritten=" + bytesWritten +
                ", wavSize=" + wavLen + ", sr=" + actualSampleRate);

    }

    // -------------------- WAV writer helpers --------------------

    /**
     * This helper function serializes a raw PCM16 mono buffer into a standard RIFF/WAVE file.
     *
     * @param pcm        The source raw PCM file to be converted (mono, 16-bit).
     * @param wav        The destination WAV file to write. Overwritten if it already exists.
     * @param sampleRate The sample rate (in Hz) to encode in the WAV header.
     * @throws IOException If reading the PCM file or writing the WAV file fails.
     */
    private static void writeWavFromPcm16Mono(File pcm, File wav, int sampleRate) throws IOException {

        // VARIABLE DECLARATION: loading entire PCM file into memory
        byte[] pcmBytes = readAll(pcm); // usually small, so should be safe

        // VARIABLE DECLARATION: header math for WAV fields
        int dataLen = pcmBytes.length;
        int byteRate = sampleRate << 1; // mono, 16-bit means 2 bytes per sample
        final int blockAlign = 2;

        // PROCESS: opening output stream for the destination WAV
        try (FileOutputStream out = new FileOutputStream(wav)) {

            out.write(new byte[]{'R', 'I', 'F', 'F'}); // RIFF subchunk ID
            writeLE32(out, 36 + dataLen); // RIFF chunk size & (fixed PCM header size + data)

            out.write(new byte[]{'W', 'A', 'V', 'E'}); // WAVE format tag

            out.write(new byte[]{'f', 'm', 't', ' '}); // FMT subchunk ID
            writeLE32(out, 16); // FMT chunk size
            writeLE16(out, 1); // PCM format code = 1
            writeLE16(out, 1); // channels = 1 (mono)
            writeLE32(out, sampleRate);
            writeLE32(out, byteRate);
            writeLE16(out, blockAlign);
            writeLE16(out, 16); // bits per sample = 16

            out.write(new byte[]{'d', 'a', 't', 'a'}); // Data subchunk ID
            writeLE32(out, dataLen);
            out.write(pcmBytes); // writing raw PCM bytes

        }

    }

    /**
     * This helper function reads the entire contents of a file into memory as a byte array.
     *
     * @param f The file to read from.
     * @return A byte array containing the full file contents.
     * @throws IOException If the file cannot be read (missing, inaccessible, or I/O error).
     */
    private static byte[] readAll(File f) throws IOException {

        // PROCESS: choosing NIO on newer Android versions for simplicity/performance
        if (26 <= Build.VERSION.SDK_INT) {

            // OUTPUT: returning contents as a byte array
            return Files.readAllBytes(f.toPath());

        } else {

            // PROCESS: manual stream copy fallback on older API levels
            //noinspection NumericCastThatLosesPrecision
            byte[] buf = new byte[(int) f.length()];

            try (InputStream in = new FileInputStream(f)) {

                int off = 0, n;

                // PROCESS: reading until EOF & tracking offset
                while (0 < (n = in.read(buf, off, buf.length - off))) off += n;

                // OUTPUT: returning the fully filled buffer
                return buf;

            }

        }

    }

    /**
     * This helper function writes a 16-bit integer in little-endian order to the given stream.
     *
     * @param os The output stream to write to.
     * @param v  The 16-bit integer value to write.
     * @throws IOException If an I/O error occurs while writing to the stream.
     */
    private static void writeLE16(OutputStream os, int v) throws IOException {

        // OUTPUT: writing low byte, then high byte
        os.write(v & 0xff);
        os.write((v >> 8) & 0xff);

    }

    /**
     * This helper function writes a 32-bit integer in little-endian order to the given stream.
     *
     * @param os The output stream to write to.
     * @param v  The 32-bit integer value to write.
     * @throws IOException If an I/O error occurs while writing to the stream.
     */
    private static void writeLE32(OutputStream os, int v) throws IOException {

        // OUTPUT: writing least-significant byte to most-significant
        os.write(v & 0xff);
        os.write((v >> 8) & 0xff);
        os.write((v >> 16) & 0xff);
        os.write((v >> 24) & 0xff);

    }

}
