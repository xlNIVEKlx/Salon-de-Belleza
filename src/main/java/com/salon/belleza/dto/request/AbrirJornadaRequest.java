package com.salon.belleza.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para abrir una nueva jornada.
 * Si el usuario ya tiene una jornada abierta en otra sede,
 * el Service la cerrará automáticamente antes de abrir esta.
 */
@Getter
@Setter
@NoArgsConstructor
public class AbrirJornadaRequest {

    @NotNull(message = "La sede es obligatoria para abrir una jornada")
    private Long sedeId;
}
