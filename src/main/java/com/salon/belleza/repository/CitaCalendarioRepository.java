package com.salon.belleza.repository;

import com.salon.belleza.model.CitaCalendario;
import com.salon.belleza.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para la entidad CitaCalendario.
 * Soporta filtrado por usuario, sede, fecha y estado para
 * la vista del calendario en el frontend.
 */
@Repository
public interface CitaCalendarioRepository extends JpaRepository<CitaCalendario, Long> {

    /**
     * Obtiene las citas de un usuario en un rango de fechas.
     * Usado para filtrar citas propias de una trabajadora.
     */
    @Query("SELECT c FROM CitaCalendario c " +
           "WHERE c.usuario.id = :usuarioId " +
           "AND c.fechaHoraCita >= :inicio " +
           "AND c.fechaHoraCita <= :fin " +
           "ORDER BY c.fechaHoraCita ASC")
    List<CitaCalendario> findByUsuarioIdAndFechaHoraCitaBetween(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Obtiene las citas de una sede en un rango de fechas.
     * Usado por el admin para ver todas las citas de la sede.
     */
    @Query("SELECT c FROM CitaCalendario c " +
           "WHERE c.sede.id = :sedeId " +
           "AND c.fechaHoraCita >= :inicio " +
           "AND c.fechaHoraCita <= :fin " +
           "ORDER BY c.fechaHoraCita ASC")
    List<CitaCalendario> findBySedeIdAndFechaHoraCitaBetween(
            @Param("sedeId") Long sedeId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Obtiene citas de una sede filtradas por usuario y estado.
     * Filtro combinado para la vista del calendario.
     */
    @Query("SELECT c FROM CitaCalendario c " +
           "WHERE c.sede.id = :sedeId " +
           "AND c.usuario.id = :usuarioId " +
           "AND c.estado = :estado " +
           "AND c.fechaHoraCita >= :inicio " +
           "AND c.fechaHoraCita <= :fin " +
           "ORDER BY c.fechaHoraCita ASC")
    List<CitaCalendario> findBySedeUsuarioEstadoYRango(
            @Param("sedeId") Long sedeId,
            @Param("usuarioId") Long usuarioId,
            @Param("estado") EstadoCita estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Obtiene citas próximas (pendientes o confirmadas) de una trabajadora.
     * Para mostrar en el dashboard como recordatorio.
     */
    @Query("SELECT c FROM CitaCalendario c " +
           "WHERE c.usuario.id = :usuarioId " +
           "AND c.fechaHoraCita >= :ahora " +
           "AND c.estado IN (com.salon.belleza.model.EstadoCita.PENDIENTE, " +
           "                 com.salon.belleza.model.EstadoCita.CONFIRMADA) " +
           "ORDER BY c.fechaHoraCita ASC")
    List<CitaCalendario> findCitasProximasByUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") LocalDateTime ahora);

    /**
     * Cuenta citas activas (pendiente/confirmada) de un usuario para hoy.
     */
    @Query("SELECT COUNT(c) FROM CitaCalendario c " +
           "WHERE c.usuario.id = :usuarioId " +
           "AND c.fechaHoraCita >= :inicioDia " +
           "AND c.fechaHoraCita <= :finDia " +
           "AND c.estado IN (com.salon.belleza.model.EstadoCita.PENDIENTE, " +
           "                 com.salon.belleza.model.EstadoCita.CONFIRMADA)")
    Long countCitasActivasHoyByUsuario(
            @Param("usuarioId") Long usuarioId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia);
}
