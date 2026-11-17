package com.notes.core;

import org.springframework.stereotype.Component;

@Component("sms")              // explicit bean name, used with @Qualifier("sms")
public class SmsSender implements MessageSender {
    public String channel() { return "sms"; }
    public String send(String to, String text) { return "[sms to " + to + "] " + text; }
}
