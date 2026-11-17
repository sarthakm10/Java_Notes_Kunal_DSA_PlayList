package com.notes.ms.tasks;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * task-service alone: no config server, no Eureka. "user-service" is a stub HTTP server, and Spring Cloud's
 * SimpleDiscoveryClient maps the name to it (spring.cloud.discovery.client.simple.instances...).
 */
@SpringBootTest(properties = {
        "CONFIG_IMPORT=",                              // no config server import (see application.yml)
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"})
@AutoConfigureMockMvc
class TaskServiceResilienceTest {
    static final AtomicInteger calls = new AtomicInteger();
    static final AtomicBoolean failing = new AtomicBoolean();
    static final HttpServer stub = startStub();

    static HttpServer startStub() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            s.createContext("/users/", ex -> {
                calls.incrementAndGet();
                byte[] body = (failing.get() ? "{\"error\":\"down\"}"
                        : "{\"id\":2,\"name\":\"ravi\",\"servedBy\":\"stub\",\"greeting\":null}")
                        .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(failing.get() ? 500 : 200, body.length);
                ex.getResponseBody().write(body);
                ex.close();
            });
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void userServiceIsTheStub(DynamicPropertyRegistry r) {
        r.add("spring.cloud.discovery.client.simple.instances.user-service[0].uri",
                () -> "http://localhost:" + stub.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        stub.stop(0);
    }

    @Autowired MockMvcTester mvc;
    @Autowired Resilience4JCircuitBreakerFactory breakers;

    @BeforeEach
    void healthyStub() {
        failing.set(false);
        calls.set(0);
        // tests share the context, so reset the breaker. find(), not circuitBreaker(): that would CREATE it here
        // with Resilience4j's defaults (window 100) before the factory applies BreakerConfig's settings
        breakers.getCircuitBreakerRegistry().find("user-service").ifPresent(cb -> cb.reset());
    }

    @Test
    void enrichesTheTaskWithTheUserFromUserService() {
        assertThat(mvc.get().uri("/tasks/1")).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 1, "title": "Design the schema", "assignee": "ravi", "assigneeServedBy": "stub"}""");
        assertThat(calls).hasValue(1);
    }

    @Test
    void failuresFallBackAndThenOpenTheBreaker() {
        failing.set(true);
        for (int i = 0; i < 6; i++) {
            assertThat(mvc.get().uri("/tasks/1")).hasStatusOk()             // still 200: the fallback answers
                    .bodyJson().extractingPath("$.assignee").isEqualTo("(unavailable)");
        }
        // window 4, 50 %: the breaker opened after the 4th failed call; calls 5 and 6 never reached the stub
        assertThat(calls).hasValue(4);
    }
}
