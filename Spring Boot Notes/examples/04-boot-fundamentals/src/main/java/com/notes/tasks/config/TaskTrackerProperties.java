package com.notes.tasks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/**
 * Every "tasks.*" key from application.yml, env vars, command-line args... bound into one
 * immutable, typed object. Relaxed binding: tasks.max-open-tasks, TASKS_MAXOPENTASKS and
 * --tasks.maxOpenTasks all fill maxOpenTasks.
 */
@ConfigurationProperties(prefix = "tasks")
public record TaskTrackerProperties(
        @DefaultValue("Task Tracker") String displayName,
        @DefaultValue("10") int maxOpenTasks,
        @DefaultValue("1h") Duration reminderEvery,          // "30m", "1h", "PT15M" all work
        List<String> admins,                                 // YAML list or "a,b"
        Notifications notifications) {

    public record Notifications(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("email") String channel) {}
}
