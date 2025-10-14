/**
 * Coordinates the presentation of chat messages between the model and the UI layer.
 *
 * <p>This package contains a controller that binds observable conversation data to
 * RecyclerView-based views, ensuring chat bubbles update reactively as messages
 * are added or modified by other features (e.g. transcription or LLM responses).</p>
 *
 * <ul>
 *     <li>{@link com.example.kramelix.feature.chat.controller.ChatController} —
 *     binds a {@link androidx.recyclerview.widget.RecyclerView} to the
 *     {@link com.example.kramelix.feature.chat.model.ConversationRepository}
 *     stream, configures layout behavior, and exposes helper methods to add or
 *     update chat bubbles.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Reactive binding:</b> observes LiveData from the conversation repository
 *     & updates the adapter automatically.</li>
 *     <li><b>UI independence:</b> does not own any Activity/Fragment references;
 *     callers provide lifecycle context and view instances.</li>
 *     <li><b>Auto-scrolling:</b> maintains a bottom-stacked layout & scrolls to
 *     the most recent message on update.</li>
 *     <li><b>Thread safety:</b> repository-level synchronization ensures safe access
 *     even when updates originate from background threads.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>Must be bound on the main (UI) thread using
 *     {@link com.example.kramelix.feature.chat.controller.ChatController#bind}.</li>
 *     <li>Each controller instance is scoped to a single RecyclerView.</li>
 *     <li>All message creation and updates flow through the repository API.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * ChatController chatController = new ChatController();
 * chatController.bind(chatRecyclerView, getViewLifecycleOwner());
 *
 * // Add a temporary user bubble
 * Message pending = chatController.addPending(Role.USER, "…");
 *
 * // Replace it with final text later
 * chatController.update(pending, "final transcript", false);
 * }</pre>
 *
 * @author Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.chat.controller;
