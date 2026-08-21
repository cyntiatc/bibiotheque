package com.ibizabroker.bibliotheque.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Point de centralisation de la traduction "exception métier -> réponse HTTP".
 * Objectif : plus aucune 500 pour un cas fonctionnel (identifiant inconnu,
 * règle de gestion violée, corps de requête invalide). Les exceptions
 * réellement inattendues (bugs) ne sont volontairement pas interceptées ici
 * et restent en 500 par défaut, pour ne pas masquer une vraie anomalie.
 *
 * NB : AuthenticationException (identifiants invalides sur /authenticate,
 * JWT absent/invalide) n'est pas gérée ici. Elle doit continuer à remonter
 * jusqu'à ExceptionTranslationFilter, qui délègue à JwtAuthenticationEntryPoint
 * pour produire le 401 -- l'intercepter dans ce @RestControllerAdvice
 * empêcherait ce mécanisme de s'exécuter.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Identifiant inconnu (ex: livre, adhérent, réservation, emprunt).
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // Filet de sécurité : javax.persistence.EntityNotFoundException (ex:
    // EntityManager.getReference/proxy Hibernate non initialisable) n'est levée
    // nulle part explicitement dans le code actuel, mais ne doit jamais
    // remonter en 500 si elle survient.
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // Filet de sécurité : tout Optional#get() résiduel sur un identifiant
    // inconnu (au lieu d'un #orElseThrow(NotFoundException::new)) lève une
    // NoSuchElementException -> transformée en 404 plutôt qu'en 500.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElement(NoSuchElementException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Ressource introuvable.", request);
    }

    // Règle de gestion violée (ex: RG-01 à RG-06 du module réservation, rupture
    // de stock à l'emprunt...). Le message de l'exception nomme explicitement
    // la règle enfreinte.
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // Échec de validation d'un DTO annoté @Valid (ex: @NotNull sur
    // ReservationRequestDTO) -> une entrée par champ en échec.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> erreurs = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Valeur invalide" : fe.getDefaultMessage(),
                        (premierMessage, second) -> premierMessage,
                        LinkedHashMap::new
                ));
        ApiError error = new ApiError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation échouée sur " + erreurs.size() + " champ(s).",
                request.getRequestURI(),
                erreurs
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Corps de requête illisible (JSON mal formé, type incompatible...).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Corps de requête illisible ou mal formé.", request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(status).body(error);
    }
}