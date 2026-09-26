package com.salon.belleza.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar un servicio del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoServicioRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "Formato de precio inválido")
    private BigDecimal precio;

    /**
     * Porcentaje de comisión para la empleada (0-100).
     * Si no se envía, el Service aplicará el valor por defecto (50.00).
     */
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar el 100%")
    @Digits(integer = 5, fraction = 2, message = "Formato de porcentaje inválido")
    private BigDecimal porcentajeComision;
}
