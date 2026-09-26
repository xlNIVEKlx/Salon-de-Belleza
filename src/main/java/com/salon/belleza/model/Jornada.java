package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un turno de trabajo de una empleada en una sede.
 *
 * RESTRICCIÓN CRÍTICA: Una trabajadora solo puede tener UNA jornada ABIERTA
 * a la vez. Si cambia de sede, el sistema debe:
 *   1. Cerrar automáticamente la jornada actual (congelando los valores).
 *   2. Abrir una nueva jornada en la nueva sede.
 *
 * La restricción de unicidad se implementa a nivel de base de datos
 * mediante un índice parcial en PostgreSQL.
 */
@Entity
@Table(
    name = "jornadas",
    indexes = {
        @Index(name = "idx_jornada_usuario_estado", columnList = "usuario_id, estado"),
        @Index(name = "idx_jornada_sede_fecha", columnList = "sede_id, fecha_apertura")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"usuario", "sede", "serviciosRealizados"})
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "usuario_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_jornada_usuario")
    )
    private Usuario usuario;

    @NotNull(message = "La sede es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "sede_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_jornada_sede")
    )
    private Sede sede;

    @NotNull(message = "La fecha de apertura es obligatoria")
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    /**
     * Fecha de cierre: null si la jornada está ABIERTA,
     * se asigna automáticamente al cerrarla.
     */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoJornada estado = EstadoJornada.ABIERTA;

    // Relación con servicios realizados durante esta jornada
    @OneToMany(mappedBy = "jornada", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ServicioRealizado> serviciosRealizados = new ArrayList<>();

    public Jornada(Usuario usuario, Sede sede) {
        this.usuario = usuario;
        this.sede = sede;
        this.fechaApertura = LocalDateTime.now();
        this.estado = EstadoJornada.ABIERTA;
    }

    /**
     * Cierra la jornada asignando la fecha de cierre y cambiando el estado.
     * Los valores financieros de los servicios ya están guardados y no cambian.
     */
    public void cerrar() {
        this.fechaCierre = LocalDateTime.now();
        this.estado = EstadoJornada.CERRADA;
    }
}
