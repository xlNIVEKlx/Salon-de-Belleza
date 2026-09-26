package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Entidad que representa una cita agendada en el calendario del salón.
 * Puede ser registrada por cualquier trabajadora y filtrada por usuario/sede.
 */
@Entity
@Table(
    name = "citas_calendario",
    indexes = {
        @Index(name = "idx_cita_usuario", columnList = "usuario_id"),
        @Index(name = "idx_cita_sede", columnList = "sede_id"),
        @Index(name = "idx_cita_fecha", columnList = "fecha_hora_cita"),
        @Index(name = "idx_cita_estado", columnList = "estado")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"usuario", "sede"})
public class CitaCalendario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "El usuario que agenda la cita es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_cita_usuario")
    )
    private Usuario usuario;

    @NotNull(message = "La sede de la cita es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sede_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_cita_sede")
    )
    private Sede sede;

    @NotBlank(message = "El nombre de la clienta es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(name = "nombre_clienta", nullable = false, length = 150)
    private String nombreClienta;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(name = "telefono_clienta", length = 20)
    private String telefonoClienta;

    @NotNull(message = "La fecha y hora de la cita son obligatorias")
    @Column(name = "fecha_hora_cita", nullable = false)
    private LocalDateTime fechaHoraCita;

    @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
    @Column(name = "notas", length = 500)
    private String notas;

    @NotNull(message = "El estado de la cita es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public CitaCalendario(Usuario usuario, Sede sede, String nombreClienta,
                          String telefonoClienta, LocalDateTime fechaHoraCita, String notas) {
        this.usuario = usuario;
        this.sede = sede;
        this.nombreClienta = nombreClienta;
        this.telefonoClienta = telefonoClienta;
        this.fechaHoraCita = fechaHoraCita;
        this.notas = notas;
        this.estado = EstadoCita.PENDIENTE;
    }
}
