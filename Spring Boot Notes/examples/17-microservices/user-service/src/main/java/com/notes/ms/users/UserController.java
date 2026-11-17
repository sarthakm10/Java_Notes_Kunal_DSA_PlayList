package com.notes.ms.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

/** The user-service owns user data. Other services ask it; they never read its database. */
@RestController
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private static final Map<Long, String> USERS = Map.of(1L, "asha", 2L, "ravi", 3L, "meera");

    private final String instance;
    private final String greeting;
    private volatile Duration delay = Duration.ZERO;

    public UserController(@Value("${eureka.instance.instance-id}") String instance,
                          @Value("${app.greeting:(no greeting: config server not used)}") String greeting) {
        this.instance = instance;
        this.greeting = greeting;
    }

    public record UserResponse(long id, String name, String servedBy, String greeting) {}

    @GetMapping("/users/{id}")
    public UserResponse get(@PathVariable long id) throws InterruptedException {
        log.info("GET /users/{}", id);
        if (!delay.isZero()) Thread.sleep(delay);                    // simulated slowness (see /chaos/delay)
        String name = USERS.get(id);
        if (name == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " does not exist");
        return new UserResponse(id, name, instance, greeting);
    }

    /** Demo only: make this instance slow, to trip the caller's timeout and circuit breaker. */
    @PostMapping("/chaos/delay")
    public Map<String, Object> delay(@RequestParam long millis) {
        delay = Duration.ofMillis(millis);
        log.warn("chaos: every response now waits {} ms", millis);
        return Map.of("instance", instance, "delayMillis", millis);
    }
}
