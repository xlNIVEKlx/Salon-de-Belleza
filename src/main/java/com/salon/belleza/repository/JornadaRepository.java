package com.salon.belleza.repository;

import com.salon.belleza.model.EstadoJornada;
import com.salon.belleza.model.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Jornada.
 *
 * RESTRICCIÓN CRÍTICA DE NEGOCIO:
 * El método findJornadaAbiertaByUsuario garantiza que solo exista
 * UNA jornada ABIERTA por usuario a la vez. El Service debe consultar
 * esto antes de abrir una nueva jornada.
 */
@Repository
public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    /**
     * Busca la jornada ABIERTA actual de una trabajadora.
     * RESTRICCIÓN: solo puede existir UNA por usuario.
     * Retorna Optional.empty() si no tiene jornada activa.
     */
    Optional<Jornada> findByUsuarioIdAndEstado(Long usuarioId, EstadoJornada estado);

    /**
     * Alias semántico para la restricción de negocio principal.
     */
    default Optional<Jornada> findJornadaAbiertaByUsuario(Long usuarioId) {
        return findByUsuarioIdAndEstado(usuarioId, EstadoJornada.ABIERTA);
    }

    /**
     * Obtiene todas las jornadas abiertas de una sede (para cálculo de caja en tiempo real).
     */
    List<Jornada> findBySedeIdAndEstado(Long sedeId, EstadoJornada estado);

    /**
     * Obtiene jornadas de una sede en un rango de fechas (para reportes).
     */
    @Query("SELECT j FROM Jornada j WHERE j.sede.id = :sedeId " +
           "AND j.fechaApertura >= :inicio AND j.fechaApertura <= :fin " +
           "ORDER BY j.fechaApertura DESC")
    List<Jornada> findBySedeIdAndFechaAperturaBetween(
            @Param("sedeId") Long sedeId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Obtiene el historial de jornadas de una trabajadora ordenado por fecha.
     */
    @Query("SELECT j FROM Jornada j WHERE j.usuario.id = :usuarioId " +
           "ORDER BY j.fechaApertura DESC")
    List<Jornada> findByUsuarioIdOrderByFechaAperturaDesc(@Param("usuarioId") Long usuarioId);

    /**
     * Obtiene todas las jornadas abiertas hoy en una sede específica.
     * Usado para el cálculo de caja en tiempo real.
     */
    @Query("SELECT j FROM Jornada j " +
           "WHERE j.sede.id = :sedeId " +
           "AND j.estado = com.salon.belleza.model.EstadoJornada.ABIERTA " +
           "AND j.fechaApertura >= :inicioDia")
    List<Jornada> findJornadasAbiertasHoyEnSede(
            @Param("sedeId") Long sedeId,
            @Param("inicioDia") LocalDateTime inicioDia);
}
