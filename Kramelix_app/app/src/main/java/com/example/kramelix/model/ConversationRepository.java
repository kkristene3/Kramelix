package com.example.kramelix.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationRepository {
    private static ConversationRepository INSTANCE;
    public static synchronized ConversationRepository get() {
        if (INSTANCE == null) INSTANCE = new ConversationRepository();
        return INSTANCE;
    }

    private final MutableLiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Message>> getMessages() { return messages; }

    /** Add and return the message so caller can keep the id. */
    public synchronized Message addMessage(Message msg) {
        List<Message> current = new ArrayList<>(messages.getValue());
        current.add(msg);
        messages.postValue(Collections.unmodifiableList(current));
        return msg;
    }

    /** Update text/pending of a message by id. No-op if not found. */
    public synchronized void updateMessage(String id, String newText, Boolean pending) {
        List<Message> current = new ArrayList<>(messages.getValue());
        boolean changed = false;
        for (Message m : current) {
            if (m.getId().equals(id)) {
                if (newText != null) m.setText(newText);
                if (pending != null) m.setPending(pending);
                changed = true;
                break;
            }
        }
        if (changed) messages.postValue(Collections.unmodifiableList(current));
    }

    public synchronized Message addPendingMessage(Message.Role role, String text) {
        Message msg = new Message(role, text, System.currentTimeMillis(), true);
        List<Message> current = new ArrayList<>(messages.getValue());
        current.add(msg);
        messages.postValue(Collections.unmodifiableList(current));
        return msg;
    }
}
