package com.notes.tasks.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumer group "notifications", 2 listener threads sharing the 4 partitions (2 each).
 * Idempotent: the processed_message row and the notification row are written in ONE database
 * transaction, so a duplicate is detected, and a failure rolls back both (the retry is not "a duplicate").
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class TaskEventsConsumer {
    private static final Logger log = LoggerFactory.getLogger(TaskEventsConsumer.class);
    private static final String CONSUMER = "notifications";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final JsonMapper json;
    private final AtomicLong duplicates = new AtomicLong();

    public TaskEventsConsumer(JdbcTemplate jdbc, TransactionTemplate tx, JsonMapper json) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.json = json;
    }

    @KafkaListener(id = "notifications", topics = TaskEventMessage.TOPIC, groupId = CONSUMER, concurrency = "2")
    public void onMessage(ConsumerRecord<String, String> rec) {
        // Malformed JSON throws tools.jackson.core.JacksonException: not retryable (MessagingConfig) -> DLT at once
        TaskEventMessage m = json.readValue(rec.value(), TaskEventMessage.class);
        tx.executeWithoutResult(status -> {
            int inserted = jdbc.update("""
                    INSERT INTO processed_message (event_id, consumer, processed_at) VALUES (?, ?, now())
                    ON CONFLICT DO NOTHING""", m.eventId(), CONSUMER);
            if (inserted == 0) {
                duplicates.incrementAndGet();
                log.info("consumer: DUPLICATE {} (task {}) skipped [partition {}, offset {}]",
                        m.eventId(), m.taskId(), rec.partition(), rec.offset());
                return;
            }
            if (m.title().contains("[boom]")) {                  // a downstream service failing, simulated
                throw new IllegalStateException("simulated: the mail service is down");
            }
            jdbc.update("INSERT INTO notification (event_id, task_id, text, created_at) VALUES (?, ?, ?, now())",
                    m.eventId(), m.taskId(), "New " + m.priority() + " task '" + m.title() + "' by " + m.createdBy());
            log.info("consumer: notified watchers of task {} '{}' [key {}, partition {}, offset {}]",
                    m.taskId(), m.title(), rec.key(), rec.partition(), rec.offset());
        });
    }

    public long duplicatesSkipped() {
        return duplicates.get();
    }
}
