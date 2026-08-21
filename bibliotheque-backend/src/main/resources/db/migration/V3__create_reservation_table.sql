-- ============================================================================
-- V3__create_reservation_table.sql
--
-- Schema de la table "reservation" (module de gestion des reservations).
-- spring.jpa.hibernate.ddl-auto=validate : comme pour V1, cette migration est
-- la seule source de verite sur le schema, Hibernate se contente de verifier
-- que l'entite correspond.
--
-- Correspondance avec l'entite JPA (noms physiques resolus par
-- SpringPhysicalNamingStrategy) :
--   Reservation -> table "reservation" (reservationId -> reservation_id,
--                  dateReservation -> date_reservation,
--                  dateExpiration -> date_expiration, statut)
--   Reservation.livre    (@ManyToOne -> Books) -> colonne "book_id" (FK explicite)
--   Reservation.adherent (@ManyToOne -> Users) -> colonne "user_id" (FK explicite)
-- ============================================================================

CREATE TABLE IF NOT EXISTS reservation (
    reservation_id    SERIAL PRIMARY KEY,
    book_id            INTEGER NOT NULL REFERENCES books (book_id),
    user_id            INTEGER NOT NULL REFERENCES users (user_id),
    date_reservation   TIMESTAMP NOT NULL,
    date_expiration    TIMESTAMP NOT NULL,
    statut             VARCHAR(20) NOT NULL
);

-- Acceleration du GET /api/reservations (filtres par statut / par adherent)
-- et des verifications RG-02/RG-03 (reservations actives d'un adherent).
CREATE INDEX IF NOT EXISTS idx_reservation_statut ON reservation (statut);
CREATE INDEX IF NOT EXISTS idx_reservation_user_id ON reservation (user_id);