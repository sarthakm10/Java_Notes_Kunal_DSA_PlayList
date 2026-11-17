package com.notes.core;

/** Anything that can deliver a message. Services depend on this interface, not on a class. */
public interface MessageSender {
    String channel();
    String send(String to, String text);
}
