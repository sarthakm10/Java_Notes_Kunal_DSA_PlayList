package com.notes.tasks;

import com.github.benmanes.caffeine.cache.Cache;
import com.notes.tasks.jdbc.ReportDao;
import com.notes.tasks.repo.ActivityLogRepository;
import com.notes.tasks.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdvancedFeaturesTest {
    @Autowired ReportDao reports;
    @Autowired CacheManager caches;
    @Autowired ExportService export;
    @Autowired ActivityLogRepository activity;

    @Test
    void secondCallIsServedFromTheCache() {
        caches.getCache("openTasks").clear();
        Cache<?, ?> nativeCache = (Cache<?, ?>) caches.getCache("openTasks").getNativeCache();
        long hitsBefore = nativeCache.stats().hitCount();

        int first = reports.openTasksOf("meera");
        int second = reports.openTasksOf("meera");

        assertThat(second).isEqualTo(first);
        assertThat(nativeCache.stats().hitCount() - hitsBefore).isEqualTo(1);
    }

    @Test
    void auditRunsThroughTheProxyButNotOnSelfInvocation() {
        long before = activity.count();
        export.exportProject(7);
        assertThat(activity.count()).isEqualTo(before + 1);    // advised

        export.exportAll();                                     // calls this.exportProject() twice
        assertThat(activity.count()).isEqualTo(before + 1);    // not advised
    }
}
