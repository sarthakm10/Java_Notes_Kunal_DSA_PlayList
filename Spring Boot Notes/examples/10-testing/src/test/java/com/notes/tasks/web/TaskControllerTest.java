package com.notes.tasks.web;

import com.notes.tasks.dto.TaskDtos.PageResponse;
import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.jdbc.ReportDao;
import com.notes.tasks.model.Priority;
import com.notes.tasks.repo.ActivityLogRepository;
import com.notes.tasks.service.NotFoundException;
import com.notes.tasks.service.TaskService;
import com.notes.tasks.model.Task;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A web slice: only MVC beans (this controller, the @RestControllerAdvice, Jackson, validation).
 * No database, no services: TaskService is a Mockito mock registered as a bean.
 */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired MockMvcTester mvc;              // AssertJ-style MockMvc (Spring 6.2+)
    @Autowired MockMvc classicMvc;             // the classic perform/andExpect style

    @MockitoBean TaskService service;
    @MockitoBean ReportDao reports;
    @MockitoBean ActivityLogRepository activity;

    static final TaskResponse TASK_3 = new TaskResponse(3, "Task Tracker", "Add validation", null, Priority.MEDIUM,
            false, LocalDate.of(2026, 11, 20), "meera", List.of("backend", "urgent"),
            Instant.parse("2026-10-22T09:00:00Z"), "asha", 0);

    @Test
    void getReturnsTheTaskAsJson() {
        when(service.get(3L)).thenReturn(TASK_3);

        assertThat(mvc.get().uri("/api/tasks/3"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson()
                .hasPathSatisfying("$.title", v -> v.assertThat().isEqualTo("Add validation"))
                .hasPathSatisfying("$.tags", v -> v.assertThat().asArray().containsExactly("backend", "urgent"))
                .doesNotHavePath("$.description");                  // null fields are left out (NON_NULL)
    }

    @Test
    void sameTestInTheClassicStyle() throws Exception {
        when(service.get(3L)).thenReturn(TASK_3);

        classicMvc.perform(get("/api/tasks/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Add validation"))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void unknownTaskIs404ProblemDetail() {
        when(service.get(99L)).thenThrow(new NotFoundException("Task", 99));

        assertThat(mvc.get().uri("/api/tasks/99"))
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .hasPathSatisfying("$.detail", v -> v.assertThat().isEqualTo("Task 99 does not exist"));
    }

    @Test
    void invalidBodyIs400AndTheServiceIsNeverCalled() {
        assertThat(mvc.post().uri("/api/projects/1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "", "tags": ["this-tag-name-is-much-too-long-for-the-column"]}"""))
                .hasStatus(400);
        verifyNoInteractions(service);
    }

    @Test
    void staleVersionIs409() {
        when(service.patch(eq(3L), any())).thenThrow(new ObjectOptimisticLockingFailureException(Task.class, 3L));

        assertThat(mvc.patch().uri("/api/tasks/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Stale edit", "version": 0}"""))
                .hasStatus(409)
                .bodyJson()
                .hasPathSatisfying("$.title", v -> v.assertThat().isEqualTo("Conflict"));
    }

    @Test
    void queryParametersBecomeFiltersAndPageable() {
        when(service.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 1, 2, 0, 0));

        assertThat(mvc.get().uri("/api/tasks?done=false&assignee=meera&page=1&size=2&sort=dueDate,desc"))
                .hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).search(eq(false), isNull(), eq("meera"), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(2);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, Task::getDueDate));
    }
}
