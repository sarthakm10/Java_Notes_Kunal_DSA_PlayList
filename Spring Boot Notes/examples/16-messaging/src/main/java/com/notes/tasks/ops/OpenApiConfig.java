package com.notes.tasks.ops;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** springdoc builds the OpenAPI document from the controllers; this bean adds the title and the auth scheme. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI taskTrackerApi() {
        return new OpenAPI()
                .info(new Info().title("Task Tracker API").version("0.16.0")
                        .description("Projects, tasks and reports. Get a token from POST /api/auth/token (HTTP Basic)."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));   // every operation needs it
    }
}
