package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de respuesta para el cálculo de caja en tiempo real de una sede.
 * Devuelto por el endpoint GET /api/caja/sede/{sedeId}/hoy
 *
 * Muestra el dinero físico que debe haber en caja en este momento,
 * sumando el trabajo de todas las empleadas activas en esa sede.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CajaSedeResponse {
    private Long sedeId;
    private String sedeNombre;
    private LocalDate fecha;
    /** Número de trabajadoras con jornada abierta en este momento */
    private Integer trabajadorasActivas;
    /** Suma de totalCobrado de todas las empleadas en la sede hoy */
    private BigDecimal totalProducido;
    /** Suma de comisionEmpleada de todas las empleadas en la sede hoy */
    private BigDecimal totalComisiones;
    /** Suma de totalCajaSalon — el dinero físico en caja */
    private BigDecimal totalCajaSalon;
}
