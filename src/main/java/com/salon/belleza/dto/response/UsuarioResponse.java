package com.salon.belleza.dto.response;

import com.salon.belleza.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para Usuario.
 * NUNCA incluye passwordHash — seguridad ante todo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String username;
    private Rol rol;
    private Long sedePrincipalId;
    private String sedePrincipalNombre;
    private Boolean activo;
}
