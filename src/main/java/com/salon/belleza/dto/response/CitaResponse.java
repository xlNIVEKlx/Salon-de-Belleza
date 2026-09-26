package com.salon.belleza.dto.response;

import com.salon.belleza.model.EstadoCita;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una CitaCalendario.
 * Incluye nombres resueltos de usuario y sede para mostrar en el calendario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponse {
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private Long sedeId;
    private String sedeNombre;
    private String nombreClienta;
    private String telefonoClienta;
    private LocalDateTime fechaHoraCita;
    private String notas;
    private EstadoCita estado;
    private LocalDateTime fechaCreacion;
}
