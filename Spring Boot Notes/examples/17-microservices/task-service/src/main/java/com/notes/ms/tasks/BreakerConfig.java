package com.notes.ms.tasks;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;

@Configuration
public class BreakerConfig {
    private static final Logger log = LoggerFactory.getLogger(BreakerConfig.class);

    /** The "user-service" circuit breaker, from task-service.yml on the config server. */
    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> userServiceBreaker(
            @Value("${app.user-service.breaker.window:4}") int window,
            @Value("${app.user-service.breaker.failure-rate:50}") float failureRate,
            @Value("${app.user-service.breaker.open-for:5s}") Duration openFor) {
        return factory -> {
            factory.configure(builder -> builder.circuitBreakerConfig(CircuitBreakerConfig.custom()
                    .slidingWindowSize(window)                        // the last N calls...
                    .minimumNumberOfCalls(window)                     // ...once there are N of them
                    .failureRateThreshold(failureRate)                // open at >= 50 % failures
                    .waitDurationInOpenState(openFor)                 // fail fast this long, then half-open
                    .permittedNumberOfCallsInHalfOpenState(1)         // one trial call decides: close or re-open
                    .ignoreExceptions(HttpClientErrorException.class) // a 404 is an answer, not an outage
                    .build()), "user-service");
            factory.addCircuitBreakerCustomizer(cb -> cb.getEventPublisher().onStateTransition(e ->
                    log.warn("circuit breaker '{}': {}", e.getCircuitBreakerName(), e.getStateTransition())),
                    "user-service");
        };
    }
}
