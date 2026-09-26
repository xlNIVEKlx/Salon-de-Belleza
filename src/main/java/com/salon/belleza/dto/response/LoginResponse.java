package com.salon.belleza.dto.response;

import com.salon.belleza.model.Rol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para el login exitoso.
 * Incluye los datos del usuario autenticado para inicializar el estado
 * del frontend (sede actual, rol, nombre para el header).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long usuarioId;
    private String nombre;
    private String username;
    private Rol rol;
    private Long sedePrincipalId;
    private String sedePrincipalNombre;
    /** Indica si la trabajadora tiene una jornada abierta al momento de iniciar sesión. */
    private Boolean tieneJornadaAbierta;
    private Long jornadaActivaId;
    private String sedeJornadaActivaNombre;
}
