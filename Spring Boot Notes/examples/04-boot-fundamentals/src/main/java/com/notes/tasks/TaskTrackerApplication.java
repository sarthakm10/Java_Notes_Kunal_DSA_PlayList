package com.notes.tasks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication          // = @Configuration + @EnableAutoConfiguration + @ComponentScan (this package and below)
@ConfigurationPropertiesScan    // find @ConfigurationProperties records in this package and below
public class TaskTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskTrackerApplication.class, args);
    }
}
