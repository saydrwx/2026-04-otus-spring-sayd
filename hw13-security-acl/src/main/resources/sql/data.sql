insert into authors(full_name)
values ('Author_1'), ('Author_2'), ('Author_3');

insert into genres(name)
values ('Genre_1'), ('Genre_2'), ('Genre_3'),
       ('Genre_4'), ('Genre_5'), ('Genre_6');

insert into books(title, author_id)
values ('BookTitle_1', 1), ('BookTitle_2', 2), ('BookTitle_3', 3);

insert into books_genres(book_id, genre_id)
values (1, 1),   (1, 2),
       (2, 3),   (2, 4),
       (3, 5),   (3, 6);

insert into comments(text, book_id)
values ('BookTitle_1 comment', 1), ('BookTitle_2 comment', 2), ('BookTitle_3 comment', 3);

insert into roles(id, name)
values (1, 'USER'), (2, 'ADMIN');

insert into users(id, user_name, password)
values (1, 'admin', '$2a$12$tYstTcf9gsAbjSO6Md4uw.C3V4ZGQz51GORCIFg6L6JfrlaUj5KWm'),
       (2, 'user', '$2a$12$HQ65sMTzF58ZoRh5bC7cEOJHMaVMVW9s0dQsOOGlIUJN.IZfUDg4e');

insert into users_roles(user_id, role_id)
values (1, 2), (2, 1);

insert into acl_sid (id, principal, sid)
values (1, 1, 'admin'),
       (2, 1, 'user'),
       (3, 0, 'ROLE_ADMIN'),
       (4, 0, 'ROLE_USER');

insert into acl_class (id, class)
values (1, 'ru.otus.hw.model.Author'),
       (2, 'ru.otus.hw.model.Book'),
       (3, 'ru.otus.hw.model.Comment'),
       (4, 'ru.otus.hw.model.Genre');

insert into acl_object_identity (id, object_id_class, object_id_identity, parent_object, owner_sid, entries_inheriting)
values (1, 1, 1, null, 1, 0),
       (2, 1, 2, null, 1, 0),
       (3, 1, 3, null, 1, 0),
       (4, 2, 1, null, 1, 0),
       (5, 2, 2, null, 1, 0),
       (6, 2, 3, null, 1, 0),
       (7, 3, 1, null, 1, 0),
       (8, 3, 2, null, 1, 0),
       (9, 3, 3, null, 1, 0),
       (10, 4, 1, null, 1, 0),
       (11, 4, 2, null, 1, 0),
       (12, 4, 3, null, 1, 0),
       (13, 4, 4, null, 1, 0),
       (14, 4, 5, null, 1, 0),
       (15, 4, 6, null, 1, 0);

insert into acl_entry (acl_object_identity, ace_order, sid, mask, granting, audit_success, audit_failure)
values (1, 1, 3,1, 1, 1, 1),
       (1, 2, 3,2, 1, 1, 1),
       (1, 3, 3,4, 1, 1, 1),
       (1, 4, 3,8, 1, 1, 1),
       (1, 5, 3,16, 1, 1, 1),
       (1, 6, 4, 1, 1, 1, 1),
       (2, 1, 3, 1, 1, 1, 1),
       (2, 2, 3, 2, 1, 1, 1),
       (2, 3, 3, 4, 1, 1, 1),
       (2, 4, 3, 8, 1, 1, 1),
       (2, 5, 3, 16, 1, 1, 1),
       (2, 6, 4, 1, 1, 1, 1),
       (3, 1, 3, 1, 1, 1, 1),
       (3, 2, 3, 2, 1, 1, 1),
       (3, 3, 3, 4, 1, 1, 1),
       (3, 4, 3, 8, 1, 1, 1),
       (3, 5, 3, 16, 1, 1, 1),
       (3, 6, 4, 1, 1, 1, 1),
       (4, 1, 3, 1, 1, 1, 1),
       (4, 2, 3, 2, 1, 1, 1),
       (4, 3, 3, 4, 1, 1, 1),
       (4, 4, 3, 8, 1, 1, 1),
       (4, 5, 3, 16, 1, 1, 1),
       (4, 6, 4, 1, 1, 1, 1),
       (5, 1, 3, 1, 1, 1, 1),
       (5, 2, 3, 2, 1, 1, 1),
       (5, 3, 3, 4, 1, 1, 1),
       (5, 4, 3, 8, 1, 1, 1),
       (5, 5, 3, 16, 1, 1, 1),
       (5, 6, 4, 1, 1, 1, 1),
       (6, 1, 3, 1, 1, 1, 1),
       (6, 2, 3, 2, 1, 1, 1),
       (6, 3, 3, 4, 1, 1, 1),
       (6, 4, 3, 8, 1, 1, 1),
       (6, 5, 3, 16, 1, 1, 1),
       (6, 6, 4, 1, 1, 1, 1),
       (7, 1, 3, 1, 1, 1, 1),
       (7, 2, 3, 2, 1, 1, 1),
       (7, 3, 3, 4, 1, 1, 1),
       (7, 4, 3, 8, 1, 1, 1),
       (7, 5, 3, 16, 1, 1, 1),
       (7, 6, 4, 1, 1, 1, 1),
       (8, 1, 3, 1, 1, 1, 1),
       (8, 2, 3, 2, 1, 1, 1),
       (8, 3, 3, 4, 1, 1, 1),
       (8, 4, 3, 8, 1, 1, 1),
       (8, 5, 3, 16, 1, 1, 1),
       (8, 6, 4, 1, 1, 1, 1),
       (9, 1, 3, 1, 1, 1, 1),
       (9, 2, 3, 2, 1, 1, 1),
       (9, 3, 3, 4, 1, 1, 1),
       (9, 4, 3, 8, 1, 1, 1),
       (9, 5, 3, 16, 1, 1, 1),
       (9, 6, 4, 1, 1, 1, 1),
       (10, 1, 3, 1, 1, 1, 1),
       (10, 2, 3, 2, 1, 1, 1),
       (10, 3, 3, 4, 1, 1, 1),
       (10, 4, 3, 8, 1, 1, 1),
       (10, 5, 3, 16, 1, 1, 1),
       (10, 6, 4, 1, 1, 1, 1),
       (11, 1, 3, 1, 1, 1, 1),
       (11, 2, 3, 2, 1, 1, 1),
       (11, 3, 3, 4, 1, 1, 1),
       (11, 4, 3, 8, 1, 1, 1),
       (11, 5, 3, 16, 1, 1, 1),
       (11, 6, 4, 1, 1, 1, 1),
       (12, 1, 3, 1, 1, 1, 1),
       (12, 2, 3, 2, 1, 1, 1),
       (12, 3, 3, 4, 1, 1, 1),
       (12, 4, 3, 8, 1, 1, 1),
       (12, 5, 3, 16, 1, 1, 1),
       (12, 6, 4, 1, 1, 1, 1),
       (13, 1, 3, 1, 1, 1, 1),
       (13, 2, 3, 2, 1, 1, 1),
       (13, 3, 3, 4, 1, 1, 1),
       (13, 4, 3, 8, 1, 1, 1),
       (13, 5, 3, 16, 1, 1, 1),
       (13, 6, 4, 1, 1, 1, 1),
       (14, 1, 3, 1, 1, 1, 1),
       (14, 2, 3, 2, 1, 1, 1),
       (14, 3, 3, 4, 1, 1, 1),
       (14, 4, 3, 8, 1, 1, 1),
       (14, 5, 3, 16, 1, 1, 1),
       (14, 6, 4, 1, 1, 1, 1),
       (15, 1, 3, 1, 1, 1, 1),
       (15, 2, 3, 2, 1, 1, 1),
       (15, 3, 3, 4, 1, 1, 1),
       (15, 4, 3, 8, 1, 1, 1),
       (15, 5, 3, 16, 1, 1, 1),
       (15, 6, 4, 1, 1, 1, 1);

ALTER TABLE roles
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM roles);

ALTER TABLE users
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM users);

ALTER TABLE acl_sid
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM acl_sid);

ALTER TABLE acl_class
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM acl_class);

ALTER TABLE acl_object_identity
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM acl_object_identity);

ALTER TABLE acl_entry
    ALTER COLUMN id RESTART WITH (SELECT COALESCE(MAX(id), 0) + 1 FROM acl_entry);