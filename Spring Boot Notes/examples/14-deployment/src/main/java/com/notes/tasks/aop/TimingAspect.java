package com.notes.tasks.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cross-cutting concern #2: time every public method of the service layer; warn about slow ones. */
@Aspect
@Component
public class TimingAspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);
    private final long slowMs;

    public TimingAspect(@Value("${app.slow-call-ms:200}") long slowMs) {
        this.slowMs = slowMs;
    }

    /** A named, reusable pointcut: any type in the service package (and sub-packages). */
    @Pointcut("within(com.notes.tasks.service..*)")
    void serviceLayer() {}

    @Around("serviceLayer() && execution(public * *(..))")
    public Object time(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();                                   // call the real method
        } finally {                                                 // also when it throws
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (ms >= slowMs) log.warn("SLOW {} took {} ms", pjp.getSignature().toShortString(), ms);
            else log.debug("{} took {} ms", pjp.getSignature().toShortString(), ms);
        }
    }
}
