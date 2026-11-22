package com.notes.tasks.repo;

import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Task;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A JPA slice: entities, repositories, Flyway and a DataSource; no web layer, no services.
 * Runs against the local PostgreSQL (schema tracker16_test), not an in-memory database.
 * Every test runs in a transaction that is ROLLED BACK at the end, so tests can't affect each other.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // keep our PostgreSQL
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired TaskRepository tasks;
    @Autowired TestEntityManager em;
    @Autowired EntityManagerFactory emf;
    Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void derivedQueryFindsOpenTasksByDueDate() {
        List<Long> ids = tasks.findByProjectIdAndDoneFalseOrderByDueDateAsc(1L).stream().map(Task::getId).toList();
        assertThat(ids).containsExactly(7L, 8L, 9L, 10L, 11L, 12L, 3L);
    }

    @ParameterizedTest(name = "done={0}, assignee={1} -> {2} tasks")
    @CsvSource(nullValues = "-", value = {
            "false, meera, 4",
            "true,  ravi,  3",
            "-,     ravi,  5",
            "false, -,     8"})
    void specificationsCombineOptionalFilters(Boolean done, String assignee, int expected) {
        assertThat(tasks.findAll(TaskSpecs.matching(done, null, assignee, null, null))).hasSize(expected);
    }

    @Test
    void fetchJoinLoadsAProjectsTasksInOneStatement() {
        stats.clear();
        List<Task> list = tasks.findWithDetailsByProjectId(1L);
        list.forEach(t -> t.getTags().size());                       // touching the tags must not query again
        assertThat(list).hasSize(11);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);   // an N+1 regression would fail here
    }

    @Test
    void bulkUpdateCompletesOverdueTasks() {
        int changed = tasks.completeOverdue(LocalDate.of(2026, 11, 16));
        assertThat(changed).isEqualTo(3);
        assertThat(tasks.findAll(TaskSpecs.isDone(false), Sort.by(Task::getId))).extracting(Task::getId)
                .containsExactly(3L, 4L, 10L, 11L, 12L);
    }                                                                // rolled back: the next test sees 8 open tasks again

    @Test
    void versionIncrementsOnEveryUpdate() {
        Task t = tasks.findById(4L).orElseThrow();
        long before = t.getVersion();
        t.setPriority(Priority.LOW);
        em.flush();                                                  // force the UPDATE now
        assertThat(t.getVersion()).isEqualTo(before + 1);
    }
}
