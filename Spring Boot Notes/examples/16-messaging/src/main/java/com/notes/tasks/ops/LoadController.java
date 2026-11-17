package com.notes.tasks.ops;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoints that only WAIT, to compare platform threads and virtual threads under load. */
@RestController
public class LoadController {
    private final JdbcTemplate jdbc;

    public LoadController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Blocks the request thread like a slow remote call would (no database, no locks). */
    @GetMapping("/api/ops/slow")
    public Map<String, Object> slow(@RequestParam(defaultValue = "1000") long ms) throws InterruptedException {
        Thread.sleep(ms);
        return describe(Thread.currentThread());
    }

    /** Blocks inside the DATABASE instead: every request needs one of the pool's few connections. */
    @GetMapping("/api/ops/slow-db")
    public Map<String, Object> slowDb(@RequestParam(defaultValue = "1000") long ms) {
        jdbc.queryForObject("select pg_sleep(?)", Object.class, ms / 1000.0);
        return describe(Thread.currentThread());
    }

    private static Map<String, Object> describe(Thread t) {
        return Map.of("thread", t.toString(), "virtual", t.isVirtual());
    }
}
