package com.notes.tasks.messaging;

import com.notes.tasks.model.Priority;
import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.service.TaskService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The whole flow against an embedded Kafka broker: task -> outbox -> relay -> topic -> consumer.
 * Messaging is asynchronous, so the assertions WAIT (Awaitility) instead of sleeping a fixed time.
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",   // set by @EmbeddedKafka
        "app.messaging.enabled=true",
        "app.outbox.poll-interval=PT0.2S"})
@EmbeddedKafka(partitions = 4, topics = {TaskEventMessage.TOPIC, TaskEventMessage.DLT})
@ActiveProfiles("test")
class MessagingFlowTest {
    private static final Duration WAIT = Duration.ofSeconds(20);

    @Autowired TaskService tasks;
    @Autowired JdbcTemplate jdbc;
    @Autowired TaskEventsConsumer consumer;
    @Autowired KafkaTemplate<String, String> kafka;
    @Autowired EmbeddedKafkaBroker broker;

    static Consumer<String, String> dlt;                   // a plain consumer that reads the dead letters

    final java.util.List<Long> created = new java.util.ArrayList<>();

    /** These tasks COMMIT (the consumer must see them), so remove them: other tests count tasks. */
    @AfterEach
    void removeWhatThisTestCreated() {
        created.forEach(id -> jdbc.update("DELETE FROM task WHERE id = ?", id));
        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM processed_message");
        jdbc.update("DELETE FROM outbox_event");
    }

    @AfterAll
    static void closeDltReader() {
        if (dlt != null) dlt.close();
    }

    @Test
    @WithMockUser(username = "asha", roles = "ADMIN")
    void createdTaskReachesTheConsumerOnce_evenWhenTheEventIsSentTwice() {
        long id = tasks.create(1, new CreateTaskRequest("Messaging test task", null, Priority.HIGH, null, null, null)).id();
        created.add(id);

        await().atMost(WAIT).until(() -> notificationsFor(id) == 1);
        assertThat(jdbc.queryForObject("SELECT published_at IS NOT NULL FROM outbox_event WHERE payload::json ->> 'taskId' = ?",
                Boolean.class, String.valueOf(id))).isTrue();

        long duplicatesBefore = consumer.duplicatesSkipped();
        jdbc.update("UPDATE outbox_event SET published_at = NULL WHERE payload::json ->> 'taskId' = ?", String.valueOf(id));

        await().atMost(WAIT).until(() -> consumer.duplicatesSkipped() > duplicatesBefore);   // delivered again...
        assertThat(notificationsFor(id)).isEqualTo(1);                                         // ...handled once
    }

    @Test
    @WithMockUser(username = "asha", roles = "ADMIN")
    void failingMessageIsRetriedThenDeadLettered_withoutANotification() {
        long id = tasks.create(2, new CreateTaskRequest("Order cake [boom]", null, Priority.LOW, null, null, null)).id();
        created.add(id);

        ConsumerRecord<String, String> dead = nextDeadLetter(rec -> rec.value().contains("\"taskId\":" + id + ","));
        assertThat(header(dead, "kafka_dlt-exception-cause-fqcn")).isEqualTo(IllegalStateException.class.getName());
        assertThat(notificationsFor(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_message pm JOIN outbox_event o USING (event_id) "
                + "WHERE o.payload::json ->> 'taskId' = ?", Long.class, String.valueOf(id)))
                .as("the failed attempts rolled back their processed_message rows").isZero();
    }

    @Test
    void malformedJsonGoesStraightToTheDeadLetterTopic() {
        kafka.send(TaskEventMessage.TOPIC, "project-9", "{this is not json");

        ConsumerRecord<String, String> dead = nextDeadLetter(rec -> rec.value().startsWith("{this is not json"));
        assertThat(header(dead, "kafka_dlt-exception-cause-fqcn")).endsWith("StreamReadException");
    }

    private long notificationsFor(long taskId) {
        return jdbc.queryForObject("SELECT count(*) FROM notification WHERE task_id = ?", Long.class, taskId);
    }

    private ConsumerRecord<String, String> nextDeadLetter(java.util.function.Predicate<ConsumerRecord<String, String>> match) {
        if (dlt == null) {
            Map<String, Object> props = KafkaTestUtils.consumerProps(broker, "test-dlt-reader", false);
            props.put("auto.offset.reset", "earliest");
            dlt = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer());
            broker.consumeFromAnEmbeddedTopic(dlt, TaskEventMessage.DLT);
        }
        var found = new java.util.concurrent.atomic.AtomicReference<ConsumerRecord<String, String>>();
        await().atMost(WAIT).until(() -> {
            for (ConsumerRecord<String, String> r : KafkaTestUtils.getRecords(dlt, Duration.ofMillis(500), 0)) {
                if (match.test(r)) found.set(r);
            }
            return found.get() != null;
        });
        return found.get();
    }

    private static String header(ConsumerRecord<?, ?> rec, String name) {
        return new String(rec.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }
}
