package com.notes.mongo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * BSON has no date-only type. By default Spring Data turns a LocalDate into midnight in the JVM's
 * time zone (on an IST machine 2026-11-20 is stored as 2026-11-19T18:30Z), so two servers in
 * different zones store the same date differently. The driver's own java.time codecs use UTC.
 */
@Configuration
public class MongoConfig {
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(adapter -> adapter.useNativeDriverJavaTimeCodecs());
    }
}
