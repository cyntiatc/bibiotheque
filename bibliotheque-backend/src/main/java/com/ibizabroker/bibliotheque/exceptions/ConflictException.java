package com.ibizabroker.bibliotheque.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Levee quand une regle de gestion (RG) est violee. Interceptee par Spring
 * (via @ResponseStatus, meme mecanisme que NotFoundException) pour renvoyer
 * un HTTP 409 Conflict.
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}