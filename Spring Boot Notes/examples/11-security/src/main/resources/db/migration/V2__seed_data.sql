-- V2: starting data, so the API has something to show.

INSERT INTO app_user (username, email) VALUES
    ('asha', 'asha@example.com'), ('ravi', 'ravi@example.com'), ('meera', 'meera@example.com');

INSERT INTO user_profile (user_id, bio, github) VALUES
    (1, 'Team lead', 'asha-codes'), (2, 'Backend developer', 'ravi-dev');      -- meera has no profile

INSERT INTO project (name, owner_id) VALUES ('Task Tracker', 1), ('Website', 3);

INSERT INTO tag (name) VALUES ('backend'), ('frontend'), ('urgent');

INSERT INTO task (project_id, assignee_id, title, priority, done, due_date, created_at, created_by) VALUES
    (1, 2, 'Design the schema',      'HIGH',   TRUE,  '2026-11-01', '2026-10-20T09:00:00Z', 'asha'),
    (1, 2, 'Write REST controllers', 'HIGH',   TRUE,  '2026-11-10', '2026-10-21T09:00:00Z', 'asha'),
    (1, 3, 'Add validation',         'MEDIUM', FALSE, '2026-11-20', '2026-10-22T09:00:00Z', 'asha'),
    (2, 3, 'New landing page',       'HIGH',   FALSE, '2026-11-18', '2026-10-23T09:00:00Z', 'meera');

INSERT INTO task_tag (task_id, tag_id) VALUES (1, 1), (2, 1), (3, 1), (3, 3), (4, 2);
