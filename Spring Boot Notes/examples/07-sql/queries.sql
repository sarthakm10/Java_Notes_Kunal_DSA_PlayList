-- File 07, Complete Example: a "team dashboard" report using most of the file's ideas.
-- Run after schema.sql:  psql -h localhost -U springnotes -d springnotes -f queries.sql
SET search_path TO sqlnotes;

\echo '== 1. Project progress (LEFT JOIN + GROUP BY + FILTER + CASE) =='
SELECT p.name                                         AS project,
       count(t.id)                                    AS tasks,
       count(t.id) FILTER (WHERE t.done)              AS done,
       coalesce(sum(t.points), 0)                     AS points,
       CASE WHEN count(t.id) = 0 THEN '-'
            ELSE round(100.0 * count(t.id) FILTER (WHERE t.done) / count(t.id)) || '%'
       END                                            AS progress
FROM projects p
LEFT JOIN tasks t ON t.project_id = p.id
GROUP BY p.id, p.name
ORDER BY p.id;

\echo '== 2. Open workload per person, busiest first (JOIN + HAVING + RANK) =='
SELECT u.username,
       count(*)                                  AS open_tasks,
       sum(t.points)                             AS open_points,
       rank() OVER (ORDER BY sum(t.points) DESC) AS busiest_rank
FROM users u
JOIN tasks t ON t.assignee_id = u.id AND NOT t.done
GROUP BY u.id, u.username
HAVING count(*) >= 1
ORDER BY busiest_rank, u.username;

\echo '== 3. The most urgent open task of each project (CTE + ROW_NUMBER, top-1 per group) =='
WITH ranked AS (
    SELECT t.*,
           row_number() OVER (PARTITION BY t.project_id
                              ORDER BY t.due_date NULLS LAST, t.points DESC) AS rn
    FROM tasks t
    WHERE NOT t.done
)
SELECT p.name AS project, r.title, r.due_date
FROM ranked r
JOIN projects p ON p.id = r.project_id
WHERE r.rn = 1
ORDER BY p.name;

\echo '== 4. Reporting chain under asha (recursive CTE) =='
WITH RECURSIVE chain AS (
    SELECT id, username, manager_id, 0 AS depth, ARRAY[username::text] AS path
    FROM users WHERE username = 'asha'
    UNION ALL
    SELECT u.id, u.username, u.manager_id, c.depth + 1, c.path || u.username::text
    FROM users u
    JOIN chain c ON u.manager_id = c.id
)
SELECT repeat('    ', depth) || username AS org_chart
FROM chain
ORDER BY path;                    -- sort by the path from the top: a tree

\echo '== 5. People with no open work and tags never used (anti-joins) =='
SELECT 'idle user' AS kind, u.username AS name
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM tasks t WHERE t.assignee_id = u.id AND NOT t.done)
UNION ALL
SELECT 'unused tag', g.name
FROM tags g
LEFT JOIN task_tags tt ON tt.tag_id = g.id
WHERE tt.tag_id IS NULL
ORDER BY kind, name;
