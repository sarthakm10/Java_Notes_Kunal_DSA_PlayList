package com.notes.mongo.service;

import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Task;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** MongoTemplate for what repositories can't express: partial updates, pipelines, bulk changes. */
@Service
public class TaskService {
    private final MongoTemplate mongo;
    private final Clock clock;

    public TaskService(MongoTemplate mongo, Clock clock) {
        this.mongo = mongo;
        this.clock = clock;
    }

    public List<Task> search(String projectKey, Priority priority, String tag, LocalDate dueBefore) {
        return mongo.find(TaskQueries.search(projectKey, priority, tag, dueBefore), Task.class);
    }

    /** true if the task existed. No read-modify-write: the server appends the comment atomically. */
    public boolean addComment(String taskId, String author, String text) {
        var comment = new Task.Comment(author, text, Instant.now(clock));
        return mongo.updateFirst(Query.query(Criteria.where("id").is(taskId)), TaskQueries.addComment(comment),
                Task.class).getMatchedCount() == 1;
    }

    public List<Document> openByPriority(String projectKey) {
        // input type Task.class: names the collection AND maps field names (dueDate -> due) in each stage
        return mongo.aggregate(TaskQueries.openByPriority(projectKey), Task.class, Document.class).getMappedResults();
    }

    public List<Document> tagUsage() {
        return mongo.aggregate(TaskQueries.tagUsage(), Task.class, Document.class).getMappedResults();
    }

    public long completeOverdue() {
        return mongo.updateMulti(TaskQueries.overdue(LocalDate.now(clock)), TaskQueries.complete(Instant.now(clock)),
                Task.class).getModifiedCount();
    }
}
