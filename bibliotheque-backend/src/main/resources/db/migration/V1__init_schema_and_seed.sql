-- ============================================================================
-- V1__init_schema_and_seed.sql
--
-- Schema initial de l'application Bibliotheque + donnees de depart (seed).
-- Reprend exactement la structure jusque-la generee par Hibernate
-- (spring.jpa.hibernate.ddl-auto=update) afin que Flyway devienne la seule
-- source de verite sur le schema. A partir de cette migration, Hibernate est
-- passe en mode "validate" (voir application.properties) : il ne cree ni
-- n'altere plus aucune table.
--
-- Correspondance avec les entites JPA (noms physiques resolus par
-- SpringPhysicalNamingStrategy) :
--   Users  -> table "users"   (userId, name, password, username)
--   Role   -> table "role"    (roleId, roleName)
--   Books  -> table "books"   (bookId, bookName, bookAuthor, bookGenre, noOfCopies)
--   Borrow -> table "borrow"  (borrowId, bookId, userId, issueDate, dueDate, returnDate)
--   Users.role (@ManyToMany)  -> table de jointure "USER_ROLE" (user_id, role_id)
-- ============================================================================

-- Users et Books utilisent @GeneratedValue(strategy = GenerationType.AUTO) sans
-- @SequenceGenerator dedie : Hibernate 5 leur attribue par defaut UNE sequence
-- partagee nommee "hibernate_sequence". Role et Borrow utilisent
-- GenerationType.IDENTITY, portee par un SERIAL propre a chaque table.
CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

-- --- Table des roles ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    role_id    SERIAL PRIMARY KEY,
    role_name  VARCHAR(255)
);

-- --- Table des utilisateurs ---------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id    INTEGER PRIMARY KEY,
    name       VARCHAR(255),
    password   VARCHAR(255),
    username   VARCHAR(255)
);

-- --- Table de jointure utilisateurs <-> roles ----------------------------------
CREATE TABLE IF NOT EXISTS user_role (
    user_id INTEGER NOT NULL REFERENCES users (user_id),
    role_id INTEGER NOT NULL REFERENCES role (role_id),
    PRIMARY KEY (user_id, role_id)
);

-- --- Table des livres -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id       INTEGER PRIMARY KEY,
    book_name     VARCHAR(255),
    book_author   VARCHAR(255),
    book_genre    VARCHAR(255),
    no_of_copies  INTEGER
);

-- --- Table des emprunts -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow (
    borrow_id   SERIAL PRIMARY KEY,
    book_id     INTEGER,
    user_id     INTEGER,
    issue_date  TIMESTAMP,
    due_date    TIMESTAMP,
    return_date TIMESTAMP
);

-- --- Contraintes d'unicite necessaires pour un seed idempotent (ON CONFLICT) -----
-- (absentes du mapping JPA d'origine ; ajoutees ici uniquement pour securiser
-- les INSERT de seed, sans impact sur la validation Hibernate)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_users_username') THEN
        ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_role_role_name') THEN
        ALTER TABLE role ADD CONSTRAINT uk_role_role_name UNIQUE (role_name);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_books_name_author') THEN
        ALTER TABLE books ADD CONSTRAINT uk_books_name_author UNIQUE (book_name, book_author);
    END IF;
END $$;

-- ============================================================================
-- SEED : donnees de depart
-- ============================================================================

-- --- Roles de base -----------------------------------------------------------
INSERT INTO role (role_name)
VALUES ('Admin'), ('User')
ON CONFLICT (role_name) DO NOTHING;

-- --- Utilisateur admin par defaut ---------------------------------------------
-- username: admin / mot de passe en clair: "admin123" (haché en BCrypt ci-dessous)
INSERT INTO users (user_id, name, password, username)
VALUES (1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd0P1R2a4qB2/Wre', 'admin')
ON CONFLICT (username) DO NOTHING;

-- Rattache l'admin au role "Admin" (necessaire pour les endpoints proteges par
-- @PreAuthorize("hasRole('Admin')")). Recherche par nom plutot que par ID en
-- dur : robuste meme si role_id ne vaut pas 1 (ex. seed rejoue partiellement).
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.username = 'admin' AND r.role_name = 'Admin'
ON CONFLICT DO NOTHING;

-- --- Livres d'exemple ----------------------------------------------------------
-- NB : l'entite Books actuelle n'a pas de champ ISBN (bookName, bookAuthor,
-- bookGenre, noOfCopies uniquement) ; on seed donc ces colonnes existantes.
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies)
VALUES
    (1, 'Le Petit Prince', 'Antoine de Saint-Exupery', 'Roman',           5),
    (2, '1984',            'George Orwell',            'Science-fiction', 3),
    (3, 'Les Miserables',  'Victor Hugo',               'Classique',      4)
ON CONFLICT (book_name, book_author) DO NOTHING;

-- --- Realignement de la sequence partagee ---------------------------------------
-- Les ID 1..3 ci-dessus sont poses "en dur" (pas de DEFAULT sur ces colonnes,
-- Hibernate lit hibernate_sequence a l'insertion). Sans ce realignement, le
-- prochain INSERT fait par l'application (nouvel utilisateur ou nouveau livre)
-- redemanderait nextval() = 1/2/3 et entrerait en collision avec le seed.
SELECT setval('hibernate_sequence', 10, false);
