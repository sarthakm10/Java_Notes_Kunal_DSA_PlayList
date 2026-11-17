package com.notes.tasks.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * app.jwt.* bound and VALIDATED at startup: a missing or weak secret stops the app before it takes traffic
 * (fail fast), instead of failing on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 32, message = "must be at least 32 characters (HS256 needs a 256-bit key)")
        String secret,
        Duration lifetime) {

    public JwtProperties {
        if (lifetime == null) lifetime = Duration.ofMinutes(15);   // default when not configured
    }
}
