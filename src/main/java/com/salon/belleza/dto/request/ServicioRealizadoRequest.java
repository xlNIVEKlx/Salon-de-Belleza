package com.salon.belleza.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para registrar un servicio realizado en una jornada.
 *
 * El frontend envía únicamente el ID del servicio y la cantidad.
 * El Service calcula automáticamente todos los valores financieros
 * usando los precios del catálogo vigentes en ese momento.
 */
@Getter
@Setter
@NoArgsConstructor
public class ServicioRealizadoRequest {

    @NotNull(message = "El ID del servicio es obligatorio")
    private Long servicioId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    /**
     * ID de la jornada activa. Puede omitirse si el backend
     * lo resuelve automáticamente desde el usuario autenticado.
     * Se mantiene por compatibilidad con llamadas directas a la API.
     */
    private Long jornadaId;
}
