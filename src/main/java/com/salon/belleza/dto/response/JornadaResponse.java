package com.salon.belleza.dto.response;

import com.salon.belleza.model.EstadoJornada;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una Jornada.
 * Incluye información del usuario y sede para el dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JornadaResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long sedeId;
    private String sedeNombre;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private EstadoJornada estado;
}
