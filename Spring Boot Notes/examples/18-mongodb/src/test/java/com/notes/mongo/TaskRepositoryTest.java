package com.notes.mongo;

import com.notes.mongo.config.MongoConfig;
import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Task;
import com.notes.mongo.repo.TaskRepository;
import com.notes.mongo.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Against a REAL MongoDB: runs only when MONGODB_URI is set, e.g.
 *   docker run -d -p 27017:27017 mongo:8   then   MONGODB_URI=mongodb://localhost:27017/tasks_test ./mvnw test
 * @DataMongoTest starts only the MongoDB slice (no web layer); TaskService is imported explicitly.
 */
@DataMongoTest(properties = "spring.mongodb.uri=${MONGODB_URI}")
@Import({MongoConfig.class, TaskService.class, TaskRepositoryTest.FixedClock.class})
@EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
class TaskRepositoryTest {
    static class FixedClock {
        @org.springframework.context.annotation.Bean   // a different name: Boot forbids overriding the app's "clock"
        @org.springframework.context.annotation.Primary
        Clock fixedClock() { return Clock.fixed(Instant.parse("2026-11-17T10:00:00Z"), ZoneOffset.UTC); }
    }

    @Autowired TaskRepository tasks;
    @Autowired TaskService service;

    @BeforeEach
    void clean() {
        tasks.deleteAll();
    }

    Task task(String title, Priority p, LocalDate due, String... tags) {
        Task t = new Task("TT", title, p, due);
        t.getTags().addAll(List.of(tags));
        return t;
    }

    @Test
    void derivedQueriesAndAggregation() {
        tasks.saveAll(List.of(
                task("A", Priority.HIGH, LocalDate.of(2026, 11, 10), "release"),
                task("B", Priority.HIGH, LocalDate.of(2026, 11, 25), "docs", "release"),
                task("C", Priority.LOW, LocalDate.of(2026, 11, 20), "docs")));

        assertThat(tasks.findByProjectKeyAndDoneFalseOrderByDueDateAsc("TT")).extracting(Task::getTitle)
                .containsExactly("A", "C", "B");
        assertThat(tasks.findByTags("release")).hasSize(2);
        assertThat(service.openByPriority("TT").getFirst()).containsEntry("_id", "HIGH").containsEntry("open", 2);
        assertThat(service.completeOverdue()).isEqualTo(1);                     // only A is before 2026-11-17
    }

    @Test
    void commentsArePushedAtomicallyAndStaleSavesFail() {
        Task saved = tasks.save(task("A", Priority.HIGH, LocalDate.of(2026, 11, 20)));
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();                           // auditing

        assertThat(service.addComment(saved.getId(), "asha", "on it")).isTrue();  // version 0 -> 1 on the server
        assertThat(tasks.findById(saved.getId()).orElseThrow().getComments()).hasSize(1);

        saved.setDone(true);                                                    // our copy still says version 0
        assertThatThrownBy(() -> tasks.save(saved)).isInstanceOf(OptimisticLockingFailureException.class);
    }
}
