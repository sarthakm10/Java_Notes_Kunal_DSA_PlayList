package com.notes.tasks.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A custom marker annotation. The AuditInterceptor looks for it on controller methods. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)      // must survive to runtime so reflection can see it
public @interface AuditLog {
    String value();                      // what action is being audited
}
