package com.notes.tasks.web;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @RequestScope: one instance per HTTP request. Singletons (controller, interceptor) get a
 * PROXY injected, which finds the instance belonging to the current request.
 */
@Component
@RequestScope
public class RequestInfo {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int number = COUNTER.incrementAndGet();

    public int number() { return number; }
}
