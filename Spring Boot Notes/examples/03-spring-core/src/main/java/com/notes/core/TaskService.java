package com.notes.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;

@Service
public class TaskService {
    private final TaskRepository repo;
    private final NotificationService notifications;
    private final Clock clock;
    private final String appName;
    private final int maxTasks;

    public TaskService(TaskRepository repo,
                       NotificationService notifications,
                       Clock clock,                                   // dev: fixed, prod: real
                       @Value("${app.name}") String appName,          // from app.properties
                       @Value("${app.max-tasks:3}") int maxTasks) {   // ":3" = default if missing
        this.repo = repo;
        this.notifications = notifications;
        this.clock = clock;
        this.appName = appName;
        this.maxTasks = maxTasks;
        System.out.println("  [lifecycle] TaskService constructed (all 5 dependencies injected)");
    }

    public String addTask(String title, String owner) {
        if (repo.findAll().size() >= maxTasks) {
            return notifications.notifyUrgent(owner,
                    "limit of " + maxTasks + " tasks reached, '" + title + "' rejected");
        }
        var t = repo.save(title, owner);
        String time = LocalTime.now(clock).toString();
        return notifications.notify(owner,
                appName + " @" + time + ": task #" + t.id() + " '" + t.title() + "' created");
    }
}
