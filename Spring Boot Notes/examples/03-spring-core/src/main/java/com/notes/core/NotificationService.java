package com.notes.core;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final MessageSender defaultSender;     // the @Primary one (email)
    private final MessageSender smsSender;         // picked by name
    private final List<MessageSender> allSenders;  // every MessageSender bean

    // One constructor => Spring uses it automatically (no @Autowired needed)
    public NotificationService(MessageSender defaultSender,
                               @Qualifier("sms") MessageSender smsSender,
                               List<MessageSender> allSenders) {
        this.defaultSender = defaultSender;
        this.smsSender = smsSender;
        this.allSenders = allSenders;
    }

    public String notify(String to, String text)       { return defaultSender.send(to, text); }
    public String notifyUrgent(String to, String text) { return smsSender.send(to, text); }
    public List<String> channels() { return allSenders.stream().map(MessageSender::channel).toList(); }
}
