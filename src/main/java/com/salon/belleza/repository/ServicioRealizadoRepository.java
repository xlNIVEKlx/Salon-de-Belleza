package com.salon.belleza.repository;

import com.salon.belleza.model.ServicioRealizado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad ServicioRealizado.
 * Contiene las consultas agregadas necesarias para los reportes
 * y el cálculo de caja en tiempo real.
 */
@Repository
public interface ServicioRealizadoRepository extends JpaRepository<ServicioRealizado, Long> {

    /**
     * Obtiene todos los servicios de una jornada específica.
     */
    List<ServicioRealizado> findByJornadaId(Long jornadaId);

    /**
     * CÁLCULO DE CAJA EN TIEMPO REAL:
     * Suma el total_caja_salon de todos los servicios realizados HOY
     * en una sede específica, independientemente de qué empleada los realizó.
     * Se une con Jornada para filtrar por sede.
     */
    @Query("SELECT COALESCE(SUM(sr.totalCajaSalon), 0) FROM ServicioRealizado sr " +
           "WHERE sr.jornada.sede.id = :sedeId " +
           "AND sr.fechaRegistro >= :inicioDia " +
           "AND sr.fechaRegistro <= :finDia")
    BigDecimal sumTotalCajaSalonPorSedeYFecha(
            @Param("sedeId") Long sedeId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);

    /**
     * Suma la comisión total de una empleada en su jornada actual.
     */
    @Query("SELECT COALESCE(SUM(sr.comisionEmpleada), 0) FROM ServicioRealizado sr " +
           "WHERE sr.jornada.id = :jornadaId")
    BigDecimal sumComisionByJornada(@Param("jornadaId") Long jornadaId);

    /**
     * Suma el total cobrado de una empleada en su jornada actual.
     */
    @Query("SELECT COALESCE(SUM(sr.totalCobrado), 0) FROM ServicioRealizado sr " +
           "WHERE sr.jornada.id = :jornadaId")
    BigDecimal sumTotalCobradoByJornada(@Param("jornadaId") Long jornadaId);

    /**
     * Obtiene servicios realizados en un rango de fechas para reportes Excel.
     * Carga relaciones necesarias para evitar N+1 queries.
     */
    @Query("SELECT sr FROM ServicioRealizado sr " +
           "JOIN FETCH sr.jornada j " +
           "JOIN FETCH j.usuario u " +
           "JOIN FETCH j.sede s " +
           "JOIN FETCH sr.servicio sv " +
           "WHERE sr.fechaRegistro >= :inicio AND sr.fechaRegistro <= :fin " +
           "ORDER BY s.nombre, u.nombre, sr.fechaRegistro")
    List<ServicioRealizado> findParaReporteEnRango(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Obtiene servicios de una sede en un rango de fechas (para pestañas del Excel).
     */
    @Query("SELECT sr FROM ServicioRealizado sr " +
           "JOIN FETCH sr.jornada j " +
           "JOIN FETCH j.usuario u " +
           "JOIN FETCH j.sede s " +
           "JOIN FETCH sr.servicio sv " +
           "WHERE j.sede.id = :sedeId " +
           "AND sr.fechaRegistro >= :inicio AND sr.fechaRegistro <= :fin " +
           "ORDER BY u.nombre, sr.fechaRegistro")
    List<ServicioRealizado> findParaReportePorSedeYRango(
            @Param("sedeId") Long sedeId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Resumen de servicios por tipo para el dashboard de la trabajadora.
     * Retorna nombre del servicio y cantidad total en la jornada.
     */
    @Query("SELECT sr.servicio.nombre, SUM(sr.cantidad) as totalCantidad, " +
           "SUM(sr.totalCobrado) as totalCobrado, SUM(sr.comisionEmpleada) as totalComision " +
           "FROM ServicioRealizado sr " +
           "WHERE sr.jornada.id = :jornadaId " +
           "GROUP BY sr.servicio.nombre " +
           "ORDER BY sr.servicio.nombre")
    List<Object[]> resumenPorServicioEnJornada(@Param("jornadaId") Long jornadaId);
}
