package com.notes.tasks.aop;

import com.notes.tasks.service.ActivityService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Cross-cutting concern #1: an audit trail for every method annotated with @Audited. */
@Aspect
@Component
public class AuditAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final ActivityService activity;

    public AuditAspect(ActivityService activity) {
        this.activity = activity;
    }

    /** Runs only when the method RETURNED normally. "audited" binds the annotation instance. */
    @AfterReturning("@annotation(audited)")
    public void success(JoinPoint jp, Audited audited) {
        activity.record(audited.value() + " by " + caller() + " via " + jp.getSignature().toShortString());
    }

    /** Runs only when the method THREW. The exception still propagates to the caller. */
    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "ex")
    public void failure(Audited audited, Exception ex) {
        log.warn("audit: '{}' by {} failed: {}", audited.value(), caller(), ex.getMessage());
    }

    private static String caller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }
}
