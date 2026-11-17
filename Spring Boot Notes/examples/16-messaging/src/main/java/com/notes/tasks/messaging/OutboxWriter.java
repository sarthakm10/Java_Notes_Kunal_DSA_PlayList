package com.notes.tasks.messaging;

import com.notes.tasks.events.TaskCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Producer side of the transactional outbox. A plain @EventListener runs on the publisher's thread,
 * INSIDE TaskService.create's transaction (file 13): the outbox row commits or rolls back with the task.
 * Nothing talks to Kafka here, so a broker outage can't fail (or slow down) creating a task.
 */
@Component
public class OutboxWriter {
    private static final Logger log = LoggerFactory.getLogger(OutboxWriter.class);
    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final Clock clock;

    public OutboxWriter(JdbcTemplate jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)       // refuse to run outside the task's transaction
    public void onTaskCreated(TaskCreatedEvent e) {
        Instant now = Instant.now(clock);
        TaskEventMessage msg = new TaskEventMessage(UUID.randomUUID(), "TaskCreated", e.taskId(), e.projectId(),
                e.title(), e.priority(), e.createdBy(), now);
        jdbc.update("""
                INSERT INTO outbox_event (event_id, topic, msg_key, payload, created_at)
                VALUES (?, ?, ?, ?, ?)""",
                msg.eventId(), TaskEventMessage.TOPIC, "project-" + e.projectId(),
                json.writeValueAsString(msg), Timestamp.from(now));
        log.info("outbox: stored {} for task {} (same transaction as the task)", msg.eventId(), e.taskId());
    }
}
