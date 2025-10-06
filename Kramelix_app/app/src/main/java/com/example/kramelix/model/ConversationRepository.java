package com.example.kramelix.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the model for a repository that holds the in-memory, per-session conversation history.
 * It exposes a {@link LiveData} stream so the UI can observe changes and update automatically.
 */
public class ConversationRepository {

    // -------------------- SINGLETON ---------------------
    /**
     * Singleton instance of the repository per process.
     * Using a singleton here keeps all of the state data for as long as the app runs.
     */
    private static ConversationRepository INSTANCE;

    /**
     * A getter method for the singleton instance.
     * @return The shared {@link ConversationRepository}.
     */
    public static synchronized ConversationRepository get() {

        // PROCESS: checking if an instance already exists
        if (INSTANCE == null) INSTANCE = new ConversationRepository();

        // OUTPUT:
        return INSTANCE;

    }

    // -------------------- STATE ---------------------
    /**
     * A live data stream of the messages as an observable state in display order.
     * Initializes with an empty list to avoid null checks later.
     */
    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());

    /**
     * A getter method for the message stream.
     * @return A live data stream of the messages in display order.
     */
    public LiveData<List<Message>> getMessages() { return messages; }

    // -------------------- MUTATION API ---------------------
    /**
     * This function adds a new pending message to the end of the conversation and returns it (so caller can keep the id).
     * It's synchronized to avoid the Thread concurrently modifying the list snapshot.
     *
     * @param role The role of the chatter (USER or ASSISTANT).
     * @param text The initial text to display in the text bubble.
     * @return The newly created pending message.
     */
    public synchronized Message addPendingMessage(Message.Role role, String text) {

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
     * @param id The ID of the msg to update.
     * @param newText The new text to set.
     * @param pending The new pending flag to set.
     */
    public synchronized void updateMessage(String id, String newText, Boolean pending) {

        // PROCESS: retrieving the current conversation
        List<Message> current = ensureList(messages.getValue());
        boolean changed = false; // flag for updating the msg

        // PROCESS: updating the msg
        for (Message m : current) {

            // TODO: consider adding some error-handling here if the ID is invalid
            if (m.getId().equals(id)) { // msg found

                if (newText != null) m.setText(newText); // updating text
                if (pending != null) m.setPending(pending); // updating flag
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
    private static List<Message> ensureList(List<Message> src) {
        return (src == null) ? new ArrayList<>() : new ArrayList<>(src);
    }

}
