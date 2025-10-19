/**
 * Provides a thin Java–Python bridge for executing chat completions via the Chaquopy runtime.
 *
 * <p>This package contains the {@link com.example.kramelix.chatgpt.LlmClient}, a lightweight
 * client that delegates text generation to a Python module (typically {@code whisper.py})
 * using the embedded Chaquopy interpreter. It allows the rest of the Android codebase to
 * call large language models (LLMs) like native Java services.</p>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Separation of concerns:</b> Java code interacts only through {@link com.example.kramelix.chatgpt.LlmClient};
 *     Python details remain isolated in {@code whisper.py}.</li>
 *     <li><b>Threading:</b> calls are blocking and must be run on a background thread to avoid UI freezes.</li>
 *     <li><b>Integration:</b> designed to be composed by
 *     {@link com.example.kramelix.feature.transcribe.controller.TranscriptionController}
 *     as part of the end-to-end speech-to-response pipeline.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>The {@code whisper.py} file must exist in the Python source directory included in the app build.</li>
 *     <li>Chaquopy automatically manages the Python interpreter lifecycle; callers should not stop it manually.</li>
 *     <li>Network operations or model calls performed inside {@code whisper.py} must handle their own errors.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * LlmClient llm = LlmClient.createLlmClient(context);
 * String reply = llm.complete(BuildConfig.OPENAI_API_KEY, "Hello!");
 * }</pre>
 *
 * @author Alex Oprea, Amy Huang
 * @since 1.0
 */
package com.example.kramelix.chatgpt;
