package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para un servicio del catálogo.
 * Usado en la pantalla POS para mostrar los botones de servicios.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoServicioResponse {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private BigDecimal porcentajeComision;
    private Boolean activo;
}
