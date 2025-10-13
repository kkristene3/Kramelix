package com.example.kramelix.feature.chat.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kramelix.feature.chat.model.ConversationRepository;
import com.example.kramelix.feature.chat.model.Message;
import com.example.kramelix.feature.chat.model.Role;
import com.example.kramelix.feature.chat.view.ChatAdapter;

import java.util.Objects;

/**
 * This controller coordinates the chat UI with the in-memory conversation state.
 *
 * <p>It binds a {@link RecyclerView} to the observable message stream, applies a bottom-stacked
 * layout, auto-scrolls on updates, and exposes small helpers to add/update message bubbles.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Wires {@link RecyclerView} to {@link ConversationRepository} via LiveData.</li>
 *     <li>Keeps the list scrolled to the latest message on updates.</li>
 *     <li>Provides thin helpers to add pending bubbles & updates existing ones.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>One controller instance is typically bound to one chat view.</li>
 *     <li>Call {@link #bind(RecyclerView, LifecycleOwner)} from the UI thread.</li>
 *     <li>All mutation methods are thread-safe in the repository; callers may invoke from workers.</li>
 * </ul>
 *
 * @author Amy Huang
 * @noinspection PublicConstructor
 * @since 1.0
 */
public final class ChatController {

    // -------------------- STATE --------------------

    /**
     * In-memory conversation store for the current app session.
     */
    @NonNull
    private final ConversationRepository repo = ConversationRepository.getInstance();

    /**
     * RecyclerView adapter for chat bubbles.
     */
    @NonNull
    private final ChatAdapter adapter = new ChatAdapter();

    // -------------------- BINDING --------------------

    /**
     * This function attaches a {@link RecyclerView} to the conversation stream;
     * It configures a bottom-stacked layout & auto-scrolls to the last item on any update.
     *
     * @param rv    The chat list to bind. Must be a valid, attached RecyclerView.
     * @param owner A {@link LifecycleOwner} (Activity/Fragment) used to observe LiveData.
     */
    public void bind(@NonNull RecyclerView rv, @NonNull LifecycleOwner owner) {

        // PROCESS: creating the layout manager
        LinearLayoutManager lm = new LinearLayoutManager(rv.getContext());
        lm.setStackFromEnd(true); // newest msg at the bottom
        rv.setLayoutManager(lm);

        // PROCESS: connecting adapter
        rv.setAdapter(adapter);

        // PROCESS: observing conversation stream & keeping view pinned to newest item
        repo.getMessages().observe(owner, msgs -> {
            adapter.submit(msgs);
            rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
        });

    }

    // -------------------- HELPERS --------------------

    /**
     * This helper function adds a new pending message (e.g. a “…” bubble) to the conversation.
     *
     * @param role The author role (USER or ASSISTANT).
     * @param seed Initial visible text (often "…").
     * @return The created {@link Message} with a stable ID.
     */
    @NonNull
    public Message addPending(@NonNull Role role, @Nullable String seed) {
        // OUTPUT:
        return repo.addPendingMessage(role, seed);
    }

    /**
     * This helper function updates an existing message by ID.
     *
     * @param id      Target message ID.
     * @param newText New text (or {@code null} to leave unchanged).
     * @param pending New pending state (or {@code null} to leave unchanged).
     */
    public void update(@NonNull String id, @Nullable String newText, @Nullable Boolean pending) {
        repo.updateMessage(Objects.requireNonNull(id, "id"), newText, pending);
    }

    /**
     * Convenience overload that accepts a {@link Message} directly (helps avoid Demeter warnings).
     *
     * @param msg     Target message.
     * @param newText New text (or {@code null} to leave unchanged).
     * @param pending New pending state (or {@code null} to leave unchanged).
     */
    public void update(@NonNull Message msg, @Nullable String newText, @Nullable Boolean pending) {
        repo.updateMessage(Objects.requireNonNull(msg, "msg").getId(), newText, pending);
    }

}
