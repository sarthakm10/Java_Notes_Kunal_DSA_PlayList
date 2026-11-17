package com.notes.tasks.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A separate bean, so calls from OuterSteps go through the transactional proxy. */
@Component
@Profile("demo")
public class InnerSteps {

    @Transactional                                    // REQUIRED (default): joins the caller's transaction
    public void failJoined() {
        throw new IllegalStateException("inner step failed");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)    // its own transaction
    public void failInNewTransaction() {
        throw new IllegalStateException("inner step failed");
    }

    @Transactional(propagation = Propagation.MANDATORY)       // must be called inside a transaction
    public void needsTransaction() {
    }
}
