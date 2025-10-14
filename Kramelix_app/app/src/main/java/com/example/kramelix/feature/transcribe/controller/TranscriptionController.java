package com.example.kramelix.feature.transcribe.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.kramelix.BuildConfig;
import com.example.kramelix.feature.chat.controller.ChatController;
import com.example.kramelix.feature.chat.model.Message;
import com.example.kramelix.feature.chat.model.Role;
import com.example.kramelix.chatgpt.LlmClient;
import com.example.kramelix.whisperjni.Whisper;

import java.io.File;
import java.util.function.Consumer;

/**
 * This controller orchestrates the Whisper model initialization, transcription of a WAV recording,
 * UX chat bubbles updates, LLM calls, and invokes TTS.
 *
 * <p>Heavy work runs on a worker thread; lightweight UX (toasts) are posted to the main thread via a {@link Handler}.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Initializes Whisper model once per app session after the model file is copied to internal storage.</li>
 *     <li>Runs transcription on a background thread & updates the chat history (pending to final text).</li>
 *     <li>Calls the LLM with the transcript & updates the assistant bubble (pending to final text).</li>
 *     <li>Invokes TTS for the assistant response (owned by caller via a {@code Consumer<String>}).</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Caller must pass a valid absolute model path to {@link #isModelInitialized(String)} before transcription.</li>
 *     <li>WAV file must exist and be non-trivial; very small files are treated as empty recordings.</li>
 *     <li>UI code (RecyclerView, adapters) lives outside; this controller only updates chat state via {@link ChatController}.</li>
 * </ul>
 *
 * @author Amy Huang, Alex Oprea, Kristen Duong
 * @since 1.0
 */
public final class TranscriptionController {

    // -------------------- CONFIGURATION --------------------

    /**
     * Minimal byte length to consider a recording 'usable' (guards against headers-only / empty).
     */
    private static final long MIN_WAV_BYTES = 2000L;

    /**
     * Worker thread name for debugging.
     */
    private static final String THREAD_NAME = "whisper-transcribe";

    /**
     * Logcat tag.
     */
    private static final String TAG = "TranscriptionController";

    // -------------------- STATE --------------------

    /**
     * Application context for toasts & main looper access.
     */
    @NonNull
    private final Context ctx;

    /**
     * Chat façade to add/update bubbles (pending to final).
     */
    @NonNull
    private final ChatController chat;

    /**
     * LLM client abstraction (network call).
     */
    @NonNull
    private final LlmClient llm;

    /**
     * Main-thread handler for UI toasts (& any future UI posts).
     */
    @NonNull
    private final Handler mainHandler;

    /**
     * Absolute path to the Whisper model; set once on successful init.
     */
    @Nullable
    private String modelPathAbs;

    // -------------------- LIFECYCLE --------------------

    /**
     * Constructor.
     *
     * @param ctx  Any context; its application context will be retained.
     * @param chat The chat controller used to mutate conversation state.
     * @param llm  The LLM client used to produce assistant replies.
     */
    private TranscriptionController(@NonNull Context ctx, @NonNull ChatController chat, @NonNull LlmClient llm) {

        // INITIALIZATION:
        this.ctx = ctx.getApplicationContext(); // avoiding Activity leak by storing application context
        this.chat = chat;
        this.llm = llm;

        mainHandler = new Handler(Looper.getMainLooper());

    }

    /**
     * Factory instance.
     *
     * @param ctx  Any context; its application context will be retained.
     * @param chat The chat controller used to mutate conversation state.
     * @param llm  The LLM client used to produce assistant replies.
     * @return The controller instance.
     */
    public static TranscriptionController getInstance(@NonNull Context ctx, @NonNull ChatController chat, @NonNull LlmClient llm) {
        // OUTPUT:
        return new TranscriptionController(ctx, chat, llm);
    }

    // -------------------- MODEL INIT --------------------

    /**
     * This function initializes the Whisper model (after the .bin is available in app files).
     *
     * @param modelPathAbs The absolute filesystem path to the Whisper model file.
     * @return {@code true} if the native whisper context initializes successfully; {@code false} otherwise.
     */
    @SuppressLint("LogConditional")
    public boolean isModelInitialized(@NonNull String modelPathAbs) {

        // INITIALIZATION:
        this.modelPathAbs = modelPathAbs;
        boolean ok = Whisper.initModel(modelPathAbs);

        // LOG OUTPUT:
        Log.i(TAG, "Whisper.initModel = " + ok);

        if (!ok) { // error-handling
            // TODO: add error-handling here
            postToast("Whisper init failed");
        }

        // OUTPUT:
        return ok;

    }

    // -------------------- PIPELINE --------------------

    /**
     * This function creates a new thread pipeline:
     * add USER pending msg -> transcribe -> replace text
     * -> add ASSISTANT pending msg -> call LLM -> replace text
     * -> TTS.
     *
     * <p>All blocking work is performed on a worker thread. Chat updates are safe because the
     * {@link ChatController} posts list snapshots back to the UI layer via LiveData.</p>
     *
     * @param wavPath The path to the recorded WAV file.
     * @param speak   The callback that performs TTS for the final assistant text (e.g. {@code ttsController::speak}).
     * @noinspection OverlyComplexMethod, OverlyLongLambda
     */
    @SuppressLint("LogConditional")
    public void transcribeAndReply(@NonNull File wavPath, @NonNull Consumer<? super String> speak) {

        // PROCESS: running the pipeline on a background thread to avoid blocking UI
        new Thread(() -> {

            try {

                // TODO: implement better error-handling for these guards
                // PROCESS: checking for preconditions
                if (null == modelPathAbs) {
                    // OUTPUT:
                    postToast("Model missing");
                    return;
                }

                if (!wavPath.exists() || MIN_WAV_BYTES >= wavPath.length()) {
                    // OUTPUT:
                    postToast("No/short recording");
                    return;
                }

                // PROCESS: adding USER pending bubble to UX (pulses + dots)
                Message userPending = chat.addPending(Role.USER, "…");

                // PROCESS: calling JNI on worker thread (to avoid jank)
                String text = Whisper.transcribeWav(wavPath.getAbsolutePath());

                // LOG OUTPUT:
                Log.i(TAG, "TRANSCRIPT: " + text);

                // PROCESS: replacing USER pending w/ real transcript
                String safeUserText = (null == text || text.isBlank()) ? "[empty transcript]" : text;
                chat.update(userPending, safeUserText, false);

                // PROCESS: adding ASSISTANT pending bubble (pulses + dots)
                Message assistantPending = chat.addPending(Role.ASSISTANT, "…");

                // PROCESS: calling LLM (still on worker thread)
                String llmText;

                try {
                    llmText = llm.complete(BuildConfig.OPENAI_API_KEY, safeUserText);
                } catch (RuntimeException e) { // error-handling

                    // LOG OUTPUT:
                    Log.e(TAG, "LLM call failed", e);

                    // TODO: output a better msg for the user
                    llmText = "[llm error: " + e.getClass().getSimpleName() + "]";

                }

                String safeResp = (null == llmText || llmText.isBlank()) ? "[no response given]" : llmText;

                // PROCESS: replacing ASSISTANT pending w/ final response
                chat.update(assistantPending, safeResp, false);

                // PROCESS: running TTS
                if (!"[no response given]".equals(safeResp)) {

                    try {
                        speak.accept(safeResp); // e.g. ttsController.speak(safeResp)
                    } catch (RuntimeException ttsErr) { // error-handling
                        // LOG OUTPUT:
                        Log.w(TAG, "TTS failed", ttsErr);
                    }

                }

            } catch (RuntimeException e) { // error-handling

                // LOG OUTPUT:
                Log.e(TAG, "Transcription pipeline failed", e);

                // TODO: add next steps for user
                postToast("Transcription failed: " + e.getMessage());

            }

        }, THREAD_NAME).start();

    }

    // -------------------- HELPERS --------------------

    /**
     * This helper function posts a toast to the main thread.
     */
    private void postToast(@NonNull CharSequence msg) {
        // OUTPUT:
        mainHandler.post(() -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }

}
