/**
 * UI components for rendering the chat conversation.
 *
 * <p>This package contains RecyclerView-facing classes that visualize the
 * conversation maintained by
 * {@link com.example.kramelix.feature.chat.model.ConversationRepository}.
 * It purely focuses on view concerns (inflation, binding, lightweight animations),
 * while controllers and models live in their respective packages.</p>
 *
 * <br>
 * <strong>Contents</strong>
 * <ul>
 *     <li>{@link com.example.kramelix.feature.chat.view.ChatAdapter} — binds
 *     {@link com.example.kramelix.feature.chat.model.Message} items to chat bubble rows,
 *     distinguishes {@link com.example.kramelix.feature.chat.model.Role} (USER/ASSISTANT),
 *     & shows pending 'typing' animations.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Separation of concerns:</b> no business logic; view only.
 *     Data and orchestration are handled by
 *     {@link com.example.kramelix.feature.chat.controller.ChatController} and
 *     {@link com.example.kramelix.feature.chat.model.ConversationRepository}.</li>
 *     <li><b>Layout contract:</b> chat row layouts must include a
 *     {@code @+id/textMessage} {@link android.widget.TextView}.</li>
 *     <li><b>Animation hygiene:</b> ViewHolders stop animations on recycle/detach
 *     to avoid visual leaks and stale states.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * // Inside a Fragment/Activity, via ChatController:
 * chatController.bind(recyclerView, getViewLifecycleOwner());
 * // LiveData updates flow into the adapter automatically.
 * }</pre>
 *
 * @author Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.chat.view;
