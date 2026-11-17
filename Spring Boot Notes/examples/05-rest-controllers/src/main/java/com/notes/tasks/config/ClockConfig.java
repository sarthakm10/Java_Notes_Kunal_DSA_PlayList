package com.notes.tasks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** A fixed clock makes createdAt predictable in demos and tests; production uses real time. */
    @Bean
    Clock clock(TaskTrackerProperties props) {
        String fixed = props.fixedClock();
        return (fixed == null || fixed.isBlank())
                ? Clock.systemUTC()
                : Clock.fixed(Instant.parse(fixed), ZoneOffset.UTC);
    }
}
