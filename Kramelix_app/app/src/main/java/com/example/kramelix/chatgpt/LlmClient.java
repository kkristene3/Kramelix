package com.example.kramelix.chatgpt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.kramelix.feature.taskexe.controller.TaskController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * @author Alex Oprea, Amy Huang, Kristen Duong
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
     * This function synchronously invokes the Python
     * {@code whisper.chat(api_key, messages_json)} function.
     *
     * <p>This call performs network I/O through Python and must be run on a worker thread.
     * The function initializes the Python runtime on first use if not already started.</p>
     *
     * @param apiKey   The OpenAI (or compatible) API key.
     * @param messages OpenAI-style messages (system + history).
     * @return The assistant's response, or {@code null} if the Python module failed or returned nothing.
     */
    @Nullable
    public String complete(@Nullable String apiKey, @NonNull Iterable<? extends Map<String, String>> messages) {

        // PROCESS: initializing Chaquopy runtime once
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(app));
        }

        // PROCESS: acquiring the Python interpreter & target module
        Python py = Python.getInstance();
        PyObject mod = py.getModule("whisper");

        // PROCESS: serializing messages to JSON for robust interop
        String json;

        try {

            org.json.JSONArray arr = new org.json.JSONArray();

            for (java.util.Map<String, String> m : messages) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("role", m.get("role"));
                o.put("content", m.get("content"));
                arr.put(o);
            }

            json = arr.toString();

        } catch (org.json.JSONException e) { // error-handling
            // OUTPUT:
            return null;
        }

        // PROCESS: calling Python-side chat() safely
        PyObject resp = mod.callAttr("chat", (null == apiKey ? "" : apiKey), json);

        // Splitting the response into task command (index 0) and chat response (index 1)
        String[] respParts = splitMsg(resp.toString());

        // INITIALIZE: variables
        String llmMessage = respParts.length > 1 ? respParts[1].trim() : "";

        TaskController taskExe = TaskController.getInstance(app);

        // If the task command is recognized as a supported command, execute the task
        if (!"chat".equals(respParts[0]) &&
                !"other".equals(respParts[0]) &&
                !"unsupportedTask".equals(respParts[0])) {

            // call the executeTask function in the TaskController class
            boolean taskStatus = taskExe.executeTask(respParts[0]);

            // TODO: TEMPORARY - to see what the task is
            System.out.println(respParts[0]);

            // TODO: Have the LLM tell the user if the task was unable to be completed (taskStatus is false) > make the llm create a response
            //  The LLM should do this automatically if it is given the text FAILURETOTASKITUP > tested but needs cleanup on prompt for better response from llm

            // If llm cannot perform task, respond with the following message
            if (!taskStatus) {

                // PROCESS: create the failure prompt to sent to llm
                List<Map<String, String>> failurePrompt = new ArrayList<>();
                for (Map<String,String> m : messages) {
                    failurePrompt.add(m);
                }

                // PROCESS: create system message
                Map<String,String> failMsg = Map.of(
                        "role", "system",
                        "content", "FAILURETOTASKITUP: The previous task \"" + respParts[0] + "\" could not be completed. " +
                                "Reply to the user apologetically to inform them that their previous requested task failed. " +
                                "If the user simply says '[BLANK_AUDIO]', output clarification. Ask the user to repeat what they just said." +
                                "Your response should be short (1–3 sentences). Remember you can still play known music."
                );
                failurePrompt.add(failMsg);

                // PROCESS: call llm to get a response
                resp = mod.callAttr("chat", (apiKey == null ? "" : apiKey), new org.json.JSONArray(failurePrompt).toString());

                // PROCESS: return only the LLM message (ignore task command for failure)
                String[] failureParts = splitMsg(resp.toString());
                return failureParts.length > 1 ? failureParts[1].trim() : resp.toString();

            }
        }
        // OUTPUT: returning the response as a String
        if (respParts.length==1){
            return respParts[0];
        }


        // OUTPUT: if task succeeded or a task was not given, return regular LLM response
        return llmMessage.isEmpty() ? respParts[0] : llmMessage;
    }

    /**
     * This helper function separates LLM reply from task command
     *
     * @param msg The message generated by the LLM.
     * @return An array detailing [given task, LLM message]
     */
    @Nullable
    private static String[] splitMsg(String msg) {
        if (null == msg) {
            return null;
        }

        return msg.split("\\|");
    }
}
