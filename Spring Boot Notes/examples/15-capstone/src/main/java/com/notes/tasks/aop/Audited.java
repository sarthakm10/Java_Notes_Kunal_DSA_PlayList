package com.notes.tasks.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a method whose successful calls are written to the activity log by AuditAspect. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)          // must survive to runtime: the aspect reads it via reflection
public @interface Audited {
    String value();                          // the action name, e.g. "create task"
}
