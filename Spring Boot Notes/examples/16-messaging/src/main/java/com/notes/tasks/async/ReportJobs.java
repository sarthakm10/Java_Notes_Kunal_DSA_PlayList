package com.notes.tasks.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ReportJobs {
    private static final Logger log = LoggerFactory.getLogger(ReportJobs.class);

    /** Runs on the "report-" pool; the caller gets a CompletableFuture immediately. */
    @Async("reportExecutor")
    public CompletableFuture<String> buildReport(String name, long millis) {
        sleep(millis);                                            // pretend to crunch numbers
        return CompletableFuture.completedFuture(name + " built on " + Thread.currentThread().getName());
    }

    /** No executor named: Spring picks the default one. Fire-and-forget (void). */
    @Async
    public void sendDigest(String recipient) {
        log.info("digest for '{}' on {}", recipient, Thread.currentThread());
        if (recipient.isBlank()) throw new IllegalArgumentException("no recipient");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
