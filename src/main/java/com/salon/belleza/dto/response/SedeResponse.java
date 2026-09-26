package com.salon.belleza.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para Sede.
 * Solo expone los campos necesarios para el frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SedeResponse {
    private Long id;
    private String nombre;
    private String direccion;
    private Boolean activa;
}
