package com.example.kramelix.model;

import java.util.UUID;

public class Message {

    public enum Role { SYSTEM, USER, ASSISTANT }

    private final String id;
    private final Role role;
    private String text;
    private final long timestampMs;
    private boolean pending;

    public Message(Role role, String text, long timestampMs) {
        this(UUID.randomUUID().toString(), role, text, timestampMs, false);
    }

    public Message(Role role, String text, long timestampMs, boolean pending) {
        this(UUID.randomUUID().toString(), role, text, timestampMs, pending);
    }

    public Message(String id, Role role, String text, long timestampMs, boolean pending) {
        this.id = (id == null ? UUID.randomUUID().toString() : id);
        this.role = role;
        this.text = text;
        this.timestampMs = timestampMs;
        this.pending = pending;
    }

    public String getId() { return id; }
    public Role getRole() { return role; }
    public String getText() { return text; }
    public long getTimestampMs() { return timestampMs; }
    public boolean isPending() { return pending; }

    void setText(String text) { this.text = text; }
    void setPending(boolean pending) { this.pending = pending; }
}
