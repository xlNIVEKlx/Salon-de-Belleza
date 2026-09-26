package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que registra cada servicio prestado dentro de una jornada.
 *
 * INMUTABILIDAD FINANCIERA: Los campos monetarios (totalCobrado,
 * comisionEmpleada, totalCajaSalon) y el precioUnitario se copian
 * en el momento de la transacción. Así, si el catálogo de precios
 * cambia, el historial de servicios pasados NO se ve afectado.
 *
 * El porcentajeComisionAplicado también se copia para trazabilidad.
 */
@Entity
@Table(
    name = "servicios_realizados",
    indexes = {
        @Index(name = "idx_sr_jornada", columnList = "jornada_id"),
        @Index(name = "idx_sr_fecha", columnList = "fecha_registro"),
        @Index(name = "idx_sr_servicio", columnList = "servicio_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"jornada", "servicio"})
public class ServicioRealizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "La jornada es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "jornada_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_sr_jornada")
    )
    private Jornada jornada;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "servicio_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_sr_servicio")
    )
    private CatalogoServicio servicio;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Precio unitario del servicio al momento de la transacción.
     * Se copia desde CatalogoServicio.precio para inmutabilidad histórica.
     */
    @NotNull
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Porcentaje de comisión aplicado al momento de la transacción.
     * Se copia desde CatalogoServicio.porcentajeComision para inmutabilidad histórica.
     */
    @NotNull
    @Column(name = "porcentaje_comision_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionAplicado;

    /**
     * Total cobrado: precioUnitario × cantidad.
     * Calculado y guardado en la transacción.
     */
    @NotNull
    @Column(name = "total_cobrado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCobrado;

    /**
     * Comisión que corresponde a la empleada: totalCobrado × (porcentajeComision / 100).
     * Calculado y guardado en la transacción.
     */
    @NotNull
    @Column(name = "comision_empleada", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionEmpleada;

    /**
     * Lo que queda para la caja del salón: totalCobrado - comisionEmpleada.
     * Calculado y guardado en la transacción.
     */
    @NotNull
    @Column(name = "total_caja_salon", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCajaSalon;

    @NotNull
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onPersist() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

    /**
     * Constructor de negocio: calcula automáticamente todos los valores financieros
     * a partir del servicio y la cantidad. Garantiza la inmutabilidad histórica
     * copiando los valores vigentes del catálogo en el momento de creación.
     *
     * @param jornada   La jornada activa a la que pertenece este servicio
     * @param servicio  El servicio del catálogo (se copian precio y comisión)
     * @param cantidad  Número de veces que se realizó el servicio
     */
    public ServicioRealizado(Jornada jornada, CatalogoServicio servicio, Integer cantidad) {
        this.jornada = jornada;
        this.servicio = servicio;
        this.cantidad = cantidad;

        // Copiar valores del catálogo al momento de la transacción
        this.precioUnitario = servicio.getPrecio();
        this.porcentajeComisionAplicado = servicio.getPorcentajeComision();

        // Cálculo de valores financieros
        this.totalCobrado = this.precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        this.comisionEmpleada = this.totalCobrado
                .multiply(this.porcentajeComisionAplicado)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        this.totalCajaSalon = this.totalCobrado.subtract(this.comisionEmpleada);

        this.fechaRegistro = LocalDateTime.now();
    }
}
