package com.notes.tasks.repo;

import com.notes.tasks.dto.TaskSummary;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Task;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // ---- 1. Derived queries: Spring Data builds the JPQL from the method name ----

    List<Task> findByProjectIdAndDoneFalseOrderByDueDateAsc(long projectId);

    long countByAssigneeUsernameAndDoneFalse(String username);

    boolean existsByProjectIdAndTitleIgnoreCase(long projectId, String title);

    List<Task> findTop3ByDoneFalseOrderByDueDateAscIdAsc();

    Page<Task> findByPriority(Priority priority, Pageable pageable);     // runs a count query too

    Slice<Task> findByDone(boolean done, Pageable pageable);             // no count query: fetches size + 1 rows

    // ---- 2. Projections: read only some columns ----

    /** Interface projection: Spring Data implements the getters. */
    interface TitleOnly {
        Long getId();
        String getTitle();
    }

    List<TitleOnly> findByAssigneeUsernameOrderById(String username);

    /** DTO projection with a JPQL constructor expression. */
    @Query("""
            select new com.notes.tasks.dto.TaskSummary(t.id, t.title, t.priority, t.dueDate, a.username)
            from Task t left join t.assignee a
            where t.done = false
            order by t.dueDate, t.id""")
    List<TaskSummary> openSummaries();

    // ---- 3. JPQL and native SQL ----

    @Query("""
            select t from Task t
            where t.project.id = :projectId and lower(t.title) like lower(concat('%', :text, '%'))
            order by t.id""")
    List<Task> searchInProject(@Param("projectId") long projectId, @Param("text") String text);

    @Query(value = """
            SELECT t.* FROM task t
            JOIN task_tag tt ON tt.task_id = t.id
            JOIN tag g ON g.id = tt.tag_id
            WHERE g.name = :tag
            ORDER BY t.id""", nativeQuery = true)
    List<Task> findByTagNative(@Param("tag") String tag);

    // ---- 4. Fixing N+1: fetch the associations in the same query ----

    @Query("""
            select distinct t from Task t
            join fetch t.project
            left join fetch t.assignee a
            left join fetch a.profile
            left join fetch t.tags
            where t.project.id = :projectId
            order by t.id""")
    List<Task> findWithDetailsByProjectId(@Param("projectId") long projectId);

    @EntityGraph(attributePaths = {"project", "assignee", "assignee.profile", "tags"})
    List<Task> findByProjectIdOrderById(long projectId);

    /** Specifications + paging, with the to-one associations fetched by an entity graph. */
    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "assignee.profile"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    /** Loads the tags of tasks that are already in the persistence context: one query for the whole page. */
    @Query("select distinct t from Task t left join fetch t.tags where t.id in :ids")
    List<Task> fetchTags(@Param("ids") Collection<Long> ids);

    /** Paging + a collection fetch join: Hibernate has to page in memory (see the warning in the notes). */
    @Query(value = "select t from Task t left join fetch t.tags",
           countQuery = "select count(t) from Task t")
    Page<Task> findAllWithTags(Pageable pageable);

    // ---- 5. Bulk update and locking ----

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.done = true where t.done = false and t.dueDate < :today")
    int completeOverdue(@Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Task t where t.id = :id")
    Optional<Task> findByIdForUpdate(@Param("id") long id);
}
