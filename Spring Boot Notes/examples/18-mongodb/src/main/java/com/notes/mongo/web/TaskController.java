package com.notes.mongo.web;

import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Task;
import com.notes.mongo.repo.TaskRepository;
import com.notes.mongo.service.TaskService;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TaskController {
    private final TaskRepository tasks;
    private final TaskService service;

    public TaskController(TaskRepository tasks, TaskService service) {
        this.tasks = tasks;
        this.service = service;
    }

    @GetMapping("/projects/{key}/board")
    public List<Task> board(@PathVariable String key) {
        return tasks.findByProjectKeyAndDoneFalseOrderByDueDateAsc(key);
    }

    @GetMapping("/tasks")
    public List<Task> search(@RequestParam(required = false) String project, @RequestParam(required = false) Priority priority,
                             @RequestParam(required = false) String tag, @RequestParam(required = false) LocalDate dueBefore) {
        return service.search(project, priority, tag, dueBefore);
    }

    @PostMapping("/tasks/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> comment(@PathVariable String id, @RequestBody Map<String, String> body) {
        if (!service.addComment(id, body.get("author"), body.get("text")))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task " + id + " does not exist");
        return Map.of("task", id, "status", "comment added");
    }

    @GetMapping("/projects/{key}/open-by-priority")
    public List<Document> openByPriority(@PathVariable String key) {
        return service.openByPriority(key);
    }
}
