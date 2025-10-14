package com.example.kramelix.chatgpt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

/**
 * This client provides a thin Java wrapper around the Python {@code whisper.py} module
 * via the Chaquopy runtime, exposing a simple synchronous API to call the Python-side
 * {@code chat(api_key, prompt)} function.
 *
 * <p>It abstracts away all Python initialization and interop logic, allowing higher-level
 * controllers (e.g. {@link com.example.kramelix.feature.transcribe.controller.TranscriptionController})
 * to invoke the model like a normal Java class.</p>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Must be called on a background thread — the Python bridge is blocking.</li>
 *     <li>The {@code whisper.py} script must exist in the app's Python source tree.</li>
 *     <li>Model/API initialization should occur once before repeated calls.</li>
 * </ul>
 *
 * @author Alex Oprea, Amy Huang
 * @since 1.0
 */
public final class LlmClient {

    // -------------------- STATE --------------------

    /**
     * Application context used for Chaquopy initialization.
     */
    @NonNull
    private final Context app;

    // -------------------- LIFECYCLE --------------------

    /**
     * Constructor.
     *
     * @param context Any valid {@link Context}; the application context will be retained.
     */
    private LlmClient(@NonNull Context context) {
        // INITIALIZATION:
        app = context.getApplicationContext();
    }

    /**
     * Factory instance.
     *
     * @param context Any valid {@link Context}; the application context will be retained.
     * @return The controller instance.
     */
    public static LlmClient createLlmClient(@NonNull Context context) {
        // OUTPUT:
        return new LlmClient(context);
    }

    // -------------------- CHAT COMPLETION --------------------

    /**
     * This function synchronously invokes the Python {@code whisper.chat(api_key, prompt)} function.
     *
     * <p>This call performs network I/O through Python and must be run on a worker thread.
     * The function initializes the Python runtime on first use if not already started.</p>
     *
     * @param apiKey The OpenAI (or compatible) API key.
     * @param prompt The user prompt text.
     * @return The assistant's response, or {@code null} if the Python module failed or returned nothing.
     */
    @Nullable
    public String complete(@Nullable String apiKey, @Nullable String prompt) {

        // PROCESS: initializing Chaquopy runtime once
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(app));
        }

        // PROCESS: acquiring the Python interpreter & target module
        Python py = Python.getInstance();
        PyObject mod = py.getModule("whisper");

        // PROCESS: calling Python-side chat() safely
        PyObject resp = mod.callAttr("chat", null != apiKey ? apiKey : "", null != prompt ? prompt : "");

        // OUTPUT: returning the response as a String (or null)
        return (null != resp) ? resp.toString() : null;

    }

}
