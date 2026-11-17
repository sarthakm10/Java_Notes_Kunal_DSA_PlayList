package com.notes.tasks.demo;

import com.notes.tasks.repo.TaskRepository;
import com.notes.tasks.service.ActivityService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Transaction behaviours for the demo. Every method renames task 3, then something happens. */
@Component
@Profile("demo")
public class OuterSteps {
    private final TaskRepository tasks;
    private final InnerSteps inner;
    private final ActivityService activity;

    public OuterSteps(TaskRepository tasks, InnerSteps inner, ActivityService activity) {
        this.tasks = tasks;
        this.inner = inner;
        this.activity = activity;
    }

    private void rename(String title) {
        tasks.findById(3L).orElseThrow().setTitle(title);
    }

    @Transactional
    public void catchJoinedFailure() {
        rename("renamed by catchJoinedFailure");
        try {
            inner.failJoined();                       // the exception marks the SHARED transaction rollback-only
        } catch (IllegalStateException e) {
            System.out.println("    caught: " + e.getMessage() + " (outer carries on and tries to commit)");
        }
    }

    @Transactional
    public void catchNewTransactionFailure() {
        rename("renamed by catchNewTransactionFailure");
        try {
            inner.failInNewTransaction();             // only the inner transaction rolls back
        } catch (IllegalStateException e) {
            System.out.println("    caught: " + e.getMessage());
        }
    }

    @Transactional
    public void logThenFail() {
        rename("renamed by logThenFail");
        activity.record("logThenFail was here");      // REQUIRES_NEW: committed immediately
        throw new IllegalStateException("outer failed after logging");
    }

    @Transactional
    public void failWithCheckedException() throws Exception {
        rename("renamed by failWithCheckedException");
        throw new Exception("checked exception");     // default rule: checked exceptions do NOT roll back
    }

    @Transactional(rollbackFor = Exception.class)
    public void failWithCheckedExceptionRollbackFor() throws Exception {
        rename("renamed by failWithCheckedExceptionRollbackFor");
        throw new Exception("checked exception");
    }

    @Transactional(readOnly = true)
    public void renameInReadOnly() {
        rename("renamed in a read-only transaction");
    }

    @Transactional
    public boolean publicTransactional() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @Transactional
    boolean packagePrivateTransactional() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /** Not transactional itself: calls a @Transactional method on "this", bypassing the proxy. */
    public boolean selfInvocation() {
        return publicTransactional();
    }

    public void callMandatoryWithoutTransaction() {
        inner.needsTransaction();
    }
}
