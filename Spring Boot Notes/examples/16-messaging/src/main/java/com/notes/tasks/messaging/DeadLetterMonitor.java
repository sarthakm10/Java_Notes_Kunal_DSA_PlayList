package com.notes.tasks.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Reads the dead-letter topic: in real life this alerts someone; here it logs what went wrong and where.
 * DeadLetterPublishingRecoverer adds kafka_dlt-* headers: original topic/partition/offset and the exception.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class DeadLetterMonitor {
    private static final Logger log = LoggerFactory.getLogger(DeadLetterMonitor.class);

    @KafkaListener(id = "dead-letters", topics = TaskEventMessage.DLT, groupId = "dlt-monitor")
    public void onDeadLetter(ConsumerRecord<String, String> rec) {
        log.warn("DLT: value={} | from {}-{}@{} | cause {}", abbreviate(rec.value()),
                header(rec, "kafka_dlt-original-topic"), intHeader(rec, "kafka_dlt-original-partition"),
                longHeader(rec, "kafka_dlt-original-offset"), header(rec, "kafka_dlt-exception-cause-fqcn"));
    }

    private static String header(ConsumerRecord<?, ?> rec, String name) {
        Header h = rec.headers().lastHeader(name);
        return h == null ? "-" : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static Object intHeader(ConsumerRecord<?, ?> rec, String name) {
        Header h = rec.headers().lastHeader(name);
        return h == null ? "-" : java.nio.ByteBuffer.wrap(h.value()).getInt();
    }

    private static Object longHeader(ConsumerRecord<?, ?> rec, String name) {
        Header h = rec.headers().lastHeader(name);
        return h == null ? "-" : java.nio.ByteBuffer.wrap(h.value()).getLong();
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 60 ? s : s.substring(0, 60) + "...";
    }
}
