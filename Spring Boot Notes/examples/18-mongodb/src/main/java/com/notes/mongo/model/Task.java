package com.notes.mongo.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One task = one document. What JPA spread over task, task_tag, tag and app_user (files 07-09) lives here
 * together: tags as an array, comments EMBEDDED, the assignee as an embedded SNAPSHOT (id + name).
 */
@Document("tasks")
@CompoundIndex(name = "project_open_due", def = "{'projectKey': 1, 'done': 1, 'due': 1}")   // the board query
public class Task {
    @Id
    private String id;                      // null until saved; MongoDB generates an ObjectId

    private String projectKey;              // a reference BY VALUE to projects.key (no foreign keys in MongoDB)
    private String title;
    private Priority priority = Priority.MEDIUM;
    private boolean done;

    @Field("due")                           // stored as "due", called dueDate in Java
    private LocalDate dueDate;

    private Assignee assignee;              // embedded: no join needed to show the name

    @Indexed                                // a multikey index: one entry per array element
    private List<String> tags = new ArrayList<>();

    private List<Comment> comments = new ArrayList<>();   // embedded, grows with the task (bounded!)

    @Version
    private Long version;                   // optimistic locking, like JPA's @Version (file 09)

    @CreatedDate
    private Instant createdAt;              // set by auditing (@EnableMongoAuditing)

    private Instant completedAt;            // added in 0.18: older documents simply don't have it (null)

    public record Assignee(long userId, String name) {}

    public record Comment(String author, String text, Instant at) {}

    protected Task() {}                     // for Spring Data

    public Task(String projectKey, String title, Priority priority, LocalDate dueDate) {
        this.projectKey = projectKey;
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public String getProjectKey() { return projectKey; }
    public String getTitle() { return title; }
    public Priority getPriority() { return priority; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public LocalDate getDueDate() { return dueDate; }
    public Assignee getAssignee() { return assignee; }
    public void setAssignee(Assignee assignee) { this.assignee = assignee; }
    public List<String> getTags() { return tags; }
    public List<Comment> getComments() { return comments; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
