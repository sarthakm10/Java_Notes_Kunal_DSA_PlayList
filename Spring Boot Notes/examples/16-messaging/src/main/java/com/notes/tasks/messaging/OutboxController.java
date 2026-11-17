package com.notes.tasks.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Operator view of both sides of the messaging flow (ADMIN only). */
@RestController
@RequestMapping("/api/ops/messaging")
@PreAuthorize("hasRole('ADMIN')")
public class OutboxController {
    private final JdbcTemplate jdbc;

    public OutboxController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/outbox")
    public List<Map<String, Object>> outbox() {
        return jdbc.queryForList("""
                SELECT id, msg_key, attempts, published_at IS NOT NULL AS published,
                       payload::json ->> 'title' AS title
                FROM outbox_event ORDER BY id""");
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> notifications() {
        return jdbc.queryForList("SELECT id, task_id, text FROM notification ORDER BY id");
    }

    /** Re-send a row: what an operator does after an incident, and what a relay crash does by accident. */
    @PostMapping("/outbox/{id}/replay")
    public Map<String, Object> replay(@PathVariable long id) {
        int n = jdbc.update("UPDATE outbox_event SET published_at = NULL WHERE id = ?", id);
        return Map.of("id", id, "requeued", n == 1);
    }
}
