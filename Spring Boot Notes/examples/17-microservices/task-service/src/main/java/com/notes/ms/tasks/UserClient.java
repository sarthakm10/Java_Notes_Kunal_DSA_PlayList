package com.notes.ms.tasks;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * A declarative HTTP client (Spring Framework's HTTP interface): Spring generates the implementation.
 * The same idea as OpenFeign's @FeignClient, built into the framework.
 */
@HttpExchange("/users")
public interface UserClient {

    record User(long id, String name, String servedBy, String greeting) {}

    @GetExchange("/{id}")
    User get(@PathVariable long id);
}
