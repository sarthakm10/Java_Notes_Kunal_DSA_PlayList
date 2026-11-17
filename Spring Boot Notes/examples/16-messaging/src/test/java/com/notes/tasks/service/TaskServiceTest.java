package com.notes.tasks.service;

import com.notes.tasks.ops.TaskMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.notes.tasks.dto.TaskDtos.PatchTaskRequest;
import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.model.AuditInfo;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Project;
import com.notes.tasks.model.Task;
import com.notes.tasks.repo.AppUserRepository;
import com.notes.tasks.repo.ProjectRepository;
import com.notes.tasks.repo.TagRepository;
import com.notes.tasks.repo.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A plain unit test: no Spring context, no database. Collaborators are Mockito mocks. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock ProjectRepository projects;
    @Mock TaskRepository tasks;
    @Mock TagRepository tags;
    @Mock AppUserRepository users;
    @Mock ActivityService activity;

    final Clock clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC);
    TaskService service;
    Task task;

    @BeforeEach
    void setUp() {
        service = new TaskService(projects, tasks, tags, users, activity, clock,
                new TaskMetrics(new SimpleMeterRegistry()),                    // a real, in-memory registry
                event -> { });                                                 // events: ignored here   // constructor injection pays off
        Project project = new Project();
        project.setName("Task Tracker");
        task = new Task("Add validation", Priority.MEDIUM, new AuditInfo(Instant.parse("2026-10-22T09:00:00Z"), "asha"));
        task.setId(3L);
        project.addTask(task);
    }

    @Test
    @DisplayName("patch changes only the fields that were sent")
    void patchAppliesChanges() {
        when(tasks.findById(3L)).thenReturn(Optional.of(task));

        TaskResponse r = service.patch(3L, new PatchTaskRequest(null, true, Priority.HIGH, 0L));

        assertThat(r.done()).isTrue();
        assertThat(r.priority()).isEqualTo(Priority.HIGH);
        assertThat(r.title()).isEqualTo("Add validation");            // untouched
        verify(tasks).flush();
    }

    @Test
    @DisplayName("patch with a stale version is rejected and changes nothing")
    void patchRejectsStaleVersion() {
        task.setVersion(2);
        when(tasks.findById(3L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.patch(3L, new PatchTaskRequest("New title", null, null, 1L)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(task.getTitle()).isEqualTo("Add validation");
        verify(tasks, never()).flush();
    }

    @Test
    void getUnknownTaskThrowsNotFound() {
        when(tasks.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task 99 does not exist");
    }

    @Test
    void completeOverdueUsesTheClockAndLogsActivity() {
        when(tasks.completeOverdue(any())).thenReturn(3);

        assertThat(service.completeOverdue()).isEqualTo(3);
        verify(tasks).completeOverdue(LocalDate.of(2026, 11, 16));     // "today" comes from the fixed clock
        verify(activity).record("completed 3 overdue tasks");
    }
}
