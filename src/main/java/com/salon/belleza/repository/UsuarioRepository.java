package com.salon.belleza.repository;

import com.salon.belleza.model.Rol;
import com.salon.belleza.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 * Incluye consultas necesarias para autenticación y filtrado por sede/rol.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su username (usado en autenticación Spring Security).
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica si existe un usuario con ese username (para validación al crear).
     */
    boolean existsByUsername(String username);

    /**
     * Obtiene todos los usuarios activos de una sede principal.
     */
    List<Usuario> findBySedePrincipalIdAndActivoTrue(Long sedeId);

    /**
     * Obtiene todas las trabajadoras activas (para reportes y asignaciones).
     */
    List<Usuario> findByRolAndActivoTrue(Rol rol);

    /**
     * Obtiene usuarios activos de una sede con un rol específico.
     */
    List<Usuario> findBySedePrincipalIdAndRolAndActivoTrue(Long sedeId, Rol rol);

    /**
     * Busca trabajadoras que tengan una jornada abierta en una sede específica hoy.
     * Útil para el cálculo de caja en tiempo real.
     */
    @Query("SELECT DISTINCT j.usuario FROM Jornada j " +
           "WHERE j.sede.id = :sedeId " +
           "AND j.estado = com.salon.belleza.model.EstadoJornada.ABIERTA")
    List<Usuario> findTrabajadorasActivasEnSede(@Param("sedeId") Long sedeId);
}
