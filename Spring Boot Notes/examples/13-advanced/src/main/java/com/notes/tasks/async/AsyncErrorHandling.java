package com.notes.tasks.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.stereotype.Component;

/**
 * Exceptions from void @Async methods have no caller to reach: they end up here.
 * Kept in its own class ON PURPOSE: with spring.task.execution.mode=force, Boot wraps the AsyncConfigurer
 * bean (to default to its applicationTaskExecutor). A @Configuration with @Bean methods must not be that bean.
 */
@Component
public class AsyncErrorHandling implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncErrorHandling.class);

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("@Async {} failed: {}", method.getName(), ex.getMessage());
    }
}
