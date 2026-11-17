package com.notes.tasks.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Moves outbox rows to Kafka. Runs only with app.messaging.enabled=true (the app works without a broker).
 * At-least-once: a row is marked published only AFTER Kafka acknowledged it. A crash between the two
 * sends the row again on the next run -> consumers must be idempotent (TaskEventsConsumer).
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka) {
        this.jdbc = jdbc;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishPending() throws Exception {
        // FOR UPDATE SKIP LOCKED: several app instances can run the relay without sending a row twice at once
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, event_id, topic, msg_key, payload FROM outbox_event
                WHERE published_at IS NULL ORDER BY id LIMIT 50 FOR UPDATE SKIP LOCKED""");
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            var record = new ProducerRecord<>((String) row.get("topic"), (String) row.get("msg_key"), (String) row.get("payload"));
            record.headers().add("event-id", row.get("event_id").toString().getBytes(StandardCharsets.UTF_8));
            try {
                RecordMetadata meta = kafka.send(record).get(10, TimeUnit.SECONDS).getRecordMetadata();  // wait for the ack
                jdbc.update("UPDATE outbox_event SET published_at = now(), attempts = attempts + 1 WHERE id = ?", id);
                log.info("relay: outbox row {} -> {}-{} offset {} (key {})", id, meta.topic(), meta.partition(),
                        meta.offset(), row.get("msg_key"));
            } catch (Exception ex) {
                jdbc.update("UPDATE outbox_event SET attempts = attempts + 1 WHERE id = ?", id);
                log.warn("relay: outbox row {} not sent ({}); will retry", id, ex.toString());
                break;                                           // keep the order: don't send later rows first
            }
        }
    }
}
