package com.salon.belleza.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando el usuario autenticado no tiene
 * permisos para realizar la operación solicitada.
 * Produce un HTTP 403 Forbidden.
 *
 * Casos de uso:
 *   - Una TRABAJADORA intenta acceder a endpoints de ADMIN
 *   - Una trabajadora intenta ver/modificar la jornada de otra trabajadora
 *   - Una trabajadora intenta editar una cita que no le pertenece
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
