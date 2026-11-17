package com.notes.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository                    // a @Component for data-access classes
public class TaskRepository {
    public record Task(long id, String title, String owner) {}

    private final Map<Long, Task> tasks = new ConcurrentSkipListMap<>();
    private final AtomicLong ids = new AtomicLong(1);

    @PostConstruct             // runs once, after injection, before anyone uses the bean
    void seed() {
        save("Read Spring docs", "asha");
        System.out.println("  [lifecycle] TaskRepository @PostConstruct: seeded " + tasks.size() + " task");
    }

    @PreDestroy                // runs when the container closes
    void shutdown() {
        System.out.println("  [lifecycle] TaskRepository @PreDestroy: " + tasks.size() + " tasks in memory");
    }

    public Task save(String title, String owner) {
        Task t = new Task(ids.getAndIncrement(), title, owner);
        tasks.put(t.id(), t);
        return t;
    }

    public List<Task> findAll() { return List.copyOf(tasks.values()); }
}
