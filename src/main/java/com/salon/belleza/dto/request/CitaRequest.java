package com.salon.belleza.dto.request;

import com.salon.belleza.model.EstadoCita;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO para crear o actualizar una cita en el calendario.
 */
@Getter
@Setter
@NoArgsConstructor
public class CitaRequest {

    @NotNull(message = "La sede de la cita es obligatoria")
    private Long sedeId;

    @NotBlank(message = "El nombre de la clienta es obligatorio")
    @Size(max = 150)
    private String nombreClienta;

    @Size(max = 20, message = "Teléfono demasiado largo")
    private String telefonoClienta;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    @FutureOrPresent(message = "La cita no puede ser en el pasado")
    private LocalDateTime fechaHoraCita;

    @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
    private String notas;

    /**
     * Estado de la cita. Si no se envía, se asume PENDIENTE por defecto.
     */
    private EstadoCita estado;
}
