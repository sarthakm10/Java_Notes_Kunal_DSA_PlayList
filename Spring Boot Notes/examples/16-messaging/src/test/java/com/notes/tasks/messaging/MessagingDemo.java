package com.notes.tasks.messaging;

import com.notes.tasks.TaskTrackerApplication;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * Runs the real app against a throwaway in-process Kafka broker (no Docker, no Kafka install).
 * Lives in src/test because spring-kafka-test is a test dependency; production points
 * spring.kafka.bootstrap-servers at a real cluster instead.
 */
public class MessagingDemo {

    public static void main(String[] args) throws Exception {
        var broker = new EmbeddedKafkaKraftBroker(1, 4);          // 1 broker; 4 partitions for auto-created topics
        // Logback's default before Spring Boot configures it is DEBUG: the broker would print ~1,300 config lines
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
                .setLevel(ch.qos.logback.classic.Level.WARN);
        broker.afterPropertiesSet();                              // starts it on a free port
        Runtime.getRuntime().addShutdownHook(new Thread(broker::destroy));
        System.out.println("DEMO: embedded Kafka broker at " + broker.getBrokersAsString());

        String[] appArgs = new String[args.length + 2];
        appArgs[0] = "--spring.kafka.bootstrap-servers=" + broker.getBrokersAsString();
        appArgs[1] = "--app.messaging.enabled=true";
        System.arraycopy(args, 0, appArgs, 2, args.length);
        SpringApplication.run(TaskTrackerApplication.class, appArgs);

        // Another (buggy) service writes something that is not JSON to our topic: a "poison pill".
        try (var producer = new KafkaProducer<>(KafkaTestUtils.producerProps(broker),
                new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>(TaskEventMessage.TOPIC, "project-1", "{not json")).get();
        }
        System.out.println("DEMO: a buggy producer sent '{not json' to " + TaskEventMessage.TOPIC);
    }
}
