package com.notes.tasks.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Task;
import com.notes.tasks.validation.NoHtml;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

/** All request/response shapes of the Task API, as immutable records. */
public final class TaskDtos {
    private TaskDtos() {}

    /** POST /api/tasks body. The constraints are checked when the controller parameter has @Valid. */
    public record CreateTaskRequest(
            @NotBlank(message = "{task.title.required}")
            @Size(max = 60, message = "{task.title.size}")
            @NoHtml
            String title,

            @Size(max = 500)
            @NoHtml
            String description,

            Priority priority,                       // optional: MEDIUM by default

            @FutureOrPresent(message = "must be today or later")
            LocalDate dueDate) {}

    /** PUT /api/tasks/{id} body: the WHOLE new state, so more fields are required. */
    public record ReplaceTaskRequest(
            @NotBlank @Size(max = 60) @NoHtml String title,
            @Size(max = 500) @NoHtml String description,
            @NotNull Priority priority,
            boolean done,
            LocalDate dueDate) {}

    /** PATCH /api/tasks/{id} body: null means "leave unchanged", so nothing is @NotNull. */
    public record PatchTaskRequest(
            @Size(min = 1, max = 60) @NoHtml String title,
            @Size(max = 500) @NoHtml String description,
            Priority priority,
            Boolean done,
            LocalDate dueDate) {}

    /** Version 1 response. */
    @JsonInclude(JsonInclude.Include.NON_NULL)           // omit null fields (e.g. no description)
    public record TaskResponse(long id, String title, String description, Priority priority,
                               boolean done, LocalDate dueDate, Instant createdAt) {
        public static TaskResponse from(Task t) {
            return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getPriority(),
                    t.isDone(), t.getDueDate(), t.getCreatedAt());
        }
    }

    /** Version 2 response: "status" text instead of a boolean, renamed JSON field. */
    public record TaskResponseV2(long id, String title, String status, Priority priority,
                                 @JsonProperty("due") LocalDate dueDate) {
        public static TaskResponseV2 from(Task t) {
            return new TaskResponseV2(t.getId(), t.getTitle(), t.isDone() ? "DONE" : "OPEN",
                    t.getPriority(), t.getDueDate());
        }
    }

    public record AttachmentInfo(long taskId, String fileName, String contentType, long sizeBytes, int lines) {}
}
