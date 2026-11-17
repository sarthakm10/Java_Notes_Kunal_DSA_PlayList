package com.notes.tasks.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Binding + validation of app.jwt.* without starting the web app or touching the database. */
class JwtPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class Config { }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(Config.class);

    @Test
    void bindsTheSecretAndDefaultsTheLifetime() {
        runner.withPropertyValues("app.jwt.secret=0123456789abcdef0123456789abcdef")
              .run(ctx -> {
                  JwtProperties jwt = ctx.getBean(JwtProperties.class);
                  assertThat(jwt.lifetime()).isEqualTo(Duration.ofMinutes(15));
              });
    }

    @Test
    void aConfiguredLifetimeOverridesTheDefault() {
        runner.withPropertyValues("app.jwt.secret=0123456789abcdef0123456789abcdef", "app.jwt.lifetime=PT5M")
              .run(ctx -> assertThat(ctx.getBean(JwtProperties.class).lifetime()).isEqualTo(Duration.ofMinutes(5)));
    }

    @Test
    void aShortSecretStopsTheContext() {
        runner.withPropertyValues("app.jwt.secret=too-short")
              .run(ctx -> assertThat(ctx).hasFailed()
                      .getFailure().rootCause().hasMessageContaining("at least 32 characters"));
    }
}
