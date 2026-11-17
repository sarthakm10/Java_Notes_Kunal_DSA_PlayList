package com.notes.core;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary                       // chosen when someone asks for "a MessageSender" without saying which
public class EmailSender implements MessageSender {
    public String channel() { return "email"; }
    public String send(String to, String text) { return "[email to " + to + "] " + text; }
}
