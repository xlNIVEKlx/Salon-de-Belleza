package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para un ServicioRealizado.
 * Expone todos los valores financieros calculados en la transacción.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicioRealizadoResponse {
    private Long id;
    private Long jornadaId;
    private Long servicioId;
    private String servicioNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal porcentajeComisionAplicado;
    private BigDecimal totalCobrado;
    private BigDecimal comisionEmpleada;
    private BigDecimal totalCajaSalon;
    private LocalDateTime fechaRegistro;
}
