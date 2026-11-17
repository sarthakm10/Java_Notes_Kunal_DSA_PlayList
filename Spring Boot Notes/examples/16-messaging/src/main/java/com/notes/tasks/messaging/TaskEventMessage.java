package com.notes.tasks.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * The message on the "task-events" topic: a public contract between teams, so it is its own type,
 * not the JPA entity and not the in-process TaskCreatedEvent. eventId makes every message unique.
 */
public record TaskEventMessage(UUID eventId, String type, long taskId, long projectId, String title,
                               String priority, String createdBy, Instant occurredAt) {
    public static final String TOPIC = "task-events";
    public static final String DLT = "task-events.DLT";
}
