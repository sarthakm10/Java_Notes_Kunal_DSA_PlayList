package com.notes.tasks.security;

import com.notes.tasks.repo.TaskRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Used from @PreAuthorize expressions as @taskSecurity.isAssignee(#id, authentication). */
@Component("taskSecurity")
public class TaskSecurity {
    private final TaskRepository tasks;

    public TaskSecurity(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public boolean isAssignee(long taskId, Authentication auth) {
        return tasks.findById(taskId)
                .map(t -> t.getAssignee() != null && t.getAssignee().getUsername().equals(auth.getName()))
                .orElse(true);                         // let the service answer 404 for unknown ids
    }
}
