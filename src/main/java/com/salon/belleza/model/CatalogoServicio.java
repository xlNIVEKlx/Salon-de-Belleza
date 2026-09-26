package com.salon.belleza.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un servicio ofrecido por el salón.
 * El porcentaje_comision se guarda aquí como valor de referencia,
 * pero al registrar un Servicio_Realizado se copia el valor
 * vigente en ese momento para preservar el historial financiero.
 */
@Entity
@Table(name = "catalogo_servicios")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "serviciosRealizados")
public class CatalogoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "El nombre del servicio no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Precio base del servicio. Tipo BigDecimal para evitar
     * errores de precisión en cálculos monetarios.
     */
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El precio debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 enteros y 2 decimales")
    @Column(name = "precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /**
     * Porcentaje de comisión para la empleada (valor entre 0 y 100).
     * Por defecto 50.00 (50%) según requisitos del negocio.
     */
    @NotNull(message = "El porcentaje de comisión es obligatorio")
    @DecimalMin(value = "0.00", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porcentaje no puede superar el 100%")
    @Digits(integer = 5, fraction = 2, message = "El porcentaje debe tener máximo 5 enteros y 2 decimales")
    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComision = new BigDecimal("50.00");

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Relación inversa con ServicioRealizado
    @OneToMany(mappedBy = "servicio", fetch = FetchType.LAZY)
    private List<ServicioRealizado> serviciosRealizados = new ArrayList<>();

    public CatalogoServicio(String nombre, BigDecimal precio, BigDecimal porcentajeComision) {
        this.nombre = nombre;
        this.precio = precio;
        this.porcentajeComision = porcentajeComision != null
                ? porcentajeComision
                : new BigDecimal("50.00");
    }
}
