package com.notes.mongo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.Clock;

@SpringBootApplication
@EnableMongoAuditing                         // fills @CreatedDate / @LastModifiedDate on save
public class TaskDocsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskDocsApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
