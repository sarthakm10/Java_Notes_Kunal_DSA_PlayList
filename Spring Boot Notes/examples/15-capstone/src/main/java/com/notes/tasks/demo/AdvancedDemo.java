package com.notes.tasks.demo;

import com.notes.tasks.async.ReportJobs;
import com.notes.tasks.client.HolidayApi;
import com.notes.tasks.client.HolidayClient;
import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.jdbc.ReportDao;
import com.notes.tasks.model.Priority;
import com.notes.tasks.repo.ActivityLogRepository;
import com.notes.tasks.service.ExportService;
import com.notes.tasks.service.TaskService;
import com.sun.net.httpserver.HttpServer;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Runs only with --spring.profiles.active=demo: AOP, events, caching, @Async, scheduling, RestClient. */
@Component
@Profile("demo")
public class AdvancedDemo implements ApplicationRunner {
    private final TaskService tasks;
    private final ExportService export;
    private final ActivityLogRepository activity;
    private final ReportDao reports;
    private final CacheManager caches;
    private final ReportJobs jobs;
    private final ThreadPoolTaskExecutor reportExecutor;
    private final TransactionTemplate tx;
    private final HolidayClient holidayClient;
    private final HolidayApi holidayApi;

    public AdvancedDemo(TaskService tasks, ExportService export, ActivityLogRepository activity, ReportDao reports,
                        CacheManager caches, ReportJobs jobs, ThreadPoolTaskExecutor reportExecutor,
                        PlatformTransactionManager txManager, HolidayClient holidayClient, HolidayApi holidayApi) {
        this.tasks = tasks;
        this.export = export;
        this.activity = activity;
        this.reports = reports;
        this.caches = caches;
        this.jobs = jobs;
        this.reportExecutor = reportExecutor;
        this.tx = new TransactionTemplate(txManager);
        this.holidayClient = holidayClient;
        this.holidayApi = holidayApi;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // the services read the caller from the SecurityContext: act as asha (ADMIN)
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "asha", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        section("1. AOP: proxies, @Around timing, @Audited");
        System.out.println("    TaskService bean is a proxy: " + AopUtils.isAopProxy(tasks)
                + " (" + tasks.getClass().getSimpleName() + ")");
        tasks.get(1);                                               // TimingAspect logs at DEBUG
        long before = activity.count();
        export.exportProject(3);                                    // through the proxy: audited
        System.out.println("    exportProject(3) directly -> audit rows added: " + (activity.count() - before));
        before = activity.count();
        export.exportAll();                                         // calls this.exportProject twice
        System.out.println("    exportAll() (self-invocation x2) -> audit rows added: " + (activity.count() - before));
        tasks.tasksOfProject(1);
        activity.findAllByOrderById().stream().skip(activity.count() - 1)
                .forEach(a -> System.out.println("    last audit row: " + a.getMessage()));

        section("2. Events: @EventListener vs @TransactionalEventListener");
        System.out.println("    -- create a task (commits):");
        tasks.create(1, new CreateTaskRequest("Plan the retro", null, Priority.LOW, null, null, null));
        Thread.sleep(300);                                          // the after-commit listener is @Async
        System.out.println("    -- create a task inside a transaction that is rolled back:");
        tx.executeWithoutResult(status -> {
            tasks.create(1, new CreateTaskRequest("Never saved", null, Priority.LOW, null, null, null));
            status.setRollbackOnly();
        });
        Thread.sleep(300);

        section("3. Caching with @Cacheable / @CacheEvict");
        System.out.println("    CacheManager: " + caches.getClass().getSimpleName() + " " + caches.getCacheNames());
        for (int i = 1; i <= 3; i++) {
            System.out.println("    projectSummaries() call " + i + ": " + reports.projectSummaries().size() + " projects");
        }
        System.out.println("    openTasksOf(ravi) = " + reports.openTasksOf("ravi") + ", again = " + reports.openTasksOf("ravi")
                + ", openTasksOf(meera) = " + reports.openTasksOf("meera"));
        tasks.create(2, new CreateTaskRequest("Update the roadmap", null, Priority.MEDIUM, null, null, null));
        System.out.println("    after create (cache evicted): " + reports.projectSummaries().size() + " projects");
        Object nativeCache = caches.getCache("projectSummaries").getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> c) {
            System.out.println("    Caffeine stats: " + c.stats());
        }

        section("4. @Async with a bounded ThreadPoolTaskExecutor");
        long t0 = System.nanoTime();
        CompletableFuture<String> a = jobs.buildReport("weekly", 500);
        CompletableFuture<String> b = jobs.buildReport("monthly", 500);
        CompletableFuture<String> c = jobs.buildReport("yearly", 500);
        System.out.println("    3 calls returned after " + ms(t0) + " ms (futures, not results)");
        CompletableFuture.allOf(a, b, c).join();
        System.out.println("    all done after " + ms(t0) + " ms: " + List.of(a.join(), b.join(), c.join()));
        System.out.println("    -- submit 8 one-second jobs to a pool of core 2 / queue 2 / max 4:");
        for (int i = 1; i <= 8; i++) {
            int n = i;
            try {
                reportExecutor.execute(() -> sleep(1000));
                var pool = reportExecutor.getThreadPoolExecutor();
                System.out.println("    job " + n + " accepted: threads=" + pool.getPoolSize() + " queued=" + pool.getQueue().size());
            } catch (TaskRejectedException e) {
                System.out.println("    job " + n + " REJECTED (" + e.getClass().getSimpleName() + ")");
            }
        }
        sleep(2100);
        System.out.println("    -- default executor for @Async without a name:");
        jobs.sendDigest("team");
        jobs.sendDigest("");                                        // throws inside the async method
        sleep(300);

        section("5. @Scheduled (app.reminder.rate=PT1S in this profile)");
        sleep(2200);                                                // let the reminder fire a couple of times

        section("6. RestClient and an HTTP interface against a stub service");
        HttpServer stub = startStub();
        try {
            System.out.println("    RestClient 2026: " + holidayClient.holidays(2026));
            System.out.println("    HTTP interface 2026: " + holidayApi.holidays(2026));
            try {
                holidayClient.holidays(1999);
            } catch (RuntimeException e) {
                System.out.println("    1999 -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            try {
                holidayClient.holidays(2030);
            } catch (RuntimeException e) {
                System.out.println("    2030 -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } finally {
            stub.stop(0);
        }
        System.out.println("\n=== demo finished");
        SecurityContextHolder.clearContext();
    }

    /** A tiny stand-in for another team's service, on the port app.holidays.base-url points to. */
    private static HttpServer startStub() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8089), 0);
        server.createContext("/holidays/", ex -> {
            String year = ex.getRequestURI().getPath().substring("/holidays/".length());
            byte[] body;
            int status = 200;
            switch (year) {
                case "1999" -> { status = 503; body = "{\"error\":\"maintenance\"}".getBytes(StandardCharsets.UTF_8); }
                case "2030" -> { sleep(3000); body = "[]".getBytes(StandardCharsets.UTF_8); }       // too slow
                default -> body = ("[{\"date\":\"" + year + "-01-26\",\"name\":\"Republic Day\"},"
                        + "{\"date\":\"" + year + "-08-15\",\"name\":\"Independence Day\"}]").getBytes(StandardCharsets.UTF_8);
            }
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, body.length);
            ex.getResponseBody().write(body);
            ex.close();
        });
        server.start();
        return server;
    }

    private static void section(String title) {
        System.out.println("\n=== " + title);
    }

    private static long ms(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
