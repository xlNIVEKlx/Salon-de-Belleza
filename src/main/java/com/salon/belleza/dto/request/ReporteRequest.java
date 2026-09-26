package com.salon.belleza.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO para solicitar la generación de un reporte Excel.
 * Si no se especifican fechas, el Service usará el día actual.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReporteRequest {

    /**
     * Fecha de inicio del reporte (inclusive).
     * Si es null, se usa el inicio del día actual.
     */
    private LocalDate fechaInicio;

    /**
     * Fecha de fin del reporte (inclusive).
     * Si es null, se usa el fin del día actual.
     */
    private LocalDate fechaFin;

    /**
     * Filtro opcional por sede. Si es null, incluye todas las sedes.
     */
    private Long sedeId;
}
