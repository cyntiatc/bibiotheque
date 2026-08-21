package com.ibizabroker.bibliotheque.entity;

/**
 * Corps de requete de POST /authenticate.
 * Record : desserialisation Jackson via le constructeur canonique (noms de
 * parametres conserves grace a -parameters, active par defaut par
 * spring-boot-starter-parent). Remplace l'ancienne classe dont les setters
 * (setUserName/setUserPassword) ne correspondaient pas aux getters
 * (getUsername/getPassword) : ca fonctionnait par un fallback Jackson sur les
 * champs prives, mais restait un piege silencieux (aucune erreur en cas de
 * echec de desserialisation, juste des valeurs null).
 */
public record JwtRequest(String username, String password) {
}
