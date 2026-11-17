package com.notes.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration                                   // a source of bean definitions
@ComponentScan("com.notes.core")                 // find @Component/@Service/@Repository classes
@PropertySource("classpath:app.properties")      // load key=value settings into the Environment
public class AppConfig {

    // A class we don't own (java.time.Clock) can't get @Component, so we use a @Bean method.
    @Bean
    @Profile("dev")                              // only exists when the "dev" profile is active
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    @Profile("prod")
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
