package com.notes.tasks.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Plain SQL with Spring JDBC, no JPA: best for reports, bulk updates and queries that don't map to one entity.
 * Both beans are auto-configured by Boot from the DataSource.
 */
@Repository
public class ReportDao {
    private final JdbcTemplate jdbcTemplate;      // the classic API (since Spring 1)
    private final JdbcClient jdbcClient;          // the fluent API (since Spring 6.1)

    public ReportDao(JdbcTemplate jdbcTemplate, JdbcClient jdbcClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcClient = jdbcClient;
    }

    public record ProjectSummary(long id, String name, String owner, int tasks, int done) {}

    private static final RowMapper<ProjectSummary> SUMMARY_MAPPER = (rs, rowNum) -> new ProjectSummary(
            rs.getLong("id"), rs.getString("name"), rs.getString("owner"),
            rs.getInt("tasks"), rs.getInt("done"));

    /** JdbcTemplate: SQL + RowMapper (one call per row) -> List. */
    public List<ProjectSummary> projectSummaries() {
        return jdbcTemplate.query("""
                SELECT p.id, p.name, u.username AS owner,
                       count(t.id) AS tasks,
                       count(t.id) FILTER (WHERE t.done) AS done
                FROM project p
                JOIN app_user u ON u.id = p.owner_id
                LEFT JOIN task t ON t.project_id = p.id
                GROUP BY p.id, p.name, u.username
                ORDER BY p.id
                """, SUMMARY_MAPPER);
    }

    /** JdbcTemplate: a single value, with a ? parameter (a prepared statement: no SQL injection). */
    public int openTasksOf(String username) {
        Integer n = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM task t JOIN app_user u ON u.id = t.assignee_id
                WHERE u.username = ? AND NOT t.done
                """, Integer.class, username);
        return n == null ? 0 : n;
    }

    /** JdbcClient: named parameters and a record mapped automatically from column names. */
    public record TagUsage(String name, long uses) {}

    public List<TagUsage> tagUsage(int minUses) {
        return jdbcClient.sql("""
                        SELECT g.name, count(tt.task_id) AS uses
                        FROM tag g LEFT JOIN task_tag tt ON tt.tag_id = g.id
                        GROUP BY g.name HAVING count(tt.task_id) >= :min
                        ORDER BY uses DESC, g.name
                        """)
                .param("min", minUses)
                .query(TagUsage.class)
                .list();
    }

    /** JdbcTemplate update + KeyHolder: insert and get the generated id back. */
    public long insertTag(String name) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement("INSERT INTO tag (name) VALUES (?)", new String[] {"id"});
            ps.setString(1, name);
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
