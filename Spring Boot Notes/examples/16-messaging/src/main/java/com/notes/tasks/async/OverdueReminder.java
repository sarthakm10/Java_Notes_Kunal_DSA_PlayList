package com.notes.tasks.async;

import com.notes.tasks.repo.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class OverdueReminder {
    private static final Logger log = LoggerFactory.getLogger(OverdueReminder.class);
    private final TaskRepository tasks;
    private final Clock clock;

    public OverdueReminder(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    /** Every app.reminder.rate (an ISO-8601 duration), counted from the START of the previous run. */
    @Scheduled(fixedRateString = "${app.reminder.rate:PT1H}", initialDelayString = "${app.reminder.rate:PT1H}")
    public void remindOverdue() {
        long overdue = tasks.countByDoneFalseAndDueDateBefore(LocalDate.now(clock));
        log.info("reminder: {} overdue tasks (on {})", overdue, Thread.currentThread());
    }

    /** Cron: second minute hour day-of-month month day-of-week. 09:00 on weekdays, India time. */
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void morningDigest() {
        log.info("morning digest");
    }
}
