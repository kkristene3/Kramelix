/**
 * Executes device-level actions recognized and requested by the LLM.
 *
 * <p>This package contains controllers responsible for interpreting high-level
 * assistant "tasks" (e.g. play music, set an alarm, etc.) and mapping them to actual
 * Android system services or behaviors. It represents the operational layer of
 * Kramelix's future phone-assistant capabilities.</p>
 *
 * <ul>
 *     <li>{@link com.example.kramelix.feature.taskexe.controller.TaskController} —
 *     A process-wide singleton which parses task commands and dispatches calls to
 *     music playback, alarm scheduling, and other system features.</li>
 *
 *     <li>{@link com.example.kramelix.feature.taskexe.controller.AlarmReceiver} —
 *     A {@link android.content.BroadcastReceiver} triggered by the system
 *     {@link android.app.AlarmManager}, responsible for delivering feedback and
 *     ringtone playback when scheduled alarms go off.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>System Integration:</b> uses Android framework services (media,
 *     alarms, intents).</li>
 *     <li><b>Controller Encapsulation:</b> business logic is centralized in
 *     {@code TaskController}, separate from LLM and UI layers.</li>
 *     <li><b>Graceful Degradation:</b> unsupported or malformed task requests
 *     must fail safely without app crashes.</li>
 *     <li><b>Extensibility:</b> structured to grow into a broader suite of
 *     assistant capabilities such as calling, messaging, calendar, and web search.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Long-running operations should not execute on the UI thread.</li>
 *     <li>Runtime permissions (e.g. recording, calling, contacts) must be
 *     requested and verified externally before execution.</li>
 *     <li>Controllers must validate task formats defensively.</li>
 *     <li>Alarm delivery relies on proper manifest registration of
 *     {@link com.example.kramelix.feature.taskexe.controller.AlarmReceiver}.</li>
 * </ul>
 *
 * <br>
 * <strong>Example (future pattern)</strong>
 * <pre>{@code
 * // From an LLM-parsed command such as:
 * // {"task": "play_music", "song": "Yellow"}
 * TaskController.getInstance(context).executeTask("playMusic(\"Yellow\")");
 * }</pre>
 *
 * @author Alex Oprea, Kristen Duong
 * @since 1.0
 */
package com.example.kramelix.feature.taskexe.controller;
