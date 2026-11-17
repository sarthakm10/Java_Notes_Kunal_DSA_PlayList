package com.notes.tasks.repo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same kind of test against a throwaway PostgreSQL in Docker: no local database needed,
 * identical on every machine and in CI. Skipped (not failed) when Docker isn't available.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class TaskRepositoryContainerTest {

    @Container
    @ServiceConnection                     // Boot points spring.datasource.* at the container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired TaskRepository tasks;

    @Test
    void migrationsRunInTheContainer() {
        assertThat(tasks.count()).isEqualTo(12);
    }
}
