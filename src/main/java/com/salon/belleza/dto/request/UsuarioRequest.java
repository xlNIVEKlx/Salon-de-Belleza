package com.salon.belleza.dto.request;

import com.salon.belleza.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear un nuevo Usuario (trabajadora o admin).
 * La contraseña llega en texto plano; el Service la hashea con BCrypt.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 4, message = "La contraseña debe tener al menos 4 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    @NotNull(message = "La sede principal es obligatoria")
    private Long sedePrincipalId;
}
