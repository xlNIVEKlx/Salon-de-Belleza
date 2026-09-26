package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una sede física del salón de belleza.
 * Una sede puede tener múltiples usuarios y jornadas asociadas.
 */
@Entity
@Table(name = "sedes")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"usuarios", "jornadas", "citas"})
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "El nombre de la sede no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "La dirección no puede estar vacía")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    // Relación inversa con Usuario (sede principal)
    @OneToMany(mappedBy = "sedePrincipal", fetch = FetchType.LAZY)
    private List<Usuario> usuarios = new ArrayList<>();

    // Relación inversa con Jornada
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Jornada> jornadas = new ArrayList<>();

    // Relación inversa con CitaCalendario
    @OneToMany(mappedBy = "sede", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CitaCalendario> citas = new ArrayList<>();

    public Sede(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
    }
}
