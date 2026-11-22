package com.notes.mongo.service;

import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Task;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

/**
 * The queries, updates and pipelines TaskService sends through MongoTemplate, built in one place so
 * WhatMongoReceivesTest can print exactly what the driver would get (no server needed for that).
 * Fields are named with method references (Task::getDueDate), not strings ("dueDate"): Spring Data 2026.1
 * (in Boot 4.2) resolves them to the property name, so a typo or a renamed field is a compile error.
 * group/unwind/sort in a pipeline still take strings: they name pipeline fields, which may not exist on Task.
 */
public final class TaskQueries {
    private TaskQueries() {}

    /** Optional filters, combined only when present: the Criteria equivalent of file 09's Specifications. */
    public static Query search(String projectKey, Priority priority, String tag, LocalDate dueBefore) {
        Criteria c = new Criteria();
        if (projectKey != null) c = c.and(Task::getProjectKey).is(projectKey);
        if (priority != null) c = c.and(Task::getPriority).is(priority);
        if (tag != null) c = c.and(Task::getTags).is(tag);
        if (dueBefore != null) c = c.and(Task::getDueDate).lt(dueBefore);   // Java name: mapped to "due"
        return Query.query(c).with(Sort.by(Task::getDueDate)).limit(20);
    }

    /** Add a comment WITHOUT reading the document: one atomic update on the server.
     *  No inc("version"): MongoTemplate adds $inc on the @Version field to every update itself. */
    public static Update addComment(Task.Comment comment) {
        return new Update().push(Task::getComments, comment);
    }

    /** Open tasks per priority, highest count first: GROUP BY in a pipeline. */
    public static Aggregation openByPriority(String projectKey) {
        return newAggregation(Task.class,
                match(Criteria.where(Task::getProjectKey).is(projectKey).and(Task::isDone).is(false)),
                group("priority").count().as("open"),
                sort(Sort.Direction.DESC, "open"));
    }

    /** How often each tag is used: $unwind turns one task with 3 tags into 3 rows. */
    public static Aggregation tagUsage() {
        return newAggregation(Task.class,
                unwind("tags"),
                group("tags").count().as("tasks"),
                sort(Sort.Direction.DESC, "tasks").and(Sort.Direction.ASC, "_id"),
                limit(10));
    }

    /** Complete everything overdue in one statement (file 09's bulk JPQL update). */
    public static Query overdue(LocalDate today) {
        return Query.query(Criteria.where(Task::isDone).is(false).and(Task::getDueDate).lt(today));
    }

    public static Update complete(Instant at) {
        return new Update().set(Task::isDone, true).set(Task::getCompletedAt, at);
    }
}
