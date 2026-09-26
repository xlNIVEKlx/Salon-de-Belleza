package com.salon.belleza.repository;

import com.salon.belleza.model.CatalogoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad CatalogoServicio.
 * Gestiona el catálogo de servicios disponibles en el salón.
 */
@Repository
public interface CatalogoServicioRepository extends JpaRepository<CatalogoServicio, Long> {

    /**
     * Obtiene solo los servicios activos para mostrar en la pantalla POS.
     */
    List<CatalogoServicio> findByActivoTrue();

    /**
     * Obtiene servicios activos ordenados por nombre (para la UI).
     */
    List<CatalogoServicio> findByActivoTrueOrderByNombreAsc();

    /**
     * Busca un servicio activo por nombre exacto (para validar duplicados).
     */
    Optional<CatalogoServicio> findByNombreIgnoreCaseAndActivoTrue(String nombre);

    /**
     * Verifica si existe un servicio activo con ese nombre.
     */
    boolean existsByNombreIgnoreCaseAndActivoTrue(String nombre);
}
