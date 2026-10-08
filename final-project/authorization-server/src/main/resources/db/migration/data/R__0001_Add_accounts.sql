INSERT INTO accounts (id, email, password, role)
VALUES ('00000000-0000-0000-0000-000000000001', 'admin@example.com', '{bcrypt}$2a$12$r0mKL96s59O6VEcvEg.QZuqWRf6FvhL55iqZ2p1xRYzJURCLIjkZS', 'ADMIN'),
       ('00000000-0000-0000-0000-000000000002', 'ivanov@yandex.ru', '{bcrypt}$2a$12$r0mKL96s59O6VEcvEg.QZuqWRf6FvhL55iqZ2p1xRYzJURCLIjkZS', 'DOCTOR'), --'{bcrypt}$2b$12$AipaiyAdeuSszHd6cGFs4ebUVSG4NPyZMYz0YrPzc91s0C6zY3Zpi'
       ('00000000-0000-0000-0000-000000000003', 'petrov@gmail.com', '{bcrypt}$2a$12$r0mKL96s59O6VEcvEg.QZuqWRf6FvhL55iqZ2p1xRYzJURCLIjkZS', 'DOCTOR'),
       ('00000000-0000-0000-0000-000000000004', 'patient@yandex.ru', '{bcrypt}$2a$12$r0mKL96s59O6VEcvEg.QZuqWRf6FvhL55iqZ2p1xRYzJURCLIjkZS', 'PATIENT')
ON CONFLICT (id) DO NOTHING;
