package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Envoltorio genérico para respuestas de error de la API.
 * Devuelto por el GlobalExceptionHandler en todos los casos de error.
 *
 * Permite que el frontend maneje errores de forma uniforme
 * sin depender de los mensajes por defecto de Spring Boot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /** Código HTTP del error (400, 404, 409, 500, etc.) */
    private int status;
    /** Mensaje de error legible para el usuario */
    private String mensaje;
    /** Timestamp en epoch millis para diagnóstico */
    private long timestamp;

    public ErrorResponse(int status, String mensaje) {
        this.status = status;
        this.mensaje = mensaje;
        this.timestamp = System.currentTimeMillis();
    }
}
