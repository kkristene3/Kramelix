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
 *     A process-wide singleton that parses abstract task commands and dispatches
 *     them to specialized feature controllers (e.g. music playback, alarms, calls, et.c).</li>
 *
 *     <li>{@link com.example.kramelix.feature.taskexe.controller.CallController} —
 *     A dedicated controller for phone-call functionality. It handles both direct
 *     number calls and contact-name lookups, including permission checks and
 *     Unicode-safe name normalization.</li>
 *
 *     <li>{@link com.example.kramelix.feature.taskexe.controller.AlarmReceiver} —
 *     A {@link android.content.BroadcastReceiver} triggered by the system
 *     {@link android.app.AlarmManager}, responsible for delivering audible feedback
 *     and triggering alarm events when scheduled times are reached.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li>System Integration: relies on Android framework services
 *     (e.g. media, telephony, alarms, intents).</li>
 *     <li>Controller Encapsulation: feature-specific logic is modularized
 *     in controllers such as {@code CallController}, keeping {@code TaskController}
 *     focused on parsing and dispatch.</li>
 *     <li>Graceful Degradation: all runtime permission and I/O failures
 *     are handled safely without app termination.</li>
 *     <li>Extensibility: structured to grow into a broader suite of
 *     assistant capabilities including messaging, calendar, and web search.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Long-running operations must not execute on the UI thread.</li>
 *     <li>Runtime permissions (e.g. recording, calling, contacts) must be
 *     requested and verified externally before task execution.</li>
 *     <li>Controllers must validate input defensively and fail gracefully.</li>
 *     <li>Alarm delivery relies on manifest registration of
 *     {@link com.example.kramelix.feature.taskexe.controller.AlarmReceiver}.</li>
 * </ul>
 *
 * <br>
 * <strong>Example (future pattern)</strong>
 * <pre>{@code
 * // From an LLM-parsed command such as:
 * // {"task": "call", "target": "Amy"}
 * new CallController(context).handleCallRequest("Amy");
 *
 * // Or routed through TaskController:
 * TaskController.getInstance(context).executeTask("playMusic(\"Yellow\")");
 * }</pre>
 *
 * @author Alex Oprea, Kristen Duong, Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.taskexe.controller;
