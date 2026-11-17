package com.notes.tasks.repo;

import com.notes.tasks.model.AppUser;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Tag;
import com.notes.tasks.model.Task;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Reusable WHERE-clause pieces. Each one is a Criteria API predicate. */
public final class TaskSpecs {
    private TaskSpecs() {}

    public static Specification<Task> isDone(boolean done) {
        return (root, query, cb) -> cb.equal(root.get("done"), done);
    }

    public static Specification<Task> hasPriority(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> assignedTo(String username) {
        return (root, query, cb) -> {
            Join<Task, AppUser> a = root.join("assignee", JoinType.INNER);
            return cb.equal(a.get("username"), username);
        };
    }

    public static Specification<Task> hasTag(String tagName) {
        return (root, query, cb) -> {
            Join<Task, Tag> tag = root.join("tags", JoinType.INNER);
            query.distinct(true);                       // a task with the tag twice can't appear twice
            return cb.equal(tag.get("name"), tagName);
        };
    }

    public static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date);
    }

    /** Only the filters the client actually sent; no filters = all tasks. */
    public static Specification<Task> matching(Boolean done, Priority priority, String assignee,
                                               String tag, LocalDate dueBefore) {
        List<Specification<Task>> specs = new ArrayList<>();
        if (done != null) specs.add(isDone(done));
        if (priority != null) specs.add(hasPriority(priority));
        if (assignee != null) specs.add(assignedTo(assignee));
        if (tag != null) specs.add(hasTag(tag));
        if (dueBefore != null) specs.add(dueBefore(dueBefore));
        return Specification.allOf(specs);
    }
}
