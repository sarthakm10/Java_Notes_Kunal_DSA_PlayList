package com.notes.tasks.ops;

import com.notes.tasks.repo.TaskRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/** A gauge is a value that goes up AND down; Micrometer reads it each time metrics are requested. */
@Component
public class OpenTasksGauge implements MeterBinder {
    private final TaskRepository tasks;

    public OpenTasksGauge(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    public void bindTo(MeterRegistry registry) {                 // Boot calls this for every MeterBinder bean
        Gauge.builder("tasks.open", tasks, TaskRepository::countByDoneFalse)
                .description("Tasks not done yet")
                .register(registry);
    }
}
