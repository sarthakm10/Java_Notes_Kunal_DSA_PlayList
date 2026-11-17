package com.notes.tasks.ops;

import com.notes.tasks.model.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/** Business metrics with Micrometer: they show up in /actuator/metrics and /actuator/prometheus. */
@Component
public class TaskMetrics {
    private final MeterRegistry registry;
    private final Timer searchTimer;

    public TaskMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.searchTimer = Timer.builder("tasks.search")
                .description("Time spent in TaskService.search")
                .register(registry);
    }

    /** A counter only goes up. One time series per tag value: priority=LOW|MEDIUM|HIGH. */
    public void taskCreated(Priority priority) {
        Counter.builder("tasks.created")
                .description("Tasks created through the API")
                .tag("priority", priority.name())                // keep tag values few (no ids, no usernames)
                .register(registry)                              // returns the existing counter after the first call
                .increment();
    }

    public <T> T timeSearch(Supplier<T> search) {
        return searchTimer.record(search);
    }
}
