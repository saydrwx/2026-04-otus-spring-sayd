CREATE TABLE IF NOT EXISTS authors (
    id BIGSERIAL,
    full_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS genres (
    id BIGSERIAL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL,
    title VARCHAR(255) NOT NULL,
    author_id bigint REFERENCES authors (id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS books_genres (
    book_id bigint REFERENCES books(id) ON DELETE CASCADE,
    genre_id bigint REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL,
    text VARCHAR(1000) NOT NULL,
    book_id bigint REFERENCES books(id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL,
    user_name VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL,
    name VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS users_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS acl_sid (
    id BIGSERIAL,
    principal BOOLEAN NOT NULL,
    sid VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_uk_1 UNIQUE (sid, principal)
);

CREATE TABLE IF NOT EXISTS acl_class (
    id BIGSERIAL,
    class VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_uk_2 UNIQUE (class)
);

CREATE TABLE IF NOT EXISTS acl_object_identity (
    id BIGSERIAL,
    object_id_class BIGINT NOT NULL,
    object_id_identity BIGINT NOT NULL,
    parent_object BIGINT DEFAULT NULL,
    owner_sid BIGINT DEFAULT NULL,
    entries_inheriting BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_uk_3 UNIQUE (object_id_class,object_id_identity)
);

CREATE TABLE IF NOT EXISTS acl_entry (
    id BIGSERIAL,
    acl_object_identity BIGINT NOT NULL,
    ace_order INT NOT NULL,
    sid BIGINT NOT NULL,
    mask INT NOT NULL,
    granting BOOLEAN NOT NULL,
    audit_success BOOLEAN NOT NULL,
    audit_failure BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT unique_uk_4 UNIQUE (acl_object_identity, ace_order)
);

ALTER TABLE acl_object_identity ADD FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id);
ALTER TABLE acl_object_identity ADD FOREIGN KEY (object_id_class) REFERENCES acl_class (id);
ALTER TABLE acl_object_identity ADD FOREIGN KEY (owner_sid) REFERENCES acl_sid (id);

ALTER TABLE acl_entry ADD FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity(id);
ALTER TABLE acl_entry ADD FOREIGN KEY (sid) REFERENCES acl_sid(id);