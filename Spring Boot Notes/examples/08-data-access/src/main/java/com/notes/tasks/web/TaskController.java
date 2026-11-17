package com.notes.tasks.web;

import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.dto.TaskDtos.PatchTaskRequest;
import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.jdbc.ReportDao;
import com.notes.tasks.jdbc.ReportDao.ProjectSummary;
import com.notes.tasks.jdbc.ReportDao.TagUsage;
import com.notes.tasks.service.TaskService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskService service;
    private final ReportDao reports;

    public TaskController(TaskService service, ReportDao reports) {
        this.service = service;
        this.reports = reports;
    }

    @GetMapping("/projects/{id}/tasks")
    public List<TaskResponse> tasksOfProject(@PathVariable long id) {
        return service.tasksOfProject(id);
    }

    @PostMapping("/projects/{id}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable long id, @Valid @RequestBody CreateTaskRequest body) {
        TaskResponse created = service.create(id, body);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tasks/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse get(@PathVariable long id) {
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
