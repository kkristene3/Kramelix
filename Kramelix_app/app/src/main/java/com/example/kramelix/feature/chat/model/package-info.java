/**
 * Defines the chat domain models used to represent and stream conversation state.
 *
 * <p>This package contains the immutable identifiers and mutable, repository-controlled
 * data structures that describe a chat between a user and an assistant (LLM):</p>
 *
 * <ul>
 *     <li>{@link com.example.kramelix.feature.chat.model.Role} — enumerates speaker roles
 *     (USER, ASSISTANT).</li>
 *     <li>{@link com.example.kramelix.feature.chat.model.Message} — a single chat message with
 *     stable ID, role, text, and a {@code pending} lifecycle flag.</li>
 *     <li>{@link com.example.kramelix.feature.chat.model.ConversationRepository} — an in-memory,
 *     per-session timeline that exposes {@link androidx.lifecycle.LiveData} snapshots for the UI.</li>
 * </ul>
 *
 * <br>
 * <strong>Design</strong>
 * <ul>
 *     <li><b>Reactive UI:</b> the repository publishes immutable list snapshots via LiveData so
 *     RecyclerView adapters can re-render on change.</li>
 *     <li><b>Thread safety:</b> mutations are synchronized to guard against background updates
 *     from controllers (e.g. transcription or LLM threads).</li>
 *     <li><b>Snapshot immutability:</b> observers receive {@code Collections.unmodifiableList(...)}
 *     to prevent accidental UI-side mutations.</li>
 *     <li><b>Per-session state:</b> data lives in memory (no persistence); the
 *     {@code ConversationRepository} uses a lightweight singleton to share state.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>All changes flow through {@link com.example.kramelix.feature.chat.model.ConversationRepository}
 *     mutation methods; do not mutate {@link com.example.kramelix.feature.chat.model.Message}
 *     directly outside the package.</li>
 *     <li>Every {@link com.example.kramelix.feature.chat.model.Message} has a unique, stable ID for
 *     diffing and in-place updates.</li>
 *     <li>{@code pending == true} denotes in-flight messages (e.g. user transcription or assistant generation)
 *     and should be rendered with a loading affordance by the UI.</li>
 * </ul>
 *
 * <br>
 * <strong>Typical Usage</strong>
 * <pre>{@code
 * ConversationRepository repo = ConversationRepository.getInstance();
 * repo.getMessages().observe(lifecycleOwner, adapter::submit);
 *
 * // Add a pending user bubble (e.g. "…")
 * Message user = repo.addPendingMessage(Role.USER, "…");
 *
 * // Later, finalize the text:
 * repo.updateMessage(user.getId(), "final user text", false);
 * }</pre>
 *
 * @author Amy Huang
 * @since 1.0
 */
package com.example.kramelix.feature.chat.model;
