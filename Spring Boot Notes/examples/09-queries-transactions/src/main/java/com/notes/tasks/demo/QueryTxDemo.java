package com.notes.tasks.demo;

import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Project;
import com.notes.tasks.model.Task;
import com.notes.tasks.repo.ActivityLogRepository;
import com.notes.tasks.repo.TaskRepository;
import com.notes.tasks.repo.TaskSpecs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Runs only with --spring.profiles.active=demo. SQL appears as "SQL :" log lines between the steps. */
@Component
@Profile("demo")
public class QueryTxDemo implements ApplicationRunner {
    private final EntityManager em;
    private final TaskRepository tasks;
    private final ActivityLogRepository logs;
    private final OuterSteps outer;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager txManager;
    private final TransactionTemplate tx;
    private final Statistics stats;

    public QueryTxDemo(EntityManager em, TaskRepository tasks, ActivityLogRepository logs, OuterSteps outer,
                       JdbcTemplate jdbc, PlatformTransactionManager txManager, EntityManagerFactory emf) {
        this.em = em;
        this.tasks = tasks;
        this.logs = logs;
        this.outer = outer;
        this.jdbc = jdbc;
        this.txManager = txManager;
        this.tx = new TransactionTemplate(txManager);
        this.stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    private static void step(String s) { System.out.println("\n=== " + s); }
    private static void say(String s)  { System.out.println("    " + s); }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        nPlusOne();
        derivedAndProjections();
        paging();
        specificationsAndCriteria();
        bulkUpdate();
        propagation();
        rollbackRules();
        proxyPitfalls();
        optimisticLocking();
        pessimisticLocking();
        isolation();
        System.out.println("\n=== demo finished");
    }

    // ---------------------------------------------------------------- queries

    private long countStatements(Runnable work) {
        stats.clear();
        tx.executeWithoutResult(s -> work.run());
        return stats.getPrepareStatementCount();
    }

    private static String describe(List<Task> list) {
        return list.stream().map(t -> t.getTitle() + "/" + (t.getAssignee() == null ? "-" : t.getAssignee().getUsername())
                + "/" + t.getTags().size()).toList().toString();
    }

    void nPlusOne() {
        step("1. N+1 and three fixes (project 1 has 11 tasks)");
        long naive = countStatements(() -> {
            Project p = em.find(Project.class, 1L);
            describe(p.getTasks());                      // touches assignee and tags of every task
        });
        say("lazy loading:  " + naive + " statements");
        long join = countStatements(() -> describe(tasks.findWithDetailsByProjectId(1L)));
        say("JOIN FETCH:    " + join + " statement(s)");
        long graph = countStatements(() -> describe(tasks.findByProjectIdOrderById(1L)));
        say("@EntityGraph:  " + graph + " statement(s)");
    }

    void derivedAndProjections() {
        step("2. Derived queries and projections");
        tx.executeWithoutResult(s -> {
            say("open in project 1 by due date: " + tasks.findByProjectIdAndDoneFalseOrderByDueDateAsc(1L)
                    .stream().map(Task::getId).toList());
            say("meera's open tasks: " + tasks.countByAssigneeUsernameAndDoneFalse("meera"));
            say("'add VALIDATION' exists: " + tasks.existsByProjectIdAndTitleIgnoreCase(1L, "add VALIDATION"));
            say("top 3 due soonest: " + tasks.findTop3ByDoneFalseOrderByDueDateAscIdAsc()
                    .stream().map(t -> t.getId() + "@" + t.getDueDate()).toList());
            say("ravi's titles (interface projection): " + tasks.findByAssigneeUsernameOrderById("ravi")
                    .stream().map(TaskRepository.TitleOnly::getTitle).toList());
            say("first open summary (DTO projection): " + tasks.openSummaries().getFirst());
            say("JPQL like '%item 1%': " + tasks.searchInProject(1L, "item 1").stream().map(Task::getTitle).toList());
            say("native SQL, tag backend: " + tasks.findByTagNative("backend").stream().map(Task::getId).toList());
        });
    }

    void paging() {
        step("3. Paging and sorting");
        tx.executeWithoutResult(s -> {
            Page<Task> page = tasks.findByPriority(Priority.MEDIUM, PageRequest.of(0, 2, Sort.by(Task::getId)));
            say("Page: content=" + page.getContent().stream().map(Task::getId).toList()
                    + " totalElements=" + page.getTotalElements() + " totalPages=" + page.getTotalPages());
            Slice<Task> slice = tasks.findByDone(false, PageRequest.of(0, 3, Sort.by(Task::getDueDate, Task::getId)));
            say("Slice: content=" + slice.getContent().stream().map(Task::getId).toList()
                    + " hasNext=" + slice.hasNext());
            Page<Task> withTags = tasks.findAllWithTags(PageRequest.of(0, 3, Sort.by(Task::getId)));
            say("collection fetch + paging: content=" + withTags.getContent().stream().map(Task::getId).toList());
        });
    }

    void specificationsAndCriteria() {
        step("4. Specifications and the Criteria API");
        tx.executeWithoutResult(s -> {
            List<Task> open = tasks.findAll(TaskSpecs.isDone(false).and(TaskSpecs.hasTag("backend")), Sort.by(Task::getId));
            say("open AND tag=backend: " + open.stream().map(Task::getId).toList());

            CriteriaBuilder cb = em.getCriteriaBuilder();             // the same thing written by hand
            CriteriaQuery<Task> q = cb.createQuery(Task.class);
            Root<Task> t = q.from(Task.class);
            q.select(t)
             .where(cb.equal(t.get("priority"), Priority.HIGH), cb.isFalse(t.get("done")))
             .orderBy(cb.asc(t.get("id")));
            say("Criteria: HIGH and not done: " + em.createQuery(q).getResultList().stream().map(Task::getId).toList());
        });
    }

    void bulkUpdate() {
        step("5. Bulk update vs the persistence context");
        tx.executeWithoutResult(s -> {
            Task t = em.find(Task.class, 7L);                          // Backlog item 3: open, due 2026-11-13
            int n = em.createQuery("update Task t set t.done = true where t.id = 7").executeUpdate();
            say("bulk update changed " + n + " row; managed entity still says done=" + t.isDone());
            em.refresh(t);
            say("after refresh: done=" + t.isDone() + ", version=" + t.getVersion() + " (bulk updates skip @Version)");
            s.setRollbackOnly();                                       // keep the data for later steps
        });
    }

    // ---------------------------------------------------------------- transactions

    private String title3() {
        return jdbc.queryForObject("select title from task where id = 3", String.class);
    }

    private void attempt(String label, ThrowingRunnable r) {
        try {
            r.run();
            say(label + ": returned normally");
        } catch (Exception e) {
            say(label + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        say("task 3 title in the database: '" + title3() + "'");
    }

    interface ThrowingRunnable { void run() throws Exception; }

    void propagation() {
        step("6. Propagation");
        attempt("REQUIRED inner fails, outer catches", outer::catchJoinedFailure);
        attempt("REQUIRES_NEW inner fails, outer catches", outer::catchNewTransactionFailure);
        long before = logs.count();
        attempt("outer logs (REQUIRES_NEW) then fails", outer::logThenFail);
        say("activity_log rows: " + before + " -> " + logs.count() + " (the log survived the rollback)");
        attempt("MANDATORY without a transaction", outer::callMandatoryWithoutTransaction);
    }

    void rollbackRules() {
        step("7. Rollback rules");
        attempt("checked exception, default rules", outer::failWithCheckedException);
        attempt("checked exception, rollbackFor = Exception.class", outer::failWithCheckedExceptionRollbackFor);
    }

    void proxyPitfalls() {
        step("8. Proxy pitfalls and readOnly");
        say("public @Transactional through the proxy: active=" + outer.publicTransactional());
        say("package-private @Transactional through the proxy: active=" + outer.packagePrivateTransactional());
        say("self-invocation (this.publicTransactional()): active=" + outer.selfInvocation());
        attempt("rename inside readOnly = true", outer::renameInReadOnly);
    }

    void optimisticLocking() {
        step("9. Optimistic locking (@Version)");
        Task alice = tx.execute(s -> em.find(Task.class, 4L));      // two users load the same task...
        Task bob = tx.execute(s -> em.find(Task.class, 4L));
        Task carol = tx.execute(s -> em.find(Task.class, 4L));
        say("all three loaded version " + alice.getVersion());
        alice.setTitle("New landing page (alice)");
        tx.executeWithoutResult(s -> em.merge(alice));               // ...alice saves first
        say("alice saved; version is now " + jdbc.queryForObject("select version from task where id = 4", Long.class));
        bob.setTitle("New landing page (bob)");
        try {
            tx.executeWithoutResult(s -> em.merge(bob));             // bob's copy still has version 0
        } catch (Exception e) {
            say("bob: " + e.getClass().getSimpleName());
            say("     " + e.getMessage());
        }
        carol.setTitle("New landing page (carol)");
        try {
            tasks.save(carol);                                       // the same merge, through a repository
        } catch (Exception e) {
            say("carol (via repository): " + e.getClass().getSimpleName());
        }
    }

    void pessimisticLocking() throws InterruptedException {
        step("10. Pessimistic locking (SELECT ... FOR UPDATE)");
        CountDownLatch t1HasLock = new CountDownLatch(1);
        CountDownLatch t2Asking = new CountDownLatch(1);
        Thread t1 = new Thread(() -> tx.executeWithoutResult(s -> {
            tasks.findByIdForUpdate(3L);
            say("T1 locked task 3");
            t1HasLock.countDown();
            await(t2Asking);
            pause(500);                                               // T2 is now blocked in the database
            say("T1 commits");
        }));
        Thread t2 = new Thread(() -> {
            await(t1HasLock);
            tx.executeWithoutResult(s -> {
                say("T2 asks for the same lock");
                t2Asking.countDown();
                tasks.findByIdForUpdate(3L);                          // waits until T1 commits
                say("T2 got the lock");
            });
        });
        t1.start(); t2.start(); t1.join(); t2.join();
    }

    void isolation() throws InterruptedException {
        step("11. Isolation levels (PostgreSQL)");
        for (int level : new int[]{TransactionDefinition.ISOLATION_READ_COMMITTED,
                                   TransactionDefinition.ISOLATION_REPEATABLE_READ}) {
            TransactionTemplate reader = new TransactionTemplate(txManager);
            reader.setIsolationLevel(level);
            String name = level == TransactionDefinition.ISOLATION_READ_COMMITTED ? "READ COMMITTED" : "REPEATABLE READ";
            CountDownLatch firstRead = new CountDownLatch(1);
            CountDownLatch otherCommitted = new CountDownLatch(1);
            Thread writer = new Thread(() -> {
                await(firstRead);
                tx.executeWithoutResult(s -> jdbc.update("update task set title = ? where id = 3", "changed by T2"));
                otherCommitted.countDown();
            });
            writer.start();
            try {
                reader.executeWithoutResult(s -> {
                    say(name + ": T1 reads '" + title3() + "'");
                    firstRead.countDown();
                    await(otherCommitted);
                    say(name + ": T2 committed; T1 reads again '" + title3() + "'");
                    jdbc.update("update task set title = ? where id = 3", "changed by T1");
                    say(name + ": T1 updated the same row");
                });
            } catch (Exception e) {
                Throwable root = e;
                while (root.getCause() != null) root = root.getCause();
                String state = root instanceof SQLException sql ? " SQLState " + sql.getSQLState() : "";
                say(name + ": T1's update failed: " + e.getClass().getSimpleName() + state);
                say("    " + root.getMessage().lines().findFirst().orElse(""));
            }
            writer.join();
            say(name + ": final title '" + title3() + "'");
        }
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new IllegalStateException(e); }
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { throw new IllegalStateException(e); }
    }
}
