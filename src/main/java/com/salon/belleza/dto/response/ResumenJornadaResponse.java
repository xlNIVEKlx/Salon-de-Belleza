package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para el resumen financiero completo de la jornada.
 * Es el "Resumen de Cierre de Caja" que ve la trabajadora al final del turno.
 *
 * Contiene:
 * - El desglose por tipo de servicio (lista de ResumenServicioResponse)
 * - Los totales generales del día
 * - Los datos de la sede y trabajadora para el encabezado
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenJornadaResponse {

    /** Datos de la jornada */
    private Long jornadaId;
    private String trabajadoraNombre;
    private String sedeNombre;
    private LocalDate fecha;

    /** Desglose por tipo de servicio */
    private List<ResumenServicioResponse> servicios;

    /** Totales generales de la jornada */
    private BigDecimal granTotalCobrado;
    private BigDecimal granTotalComision;
    private BigDecimal granTotalCajaSalon;
}
