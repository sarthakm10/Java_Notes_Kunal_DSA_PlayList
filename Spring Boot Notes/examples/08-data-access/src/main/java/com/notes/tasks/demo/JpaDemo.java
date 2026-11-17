package com.notes.tasks.demo;

import com.notes.tasks.model.AppUser;
import com.notes.tasks.model.AuditInfo;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Project;
import com.notes.tasks.model.Tag;
import com.notes.tasks.model.Task;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * Runs only with --spring.profiles.active=demo. Each step prints a header; the SQL that Hibernate
 * sends appears as "org.hibernate.SQL" log lines between the headers.
 * TransactionTemplate = a transaction in code (file 09 compares it with @Transactional).
 */
@Component
@Profile("demo")
public class JpaDemo implements ApplicationRunner {
    private final EntityManager em;             // a shared proxy: talks to the current transaction's context
    private final TransactionTemplate tx;
    private final Statistics stats;

    public JpaDemo(EntityManager em, TransactionTemplate tx, EntityManagerFactory emf) {
        this.em = em;
        this.tx = tx;
        this.stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    private static void step(String s) { System.out.println("\n=== " + s); }
    private static void say(String s)  { System.out.println("    " + s); }

    @Override
    public void run(ApplicationArguments args) {
        lifecycle();
        firstLevelCache();
        dirtyChecking();
        secondLevelCache();
        lazyLoading();
        cascadeAndOrphanRemoval();
        System.out.println("\n=== demo finished");
    }

    void lifecycle() {
        step("1. Entity lifecycle: transient -> managed -> detached -> merged -> removed");
        AppUser dev = new AppUser("dev", "dev@example.com");
        say("transient: id=" + dev.getId());
        tx.executeWithoutResult(s -> {
            em.persist(dev);                                     // IDENTITY: INSERT runs now to get the id
            say("managed:   id=" + dev.getId() + ", em.contains=" + em.contains(dev));
        });
        say("detached:  em.contains=" + tx.execute(s -> em.contains(dev)));
        dev.setEmail("dev@new.example.com");                     // change while detached: nothing is sent
        say("changed the detached object (no SQL)");
        AppUser merged = tx.execute(s -> em.merge(dev));         // SELECT current state, then UPDATE at commit
        say("merge returned a different instance: " + (merged != dev));
        tx.executeWithoutResult(s -> em.remove(em.find(AppUser.class, dev.getId())));
        say("removed");
    }

    void firstLevelCache() {
        step("2. First-level cache (persistence context): one per transaction");
        tx.executeWithoutResult(s -> {
            Task a = em.find(Task.class, 1L);
            Task b = em.find(Task.class, 1L);                    // no second SELECT
            say("same instance within one transaction: " + (a == b));
        });
        tx.executeWithoutResult(s -> {
            em.find(Task.class, 1L);                             // new transaction -> new context -> SELECT again
            say("new transaction loaded it again");
        });
    }

    void dirtyChecking() {
        step("3. Dirty checking: change a managed entity, no save() call");
        tx.executeWithoutResult(s -> {
            Task t = em.find(Task.class, 3L);
            t.setTitle("Add validation (Bean Validation)");
            say("title changed in memory; committing...");
        });                                                      // UPDATE is sent at commit (flush)
        say("committed");
    }

    void secondLevelCache() {
        step("4. Second-level cache: shared across transactions (Tag is @Cacheable)");
        stats.clear();
        tx.executeWithoutResult(s -> say("tx A: " + em.find(Tag.class, 1L).getName()));
        tx.executeWithoutResult(s -> say("tx B: " + em.find(Tag.class, 1L).getName() + " (no SQL above this line)"));
        say("L2 puts=" + stats.getSecondLevelCachePutCount()
                + " hits=" + stats.getSecondLevelCacheHitCount()
                + " misses=" + stats.getSecondLevelCacheMissCount());
    }

    void lazyLoading() {
        step("5. Lazy loading");
        tx.executeWithoutResult(s -> {
            Project p = em.find(Project.class, 1L);
            say("loaded project '" + p.getName() + "' (tasks not loaded yet)");
            say("tasks: " + p.getTasks().size());                // SELECT of the tasks happens here
        });
        Project detached = tx.execute(s -> em.find(Project.class, 2L));
        try {
            detached.getTasks().size();                          // no transaction, no session
        } catch (LazyInitializationException e) {
            say("outside the transaction: " + e.getClass().getSimpleName());
            say(e.getMessage());
        }
    }

    void cascadeAndOrphanRemoval() {
        step("6. Cascade (persist through the parent) and orphan removal");
        Long newId = tx.execute(s -> {
            Project p = em.find(Project.class, 2L);
            Task t = new Task("Write the footer", Priority.LOW, new AuditInfo(Instant.parse("2026-11-16T09:00:00Z"), "meera"));
            p.addTask(t);                                        // no em.persist(t): cascade does it
            em.flush();
            say("task saved through the project, id=" + t.getId());
            return t.getId();
        });
        tx.executeWithoutResult(s -> {
            Project p = em.find(Project.class, 2L);
            p.removeTask(em.find(Task.class, newId));            // removed from the list -> orphan -> DELETE
            say("removed from project.tasks; committing...");
        });
    }
}
