package com.notes.tasks.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The publisher (TaskService) doesn't know these listeners exist: loose coupling. */
@Component
public class TaskEventListeners {
    private static final Logger log = LoggerFactory.getLogger(TaskEventListeners.class);

    /** Plain listener: runs IMMEDIATELY, on the publisher's thread, inside its transaction. */
    @EventListener
    public void onCreated(TaskCreatedEvent e) {
        log.info("[sync] task {} created; in a transaction: {}", e.taskId(),
                TransactionSynchronizationManager.isActualTransactionActive());
    }

    /** Runs only after the transaction COMMITTED (the task really exists), here on another thread. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyAfterCommit(TaskCreatedEvent e) {
        log.info("[after commit] notify watchers about '{}' (on {})", e.title(), Thread.currentThread());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(TaskCreatedEvent e) {
        log.info("[after rollback] task {} was never saved: no notification", e.taskId());
    }
}
