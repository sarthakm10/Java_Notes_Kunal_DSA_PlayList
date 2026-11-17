package com.notes.mongo.repo;

import com.notes.mongo.model.Priority;
import com.notes.mongo.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

/** The same Spring Data repository model as JPA (file 08): derived queries, @Query, paging. */
public interface TaskRepository extends MongoRepository<Task, String> {

    // derived: { projectKey: ?0, done: false } sorted by due ascending
    List<Task> findByProjectKeyAndDoneFalseOrderByDueDateAsc(String projectKey);

    // on an array field, equality means "contains": { tags: ?0 }
    List<Task> findByTags(String tag);

    // a path into the embedded assignee: { "assignee.name": ?0 }
    Page<Task> findByAssigneeName(String name, Pageable page);

    // MongoDB's own query language when deriving gets awkward; ?0 ?1 are the parameters
    @Query("{ 'priority': ?0, 'done': false, 'due': { $lt: ?1 } }")
    List<Task> findOverdue(Priority priority, LocalDate today);

    long countByProjectKeyAndDoneFalse(String projectKey);
}
