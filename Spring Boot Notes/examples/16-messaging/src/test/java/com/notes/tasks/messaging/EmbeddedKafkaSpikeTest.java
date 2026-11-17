package com.notes.tasks.messaging;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Does an embedded (KRaft) Kafka broker start on this machine? One message there and back. */
@SpringJUnitConfig
@EmbeddedKafka(partitions = 1, topics = "spike")
class EmbeddedKafkaSpikeTest {

    @Autowired EmbeddedKafkaBroker broker;

    @Test
    void sendsAndReceivesOneMessage() throws Exception {
        Map<String, Object> pp = KafkaTestUtils.producerProps(broker);
        try (var producer = new KafkaProducer<>(pp, new StringSerializer(), new StringSerializer())) {
            producer.send(new ProducerRecord<>("spike", "k1", "hello kafka")).get();
        }
        Map<String, Object> cp = KafkaTestUtils.consumerProps(broker, "spike-group", true);
        cp.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new KafkaConsumer<>(cp, new StringDeserializer(), new StringDeserializer())) {
            broker.consumeFromAnEmbeddedTopic(consumer, "spike");
            ConsumerRecord<String, String> r = KafkaTestUtils.getSingleRecord(consumer, "spike");
            System.out.println("SPIKE broker=" + broker.getBrokersAsString() + " got " + r.key() + "=" + r.value()
                    + " partition=" + r.partition() + " offset=" + r.offset());
            assertThat(r.value()).isEqualTo("hello kafka");
        }
    }
}
