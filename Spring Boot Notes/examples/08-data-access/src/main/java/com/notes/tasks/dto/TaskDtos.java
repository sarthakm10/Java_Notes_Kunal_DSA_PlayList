package com.notes.tasks.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Tag;
import com.notes.tasks.model.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class TaskDtos {
    private TaskDtos() {}

    public record CreateTaskRequest(
            @NotBlank @Size(max = 60) String title,
            @Size(max = 500) String description,
            Priority priority,
            Long assigneeId,
            LocalDate dueDate,
            Set<@NotBlank String> tags) {}

    public record PatchTaskRequest(@Size(min = 1, max = 60) String title, Boolean done, Priority priority) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TaskResponse(long id, String project, String title, String description, Priority priority,
                               boolean done, LocalDate dueDate, String assignee, List<String> tags,
                               Instant createdAt, String createdBy) {
        /** Must be called inside a transaction: it touches lazy associations. */
        public static TaskResponse from(Task t) {
            return new TaskResponse(t.getId(), t.getProject().getName(), t.getTitle(), t.getDescription(),
                    t.getPriority(), t.isDone(), t.getDueDate(),
                    t.getAssignee() == null ? null : t.getAssignee().getUsername(),
                    t.getTags().stream().map(Tag::getName).sorted().toList(),
                    t.getAudit().getCreatedAt(), t.getAudit().getCreatedBy());
        }
    }
}
