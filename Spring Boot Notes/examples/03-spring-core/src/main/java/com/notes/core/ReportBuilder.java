package com.notes.core;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope("prototype")            // a NEW instance every time one is requested
public class ReportBuilder {
    private static final AtomicInteger CREATED = new AtomicInteger();
    private final int number = CREATED.incrementAndGet();
    private final StringBuilder lines = new StringBuilder();

    public ReportBuilder line(String s) { lines.append("    ").append(s).append('\n'); return this; }
    public String build() { return "  Report #" + number + "\n" + lines; }
}
