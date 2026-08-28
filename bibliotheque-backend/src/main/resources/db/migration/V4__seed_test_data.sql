-- ============================================================================
-- V4__seed_test_data.sql
--
-- Donnees de test dediees au module de reservation (RG-01 a RG-06), en
-- complement du seed de base pose par V1 (roles, admin, 3 livres de demo).
-- V1 est deja appliquee (checksum verrouille par Flyway) : on ne la modifie
-- jamais apres coup, on ajoute ce jeu de donnees dans une migration suivante,
-- comme V2 l'a deja fait pour l'admin.
--
-- Toutes les insertions sont idempotentes (ON CONFLICT / NOT EXISTS) afin que
-- cette migration puisse etre rejouee sur une base deja seedee sans erreur.
--
-- Correspondance avec les entites JPA (identique a V1/V3, noms physiques
-- resolus par SpringPhysicalNamingStrategy) :
--   Users       -> table "users"       (userId, name, password, username)
--   Books       -> table "books"       (bookId, bookName, bookAuthor, bookGenre, noOfCopies)
--   Borrow      -> table "borrow"      (borrowId, bookId, userId, issueDate, dueDate, returnDate)
--   Reservation -> table "reservation" (reservationId, book_id, user_id,
--                  date_reservation, date_expiration, statut)
--
-- Jeu de donnees cree :
--   Adherents (username) :
--     - adherent_a1 : A1, reservateur principal (aucune reservation
--       preexistante -> libre pour scenarios RG-01/RG-02/RG-04).
--     - adherent_a2 : A2, deja au maximum de 3 reservations actives
--       (EN_ATTENTE) -> quota RG-03 (MAX_RESERVATIONS_ACTIVES = 3, voir
--       ReservationService) deja sature, pret pour un test de rejet 409.
--     - adherent_a3 : A3, emprunteur -> emprunts en cours (return_date NULL)
--       sur L2..L5, ce qui justifie que ces 4 livres soient a 0 exemplaire.
--   Mot de passe (BCrypt, force 10) identique pour les 3 comptes : "admin123"
--   en clair. On reutilise ici le hash deja verifie par V2 (BCryptPasswordEncoder
--   #matches confirme) plutot que d'en regenerer un nouveau non verifiable
--   dans ce fichier.
--
--   Livres (book_name) :
--     - L1 "Livre Test Disponible RG"     : no_of_copies = 3 (disponible ->
--       sert a demontrer le rejet RG-01 "livre disponible non reservable").
--     - L2 "Livre Test Indisponible RG 1" : no_of_copies = 0
--     - L3 "Livre Test Indisponible RG 2" : no_of_copies = 0
--     - L4 "Livre Test Indisponible RG 3" : no_of_copies = 0
--     - L5 "Livre Test Indisponible RG 4" : no_of_copies = 0
--
--   Reservations actives de A2 (statut EN_ATTENTE) sur L2, L3, L4 : exactement
--   3 reservations actives, la limite RG-03. L5 reste volontairement libre
--   pour un 4e essai de reservation par A2, qui doit alors etre rejete (409).
--
--   Emprunts en cours de A3 (return_date NULL) sur L2, L3, L4, L5.
-- ============================================================================

-- --- Adherents de test ---------------------------------------------------------
-- IDs explicites requis : la colonne user_id n'a pas de DEFAULT au niveau SQL
-- (Hibernate lit hibernate_sequence a l'insertion cote application). V1 avait
-- realigne cette sequence a 10 (setval(..., 10, false)) : 10/11/12 sont donc
-- libres a ce stade.
INSERT INTO users (user_id, name, password, username)
VALUES
    (10, 'Adherent Test A1', '$2a$10$vnYzavCdUf12loSX7Bjs3.TdGUcDAMTWmg00Katu6.DUYwWCSGhfK', 'adherent_a1'),
    (11, 'Adherent Test A2', '$2a$10$vnYzavCdUf12loSX7Bjs3.TdGUcDAMTWmg00Katu6.DUYwWCSGhfK', 'adherent_a2'),
    (12, 'Adherent Test A3', '$2a$10$vnYzavCdUf12loSX7Bjs3.TdGUcDAMTWmg00Katu6.DUYwWCSGhfK', 'adherent_a3')
ON CONFLICT (username) DO NOTHING;

-- Rattache les 3 adherents au role "User" (seede en V1). Recherche par nom
-- (username / role_name) plutot que par ID en dur, comme le fait deja V1 pour
-- l'admin : robuste meme si les ID reels different (seed rejoue partiellement).
INSERT INTO user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, role r
WHERE u.username IN ('adherent_a1', 'adherent_a2', 'adherent_a3')
  AND r.role_name = 'User'
ON CONFLICT DO NOTHING;

-- --- Livres de test (disponibilite) ---------------------------------------------
-- IDs explicites 20..24, hors de la plage deja utilisee par V1 (1..3) et par
-- les adherents ci-dessus (10..12), pour rester lisibles et sans collision.
INSERT INTO books (book_id, book_name, book_author, book_genre, no_of_copies)
VALUES
    (20, 'Livre Test Disponible RG',      'Auteur Test', 'Test', 3),
    (21, 'Livre Test Indisponible RG 1',  'Auteur Test', 'Test', 0),
    (22, 'Livre Test Indisponible RG 2',  'Auteur Test', 'Test', 0),
    (23, 'Livre Test Indisponible RG 3',  'Auteur Test', 'Test', 0),
    (24, 'Livre Test Indisponible RG 4',  'Auteur Test', 'Test', 0)
ON CONFLICT (book_name, book_author) DO NOTHING;

-- --- Realignement de la sequence partagee ---------------------------------------
-- Meme raison qu'en V1 : les ID 10..12 (users) et 20..24 (books) sont poses en
-- dur, sans passer par hibernate_sequence. Sans ce realignement, le prochain
-- INSERT fait par l'application redemanderait nextval() = 10 et entrerait en
-- collision avec les lignes ci-dessus.
SELECT setval('hibernate_sequence', 30, false);

-- --- Emprunts en cours de A3 sur L2, L3, L4, L5 ----------------------------------
-- return_date NULL = emprunt toujours en cours (justifie no_of_copies = 0 sur
-- ces 4 livres). due_date = issue_date + 7 jours, alignee sur la regle
-- appliquee par BorrowController lors d'un emprunt reel.
INSERT INTO borrow (book_id, user_id, issue_date, due_date, return_date)
SELECT b.book_id, u.user_id, now(), now() + INTERVAL '7 days', NULL
FROM books b
CROSS JOIN users u
WHERE u.username = 'adherent_a3'
  AND b.book_name IN (
      'Livre Test Indisponible RG 1',
      'Livre Test Indisponible RG 2',
      'Livre Test Indisponible RG 3',
      'Livre Test Indisponible RG 4'
  )
  AND NOT EXISTS (
      SELECT 1 FROM borrow br
      WHERE br.book_id = b.book_id
        AND br.user_id = u.user_id
        AND br.return_date IS NULL
  );

-- --- Reservations actives de A2 sur L2, L3, L4 (quota RG-03 sature) -------------
-- 3 reservations EN_ATTENTE = exactement MAX_RESERVATIONS_ACTIVES (voir
-- ReservationService). date_expiration = date_reservation + 7 jours (RG-04).
-- L5 reste sans reservation pour A2 : une tentative de reservation
-- supplementaire (sur L5 ou tout autre livre indisponible) doit etre rejetee
-- en 409 par RG-03.
INSERT INTO reservation (book_id, user_id, date_reservation, date_expiration, statut)
SELECT b.book_id, u.user_id, now(), now() + INTERVAL '7 days', 'EN_ATTENTE'
FROM books b
CROSS JOIN users u
WHERE u.username = 'adherent_a2'
  AND b.book_name IN (
      'Livre Test Indisponible RG 1',
      'Livre Test Indisponible RG 2',
      'Livre Test Indisponible RG 3'
  )
  AND NOT EXISTS (
      SELECT 1 FROM reservation r
      WHERE r.book_id = b.book_id
        AND r.user_id = u.user_id
        AND r.statut IN ('EN_ATTENTE', 'DISPONIBLE')
  );
