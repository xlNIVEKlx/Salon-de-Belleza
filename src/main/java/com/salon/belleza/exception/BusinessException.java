package com.salon.belleza.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 * Produce un HTTP 409 Conflict, que indica que la operación
 * es semánticamente incorrecta dado el estado actual del sistema.
 *
 * Casos de uso en el salón de belleza:
 *   - Intentar abrir una jornada cuando ya existe otra ABIERTA en la misma sede
 *   - Registrar un servicio en una jornada ya CERRADA
 *   - Crear un usuario con un username que ya existe
 *   - Intentar cambiar el estado de una cita a uno no permitido
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
