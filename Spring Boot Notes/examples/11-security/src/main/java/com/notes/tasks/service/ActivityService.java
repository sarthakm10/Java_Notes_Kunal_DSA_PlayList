package com.notes.tasks.service;

import com.notes.tasks.model.ActivityLog;
import com.notes.tasks.repo.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ActivityService {
    private final ActivityLogRepository logs;
    private final Clock clock;

    public ActivityService(ActivityLogRepository logs, Clock clock) {
        this.logs = logs;
        this.clock = clock;
    }

    /**
     * REQUIRES_NEW: the caller's transaction is suspended and this runs in a NEW one that commits
     * on return. The log row is kept even if the caller rolls back afterwards.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message) {
        logs.save(new ActivityLog(message, Instant.now(clock)));
    }
}
