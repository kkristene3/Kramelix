package com.example.kramelix.controller;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

/**
 * This class will handle Text-to-Speech for the LLM
 */
public class TTSController implements TextToSpeech.OnInitListener{
    private static final String TAG = "TTSController";

    private final TextToSpeech textToSpeech;

    public TTSController(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {

        // if no error found
        if(status!=TextToSpeech.ERROR){
            // voice will be with English accent
            textToSpeech.setLanguage(Locale.US);

        }
    }

    /**
     * Convert text to speech
     * @param text - llm's response
     */
    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        try{
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        } catch (Exception e) {
            Log.e(TAG, "speak failed");
        }
    }

    // -------------------- SHUTDOWN TTS --------------------
    public void shutdown() {
        try {
            if (textToSpeech != null) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
        } catch (Exception e) {
            Log.w(TAG, "shutdown tts failed", e);
        }
    }
}
