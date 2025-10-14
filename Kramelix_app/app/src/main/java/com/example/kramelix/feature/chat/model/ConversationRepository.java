package com.example.kramelix.feature.chat.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This model maintains the in-memory conversation history between the user and the assistant
 * for the current app session.
 *
 * <p>The repository exposes a {@link androidx.lifecycle.LiveData} stream that UI layers can observe
 * to render updates automatically. It encapsulates all message creation, mutation, and notification
 * logic, ensuring thread-safe access from controllers such as
 * {@link com.example.kramelix.feature.chat.controller.ChatController}.</p>
 *
 * <br>
 * <strong>Responsibilities</strong>
 * <ul>
 *     <li>Owns the per-session list of {@link Message} objects representing the conversation timeline.</li>
 *     <li>Exposes observable {@link LiveData} for reactive UI updates.</li>
 *     <li>Provides thread-safe mutation APIs to add or update messages in order.</li>
 *     <li>Ensures immutability of snapshots by posting unmodifiable list copies to observers.</li>
 *     <li>Implements a lightweight singleton pattern to persist chat state across components.</li>
 * </ul>
 *
 * <br>
 * <strong>Contracts</strong>
 * <ul>
 *     <li>All repository modifications must occur through its synchronized public methods.</li>
 *     <li>{@link #getMessages()} always returns a non-null stream; observers receive an empty list initially.</li>
 *     <li>Message IDs are unique and stable throughout the app session.</li>
 *     <li>Each {@link androidx.lifecycle.MutableLiveData#postValue(Object)} call emits a new immutable snapshot suitable for diffing in the UI.</li>
 * </ul>
 *
 * @author Amy Huang
 * @noinspection PublicConstructor
 * @since 1.0
 */
public class ConversationRepository {

    // -------------------- SINGLETON ---------------------
    /**
     * Singleton instance of the repository per process.
     * Using a singleton here keeps all of the state data for as long as the app runs.
     */
    private static ConversationRepository instance;

    /**
     * A getter method for the singleton instance.
     *
     * @return The shared ConversationRepository.
     */
    public static synchronized ConversationRepository getInstance() {

        // PROCESS: checking if an instance already exists
        if (null == instance) instance = new ConversationRepository();

        // OUTPUT:
        //noinspection StaticVariableUsedBeforeInitialization
        return instance;

    }

    // -------------------- STATE ---------------------
    /**
     * A live data stream of the messages as an observable state in display order.
     * Initializes with an empty list to avoid null checks later.
     */
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>(0));

    /**
     * A getter method for the message stream.
     *
     * @return A live data stream of the messages in display order.
     */
    public final LiveData<List<Message>> getMessages() {
        return messages;
    }

    // -------------------- MUTATION API ---------------------

    /**
     * This function adds a new pending message to the end of the conversation and returns it (so caller can keep the id).
     * It's synchronized to avoid the Thread concurrently modifying the list snapshot.
     *
     * @param role The role of the chatter (USER or ASSISTANT).
     * @param text The initial text to display in the text bubble.
     * @return The newly created pending message.
     */
    public final synchronized Message addPendingMessage(Role role, String text) {

        // VARIABLE DECLARATION: creating a new msg
        Message msg = new Message(role, text, true);

        // PROCESS: retrieving the current conversation & adding the new msg
        List<Message> current = ensureList(messages.getValue());
        current.add(msg);

        messages.postValue(Collections.unmodifiableList(current)); // notifying observers

        // OUTPUT:
        return msg;

    }

    /**
     * This function updates the text and/or pending flag of an existing message, given the ID.
     *
     * @param id      The ID of the msg to update.
     * @param newText The new text to set.
     * @param pending The new pending flag to set.
     */
    public final synchronized void updateMessage(String id, String newText, Boolean pending) {

        // PROCESS: retrieving the current conversation
        List<Message> current = ensureList(messages.getValue());
        boolean changed = false; // flag for updating the msg

        // PROCESS: updating the msg
        for (Message m : current) {

            // TODO: consider adding some error-handling here if the ID is invalid
            if (m.getId().equals(id)) { // msg found

                if (null != newText) m.setText(newText); // updating text
                if (null != pending) m.setPending(pending); // updating flag
                changed = true; // updating flag
                break;

            }

        }

        if (changed) { // msg was updated
            messages.postValue(Collections.unmodifiableList(current)); // notifying observers
        }

    }

    // -------------------- HELPER METHODS ---------------------

    /**
     * This helper function ensures a non-null, mutable list for local edits.
     * If the LiveData somehow contained null elements, this returns a fresh list copy.
     *
     * @param src The list of messages to copy.
     * @return A non-null, mutable list of messages.
     */
    private static List<Message> ensureList(List<? extends Message> src) {
        return (null == src) ? new ArrayList<>(0) : new ArrayList<>(src);
    }

}
