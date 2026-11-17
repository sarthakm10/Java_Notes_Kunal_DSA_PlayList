-- V4: login credentials and a role for each user.
-- Passwords are stored as BCrypt hashes only, with the {bcrypt} prefix that DelegatingPasswordEncoder needs.
-- The plain values are throwaway practice passwords for this local example: asha / asha-secret (ADMIN), ravi / ravi-secret, meera / meera-secret.

ALTER TABLE app_user
    ADD COLUMN password_hash VARCHAR(100),
    ADD COLUMN role          VARCHAR(10) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN'));

UPDATE app_user SET role = 'ADMIN',
       password_hash = '{bcrypt}$2a$10$gvDTKNbZdV6x7T3cH0QtGODLrpDUk.v7BqoUAQ5PJu5rH0f2ERu2i' WHERE username = 'asha';
UPDATE app_user SET password_hash = '{bcrypt}$2a$10$uuFYiHY.928g53eX15PHwuRz5S/zBkDRAUl1K077DOFTvPIr55gdm' WHERE username = 'ravi';
UPDATE app_user SET password_hash = '{bcrypt}$2a$10$Mvgx72DEgVlfeJtbSyZ36uCwJ4dxZm.nQI1/uQzGFQXhfc3OcLx7a' WHERE username = 'meera';

ALTER TABLE app_user ALTER COLUMN password_hash SET NOT NULL;
