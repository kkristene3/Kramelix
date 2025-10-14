package com.example.kramelix.feature.tts.controller;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * This controller wraps Android's {@link TextToSpeech} engine to handle
 * voice playback for LLM responses and other synthesized text.
 *
 * <p>It initializes a {@link TextToSpeech} instance tied to the application
 * context, sets a default language (US English), and exposes a simple API to
 * speak text or shut down the TTS engine cleanly.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Initializes & configures a {@link TextToSpeech} instance.</li>
 *     <li>Speaks strings on demand (flushing any previous utterances).</li>
 *     <li>Stop & releases resources when no longer needed.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Caller must retain this controller for the app/session lifetime (avoid recreating each call).</li>
 *     <li>Always call {@link #shutdown()} from {@code onDestroy()} or equivalent lifecycle point.</li>
 *     <li>TTS runs asynchronously; calling {@link #speak(String)} before initialization may queue speech late.</li>
 * </ul>
 *
 * @author Kristen Duong, Amy Huang
 * @since 1.0
 */
public final class TTSController implements TextToSpeech.OnInitListener {

    // -------------------- CONFIGURATION --------------------

    /**
     * Logcat tag for diagnostics.
     */
    private static final String TAG = "TTSController";

    /**
     * Default locale (US English).
     */
    private static final Locale DEFAULT_LOCALE = Locale.US;

    // -------------------- STATE --------------------

    /**
     * Underlying Android Text-to-Speech engine.
     */
    @Nullable
    private TextToSpeech textToSpeech;

    /**
     * Indicates whether the engine has successfully initialized.
     */
    private volatile boolean initialized;

    // -------------------- LIFECYCLE --------------------

    /**
     * Constructor.
     *
     * @param context Any valid {@link Context}; the application context is retained internally.
     */
    private TTSController(@NonNull Context context) {
        // INITIALIZATION:
        //noinspection ThisEscapedInObjectConstruction
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
    }

    /**
     * Factory instance.
     *
     * @param context Any valid {@link Context}; the application context is retained internally.
     * @return The controller instance.
     */
    public static TTSController getInstance(@NonNull Context context) {
        return new TTSController(context);
    }

    /**
     * Callback triggered after {@link TextToSpeech} initialization.
     *
     * @param status The initialization status; {@link TextToSpeech#SUCCESS} or {@link TextToSpeech#ERROR}.
     */
    @Override
    public void onInit(int status) {

        if (TextToSpeech.SUCCESS == status && null != textToSpeech) {

            int result = textToSpeech.setLanguage(DEFAULT_LOCALE);

            if (TextToSpeech.LANG_MISSING_DATA == result || TextToSpeech.LANG_NOT_SUPPORTED == result) {
                // LOG OUTPUT:
                Log.w(TAG, "Selected language not supported or missing data.");
            } else {

                initialized = true; // updating flag

                // LOG OUTPUT:
                Log.i(TAG, "TTS initialized with locale: " + DEFAULT_LOCALE);

            }
        } else {
            // LOG OUTPUT:
            Log.e(TAG, "TTS initialization failed (status=" + status + ")");
        }

    }

    // -------------------- SPEECH --------------------

    /**
     * This function converts text into speech.
     *
     * @param text The string to speak. Empty or {@code null} text is ignored.
     * @noinspection TypeMayBeWeakened
     */
    public void speak(@Nullable String text) {

        if (null == text || text.isEmpty()) return;

        if (!initialized || null == textToSpeech) {
            // LOG OUTPUT:
            Log.w(TAG, "TTS not initialized yet; speech ignored.");
            return;
        }

        try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-utterance");
        } catch (RuntimeException e) { // error-handling
            // TODO: maybe consider restarting the instance or smth?
            // LOG OUTPUT:
            Log.e(TAG, "TTS speak failed", e);
        }

    }

    // -------------------- SHUTDOWN --------------------

    /**
     * This function stops ongoing speech (if any) & releases all TTS resources.
     *
     * <p>Must be called from {@code onDestroy()} or when the owning component
     * is no longer visible to ensure proper resource cleanup.</p>
     */
    public void shutdown() {

        if (null != textToSpeech) {

            try {

                textToSpeech.stop();
                textToSpeech.shutdown();

                // LOG OUTPUT:
                Log.i(TAG, "TTS engine shut down successfully.");

            } catch (RuntimeException e) {
                // LOG OUTPUT:
                Log.w(TAG, "TTS shutdown failed", e);
            } finally { // resetting all flags

                textToSpeech = null;
                initialized = false;

            }

        }

    }

}
