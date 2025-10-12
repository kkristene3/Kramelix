package com.example.kramelix.controller;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * This class will handle Text-to-Speech for the LLM
 */
public class TTSController implements TextToSpeech.OnInitListener {
    private static final String TAG = "TTSController";

    private final TextToSpeech textToSpeech;

    TTSController(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public final void onInit(int status) {

        // if no error found
        if (TextToSpeech.ERROR != status) {
            // voice will be with English accent
            textToSpeech.setLanguage(Locale.US);

        }
    }

    /**
     * Convert text to speech
     * @param text - llm's response
     */
    public final void speak(String text) {
        if (null == text || text.isEmpty()) return;
        try {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        } catch (RuntimeException e) {
            Log.e(TAG, "TTS speach failed");
        }
    }

    // -------------------- SHUTDOWN TTS --------------------

    /**
     * Shuts down the TTS listener
     */
    public final void shutdown() {
        try {
            if (null != textToSpeech) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "TTS shutdown failed", e);
        }
    }
}
