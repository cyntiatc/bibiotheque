-- ============================================================================
-- V2__fix_admin_password_hash.sql
--
-- Correctif : le hash BCrypt seed en V1 pour l'utilisateur "admin" ne
-- correspondait PAS au mot de passe en clair "admin123"
-- (verifie avec BCryptPasswordEncoder#matches -> false). V1 est deja
-- appliquee (checksum verrouille par Flyway) : on ne la modifie jamais apres
-- coup, on corrige via une migration suivante.
--
-- Nouveau hash regenere avec org.springframework.security.crypto.bcrypt.
-- BCryptPasswordEncoder (force 10, defaut Spring Security) pour "admin123".
-- ============================================================================
UPDATE users
SET password = '$2a$10$vnYzavCdUf12loSX7Bjs3.TdGUcDAMTWmg00Katu6.DUYwWCSGhfK'
WHERE username = 'admin';
