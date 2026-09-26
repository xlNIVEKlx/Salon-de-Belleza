package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para el resumen por tipo de servicio en el dashboard.
 * Muestra cuántas veces hizo cada servicio la trabajadora en su jornada
 * y los totales financieros correspondientes.
 *
 * Ejemplo: "Uñas Acrílicas — 3 realizadas — $255.000 total — $127.500 comisión"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumenServicioResponse {
    private String servicioNombre;
    private Long cantidadTotal;
    private BigDecimal totalCobrado;
    private BigDecimal totalComision;
    private BigDecimal totalCajaSalon;
}
