package com.notes.ms.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** The task-service owns tasks. A task stores only the assignee's ID; the name comes from user-service. */
@RestController
public class TaskController {
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    record Task(long id, String title, long assigneeId) {}

    public record TaskView(long id, String title, String assignee, String assigneeServedBy) {}

    private static final Map<Long, Task> TASKS = Map.of(
            1L, new Task(1, "Design the schema", 2),
            2L, new Task(2, "Write REST controllers", 3),
            3L, new Task(3, "Plan the release", 1));

    private final UserLookup users;

    public TaskController(UserLookup users) {
        this.users = users;
    }

    @GetMapping("/tasks/{id}")
    public TaskView get(@PathVariable long id) {
        log.info("GET /tasks/{}", id);
        Task t = TASKS.get(id);
        if (t == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task " + id + " does not exist");
        UserClient.User u = users.find(t.assigneeId());               // a network call to another service
        return new TaskView(t.id(), t.title(), u.name(), u.servedBy());
    }
}
