package com.notes.tasks.ops;

import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

@Configuration
public class ActuatorConfig {
    private static final Pattern SENSITIVE = Pattern.compile("(?i).*(password|secret|token|credential).*");

    /**
     * /actuator/env and /actuator/configprops show values to admins (show-values: when-authorized).
     * This function still masks anything whose KEY looks sensitive. Switch it off with
     * --app.actuator.mask-secrets=false to see what Boot does on its own.
     */
    @Bean
    @ConditionalOnProperty(name = "app.actuator.mask-secrets", havingValue = "true", matchIfMissing = true)
    SanitizingFunction maskSecrets() {
        return data -> SENSITIVE.matcher(data.getKey()).matches() ? data.withSanitizedValue() : data;
    }
}
