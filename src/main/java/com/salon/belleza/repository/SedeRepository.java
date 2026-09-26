package com.salon.belleza.repository;

import com.salon.belleza.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Sede.
 * Extiende JpaRepository para operaciones CRUD básicas.
 */
@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {

    /**
     * Obtiene solo las sedes activas para mostrar en la UI.
     */
    List<Sede> findByActivaTrue();

    /**
     * Busca una sede por nombre (útil para validaciones de duplicados).
     */
    Optional<Sede> findByNombreIgnoreCase(String nombre);

    /**
     * Busca sedes activas que contengan el texto en nombre o dirección.
     */
    @Query("SELECT s FROM Sede s WHERE s.activa = true AND " +
           "(LOWER(s.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(s.direccion) LIKE LOWER(CONCAT('%', :texto, '%')))")
    List<Sede> buscarActivasPorTexto(String texto);
}
