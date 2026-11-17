package com.notes.tasks.service;

import com.notes.tasks.config.TaskTrackerProperties;
import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.dto.TaskDtos.PatchTaskRequest;
import com.notes.tasks.dto.TaskDtos.ReplaceTaskRequest;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** Business logic. Stores tasks in memory for now; file 08 swaps this for PostgreSQL. */
@Service
public class TaskService {
    private final Map<Long, Task> tasks = new ConcurrentSkipListMap<>();
    private final AtomicLong ids = new AtomicLong(1);
    private final Clock clock;
    private final TaskTrackerProperties props;

    public TaskService(Clock clock, TaskTrackerProperties props) {
        this.clock = clock;
        this.props = props;
    }

    public List<Task> findAll(Boolean done, Priority priority) {
        return tasks.values().stream()
                .filter(t -> done == null || t.isDone() == done)
                .filter(t -> priority == null || t.getPriority() == priority)
                .toList();
    }

    public Task get(long id) {
        Task t = tasks.get(id);
        if (t == null) throw new TaskNotFoundException(id);
        return t;
    }

    public Task create(CreateTaskRequest req) {
        long open = tasks.values().stream().filter(t -> !t.isDone()).count();
        if (open >= props.maxOpenTasks()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Too many open tasks (max " + props.maxOpenTasks() + ")");
        }
        Task t = Task.builder()
                .id(ids.getAndIncrement())
                .title(req.title())
                .description(req.description())
                .priority(req.priority() == null ? Priority.MEDIUM : req.priority())
                .dueDate(req.dueDate())
                .createdAt(Instant.now(clock))
                .internalNotes("created via API")
                .build();
        tasks.put(t.getId(), t);
        return t;
    }

    public Task replace(long id, ReplaceTaskRequest req) {
        Task t = get(id);
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
        t.setDone(req.done());
        t.setDueDate(req.dueDate());
        return t;
    }

    public Task patch(long id, PatchTaskRequest req) {
        Task t = get(id);
        if (req.title() != null) t.setTitle(req.title());
        if (req.description() != null) t.setDescription(req.description());
        if (req.priority() != null) t.setPriority(req.priority());
        if (req.done() != null) t.setDone(req.done());
        if (req.dueDate() != null) t.setDueDate(req.dueDate());
        return t;
    }

    public void delete(long id) {
        if (tasks.remove(id) == null) throw new TaskNotFoundException(id);
    }
}
