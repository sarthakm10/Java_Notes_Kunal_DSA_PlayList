package com.notes.tasks.web;

import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.dto.TaskDtos.PageResponse;
import com.notes.tasks.dto.TaskDtos.PatchTaskRequest;
import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.dto.TaskSummary;
import com.notes.tasks.jdbc.ReportDao;
import com.notes.tasks.jdbc.ReportDao.ProjectSummary;
import com.notes.tasks.jdbc.ReportDao.TagUsage;
import com.notes.tasks.model.ActivityLog;
import com.notes.tasks.model.Priority;
import com.notes.tasks.repo.ActivityLogRepository;
import com.notes.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Tasks", description = "Tasks, projects and reports")    // groups the operations in Swagger UI
public class TaskController {
    private final TaskService service;
    private final ReportDao reports;
    private final ActivityLogRepository activity;

    public TaskController(TaskService service, ReportDao reports, ActivityLogRepository activity) {
        this.service = service;
        this.reports = reports;
        this.activity = activity;
    }

    /** GET /api/tasks?done=false&assignee=meera&page=0&size=5&sort=dueDate,desc  (all filters optional) */
    @GetMapping("/tasks")
    public PageResponse<TaskResponse> search(@RequestParam(required = false) Boolean done,
                                             @RequestParam(required = false) Priority priority,
                                             @RequestParam(required = false) String assignee,
                                             @RequestParam(required = false) String tag,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
                                             Pageable pageable) {      // built from page, size, sort
        return service.search(done, priority, assignee, tag, dueBefore, pageable);
    }

    @GetMapping("/tasks/open-summaries")
    public List<TaskSummary> openSummaries() {
        return service.openSummaries();
    }

    @PostMapping("/tasks/complete-overdue")
    public Map<String, Integer> completeOverdue() {
        return Map.of("completed", service.completeOverdue());
    }

    @GetMapping("/activity")
    public List<ActivityLog> activity() {
        return activity.findAllByOrderById();
    }

    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> tasksOfProject(@PathVariable long id) {
        return service.tasksOfProject(id);
    }

    @Operation(summary = "Create a task in a project")
    @ApiResponse(responseCode = "201", description = "Created; the Location header points to it")
    @PostMapping("/projects/{id}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable long id, @Valid @RequestBody CreateTaskRequest body) {
        TaskResponse created = service.create(id, body);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tasks/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get one task", description = "Users may read tasks assigned to them; admins read all.")
    @ApiResponse(responseCode = "200", description = "The task")
    @ApiResponse(responseCode = "403", description = "The task is assigned to someone else")
    @ApiResponse(responseCode = "404", description = "No task with this id")
    @GetMapping("/tasks/{id}")
    public TaskResponse get(@Parameter(description = "Task id", example = "1") @PathVariable long id) {
        return service.get(id);
    }

    @PatchMapping("/tasks/{id}")
    public TaskResponse patch(@PathVariable long id, @Valid @RequestBody PatchTaskRequest body) {
        return service.patch(id, body);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- reports straight from SQL (Spring JDBC) ----

    @GetMapping("/reports/projects")
    public List<ProjectSummary> projectSummaries() {
        return reports.projectSummaries();
    }

    @GetMapping("/reports/tags")
    public List<TagUsage> tagUsage(@RequestParam(defaultValue = "1") int minUses) {
        return reports.tagUsage(minUses);
    }

    @GetMapping("/reports/open-tasks/{username}")
    public Map<String, Object> openTasks(@PathVariable String username) {
        return Map.of("username", username, "openTasks", reports.openTasksOf(username));
    }
}
