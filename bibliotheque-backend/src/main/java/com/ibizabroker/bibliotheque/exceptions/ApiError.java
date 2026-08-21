package com.ibizabroker.bibliotheque.exceptions;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Corps JSON uniforme renvoyé par GlobalExceptionHandler pour toute erreur
 * fonctionnelle (404, 409, 400...). validationErrors reste null sauf pour les
 * erreurs de validation de DTO (une entrée par champ en échec).
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}