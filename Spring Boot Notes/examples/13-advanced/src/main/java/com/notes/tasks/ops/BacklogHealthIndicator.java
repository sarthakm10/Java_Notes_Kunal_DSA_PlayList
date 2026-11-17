package com.notes.tasks.ops;

import com.notes.tasks.repo.TaskRepository;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * A custom health check. The bean name minus "HealthIndicator" is its name in /actuator/health: "backlog".
 * AbstractHealthIndicator turns an exception (e.g. the database is down) into DOWN for us.
 */
@Component
public class BacklogHealthIndicator extends AbstractHealthIndicator {
    private final TaskRepository tasks;
    private final Clock clock;

    public BacklogHealthIndicator(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long open = tasks.countByDoneFalse();
        long overdue = tasks.countByDoneFalseAndDueDateBefore(LocalDate.now(clock));
        builder.up()
               .withDetail("openTasks", open)
               .withDetail("overdueTasks", overdue);
    }
}
