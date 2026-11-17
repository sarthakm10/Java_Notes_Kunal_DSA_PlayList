package com.notes.ms.tasks;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** Every call to user-service goes through the "user-service" circuit breaker, with a fallback. */
@Component
public class UserLookup {
    private static final Logger log = LoggerFactory.getLogger(UserLookup.class);
    private final UserClient users;
    private final CircuitBreaker breaker;

    public UserLookup(UserClient users, CircuitBreakerFactory<?, ?> factory) {
        this.users = users;
        this.breaker = factory.create("user-service");
    }

    public UserClient.User find(long id) {
        return breaker.run(() -> users.get(id), failure -> fallback(id, failure));
    }

    private UserClient.User fallback(long id, Throwable failure) {
        if (failure instanceof HttpClientErrorException e) throw e;     // 404 etc.: pass the answer on
        String reason = failure instanceof CallNotPermittedException
                ? "circuit open: user-service not called"
                : failure.getClass().getSimpleName() + ": " + firstLine(failure.getMessage());
        log.warn("fallback for user {}: {}", id, reason);
        return new UserClient.User(id, "(unavailable)", "fallback", null);
    }

    private static String firstLine(String s) {
        return s == null ? "" : s.lines().findFirst().orElse("");
    }
}
