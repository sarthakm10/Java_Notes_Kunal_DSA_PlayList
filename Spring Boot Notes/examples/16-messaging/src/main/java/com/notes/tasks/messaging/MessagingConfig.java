package com.notes.tasks.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/** Topics and error handling; only when app.messaging.enabled=true. */
@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
public class MessagingConfig {
    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    /** KafkaAdmin (auto-configured) creates these at startup if they don't exist. */
    @Bean
    NewTopic taskEvents() {
        return TopicBuilder.name(TaskEventMessage.TOPIC).partitions(4).replicas(1).build();   // 4: see the notes (3 put both projects on one)
    }

    @Bean
    NewTopic taskEventsDlt() {                       // same partition count: a dead letter keeps its partition
        return TopicBuilder.name(TaskEventMessage.DLT).partitions(4).replicas(1).build();
    }

    /**
     * Boot gives this CommonErrorHandler to every @KafkaListener container. A failing record is retried
     * 2 more times, 500 ms apart (3 attempts), then published to task-events.DLT (same partition) and SKIPPED, so one
     * bad message doesn't block its partition forever. Malformed JSON can never succeed: no retries.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(TaskEventMessage.DLT, record.partition()));
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(500L, 2L));
        handler.addNotRetryableExceptions(JacksonException.class);
        handler.setRetryListeners(new RetryListener() {             // make every failed attempt visible
            @Override
            public void failedDelivery(ConsumerRecord<?, ?> rec, Exception ex, int attempt) {
                log.warn("retry: {}-{}@{} failed, attempt {}: {}", rec.topic(), rec.partition(), rec.offset(),
                        attempt, rootMessage(ex));
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> rec, Exception ex) {
                log.warn("retry: {}-{}@{} gave up -> {}", rec.topic(), rec.partition(), rec.offset(), TaskEventMessage.DLT);
            }
        });
        return handler;
    }

    private static String rootMessage(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        String msg = String.valueOf(t.getMessage()).lines().findFirst().orElse("");
        return t.getClass().getSimpleName() + ": " + msg;
    }
}
