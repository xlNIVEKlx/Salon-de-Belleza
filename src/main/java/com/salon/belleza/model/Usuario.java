package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a una trabajadora o administradora del salón.
 * Implementa la autenticación mediante Spring Security.
 * Una trabajadora tiene una sede principal asignada, aunque puede
 * trabajar en otras sedes abriendo jornadas distintas.
 */
@Entity
@Table(
    name = "usuarios",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_username", columnNames = "username")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"jornadas", "citas"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El username no puede estar vacío")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Sede principal donde normalmente trabaja la empleada.
     * Puede trabajar en otras sedes abriendo jornadas diferentes.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sede_principal_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_usuario_sede_principal")
    )
    private Sede sedePrincipal;

    // Relación inversa con Jornada
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Jornada> jornadas = new ArrayList<>();

    // Relación inversa con CitaCalendario
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CitaCalendario> citas = new ArrayList<>();

    public Usuario(String nombre, String username, String passwordHash, Rol rol, Sede sedePrincipal) {
        this.nombre = nombre;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.sedePrincipal = sedePrincipal;
    }
}
