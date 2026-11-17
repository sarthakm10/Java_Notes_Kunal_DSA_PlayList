package com.notes.ms.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** A slice test: no config server, no Eureka. Each service is tested on its own first. */
@WebMvcTest(properties = {
        "CONFIG_IMPORT=",                              // no config server import (see application.yml)
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"})
@TestPropertySource(properties = {"eureka.instance.instance-id=user-service:test", "app.greeting=hi"})
class UserControllerTest {
    @Autowired MockMvcTester mvc;

    @Test
    void returnsAUserWithTheInstanceThatServedIt() {
        assertThat(mvc.get().uri("/users/2")).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {"id": 2, "name": "ravi", "servedBy": "user-service:test", "greeting": "hi"}""");
    }

    @Test
    void unknownUserIs404() {
        assertThat(mvc.get().uri("/users/99")).hasStatus(404);
    }
}
