package com.example.kramelix.model;

import java.util.UUID;

/**
 * This class represents the model for a message in a {@link ConversationRepository} between the user and the LLM.
 */
public class Message {

    // -------------------- COMPONENTS ---------------------
    /**
     * The unique ID associated with the message (needed for tracking conversation history).
     */
    private final String id;

    /**
     * The role of 'chatter' saying the message.
     */
    private final Role role;

    /**
     * The text content of the message.
     */
    private String text;

    /**
     * A flag for whether the message is still being transcribed (user-end) or generated (LLM-end).
     */
    private boolean pending;

    // -------------------- CONSTRUCTORS ---------------------

    /**
     * Constructor for a new message.
     *
     * @param role The role of the chatter.
     * @param text The text content of the message.
     * @param pending Whether the message is still being transcribed or generated.
     */
    Message(Role role, String text, boolean pending) {
        // INITIALIZATION: setting the constructor content w/ a randomly-generated ID
        this(UUID.randomUUID().toString(), role, text, pending);
    }

    /**
     * Constructor for a new message.
     *
     * @param id The unique ID associated with the message.
     * @param role The role of the chatter.
     * @param text The text content of the message.
     * @param pending Whether the message is still being transcribed or generated.
     */
    private Message(String id, Role role, String text, boolean pending) {

        // INITIALIZATION: setting the constructor content
        this.id = (null == id ? UUID.randomUUID().toString() : id);
        this.role = role;
        this.text = text;
        this.pending = pending;

    }

    // -------------------- GETTERS ---------------------

    /**
     * A getter method for the message ID.
     * @return The unique ID associated with the message.
     */
    public final String getId() {
        return id;
    }

    /**
     * A getter method for the message role.
     * @return The role of the chatter.
     */
    public final Role getRole() {
        return role;
    }

    /**
     * A getter method for the message text.
     * @return The text content of the message.
     */
    public final String getText() {
        return text;
    }

    /**
     * A getter method for whether the message is still loading.
     * @return Whether the message is still being transcribed or generated.
     */
    public final boolean isPending() {
        return pending;
    }

    // -------------------- SETTERS ---------------------

    /**
     * A setter method for the message text.
     * @param text The new text content of the message.
     */
    final void setText(String text) {
        this.text = text;
    }

    /**
     * A setter method for whether the message is still loading.
     * @param pending Whether the message is still being transcribed or generated.
     */
    final void setPending(boolean pending) {
        this.pending = pending;
    }

}
