package com.notes.tasks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "tasks")
public record TaskTrackerProperties(
        @DefaultValue("100") int maxOpenTasks,
        String fixedClock) {                 // e.g. 2026-11-16T09:00:00Z; empty = real time
}
