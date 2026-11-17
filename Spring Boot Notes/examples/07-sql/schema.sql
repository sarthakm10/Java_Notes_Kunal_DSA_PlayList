-- File 07: SQL essentials. Creates the "sqlnotes" schema with sample data.
-- Run:  psql -h localhost -U springnotes -d springnotes -f schema.sql
-- Safe to re-run: it drops and recreates only the sqlnotes schema.

DROP SCHEMA IF EXISTS sqlnotes CASCADE;
CREATE SCHEMA sqlnotes;
SET search_path TO sqlnotes;

CREATE TABLE users (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    VARCHAR(30)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    manager_id  BIGINT REFERENCES users (id),          -- self-reference: who you report to
    salary      NUMERIC(10, 2) NOT NULL CHECK (salary > 0),
    joined_on   DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE projects (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(60) NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL REFERENCES users (id)
);

CREATE TABLE tasks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id  BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    assignee_id BIGINT REFERENCES users (id) ON DELETE SET NULL,   -- may be unassigned
    title       VARCHAR(100) NOT NULL,
    priority    VARCHAR(6) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    points      INT NOT NULL DEFAULT 1 CHECK (points BETWEEN 1 AND 13),
    done        BOOLEAN NOT NULL DEFAULT FALSE,
    due_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tags (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

-- many-to-many: a task has many tags, a tag is on many tasks
CREATE TABLE task_tags (
    task_id BIGINT REFERENCES tasks (id) ON DELETE CASCADE,
    tag_id  BIGINT REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, tag_id)
);

INSERT INTO users (username, email, manager_id, salary, joined_on) VALUES
    ('asha',  'asha@example.com',  NULL, 150000, '2022-01-10'),   -- 1: the boss
    ('ravi',  'ravi@example.com',  1,    95000,  '2023-03-01'),   -- 2
    ('meera', 'meera@example.com', 1,    105000, '2023-06-15'),   -- 3
    ('kabir', 'kabir@example.com', 2,    70000,  '2024-02-01'),   -- 4
    ('zoya',  'zoya@example.com',  2,    70000,  '2024-08-20'),   -- 5
    ('dev',   'dev@example.com',   3,    60000,  '2025-01-05');   -- 6: has no tasks

INSERT INTO projects (name, owner_id) VALUES
    ('Task Tracker', 1), ('Website', 3), ('Mobile App', 2), ('Archive', 1);  -- Archive has no tasks

INSERT INTO tasks (project_id, assignee_id, title, priority, points, done, due_date) VALUES
    (1, 2, 'Design the schema',        'HIGH',   5, TRUE,  '2026-11-01'),
    (1, 2, 'Write REST controllers',   'HIGH',   8, TRUE,  '2026-11-10'),
    (1, 4, 'Add validation',           'MEDIUM', 3, FALSE, '2026-11-20'),
    (1, 5, 'Write tests',              'MEDIUM', 5, FALSE, '2026-11-25'),
    (1, NULL, 'Set up CI',             'LOW',    2, FALSE, NULL),
    (2, 3, 'New landing page',         'HIGH',   8, FALSE, '2026-11-18'),
    (2, 3, 'Fix footer links',         'LOW',    1, TRUE,  '2026-10-30'),
    (2, 5, 'SEO audit',                'MEDIUM', 3, FALSE, '2026-12-05'),
    (3, 4, 'Login screen',             'HIGH',   5, FALSE, '2026-11-22'),
    (3, 4, 'Push notifications',       'MEDIUM', 8, FALSE, NULL),
    (3, 2, 'App store listing',        'LOW',    2, TRUE,  '2026-11-05');

INSERT INTO tags (name) VALUES ('backend'), ('frontend'), ('urgent'), ('devops'), ('research');  -- research: unused

INSERT INTO task_tags (task_id, tag_id) VALUES
    (1, 1), (2, 1), (3, 1), (4, 1), (5, 4), (6, 2), (6, 3), (7, 2), (9, 2), (9, 3), (10, 1), (10, 2);
