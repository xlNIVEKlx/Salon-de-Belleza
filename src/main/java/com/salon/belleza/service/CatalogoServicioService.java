package com.salon.belleza.service;

import com.salon.belleza.dto.request.CatalogoServicioRequest;
import com.salon.belleza.dto.response.CatalogoServicioResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.CatalogoServicio;
import com.salon.belleza.repository.CatalogoServicioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service para gestión del catálogo de servicios del salón.
 * Solo ADMIN puede crear/editar/desactivar servicios.
 * Las trabajadoras pueden listar los activos (para la pantalla POS).
 */
@Service
@RequiredArgsConstructor
public class CatalogoServicioService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoServicioService.class);
    private static final BigDecimal COMISION_DEFAULT = new BigDecimal("50.00");

    private final CatalogoServicioRepository catalogoServicioRepository;

    // ── Mapeo ────────────────────────────────────────────────────────────────

    public CatalogoServicioResponse toResponse(CatalogoServicio s) {
        return new CatalogoServicioResponse(
                s.getId(),
                s.getNombre(),
                s.getPrecio(),
                s.getPorcentajeComision(),
                s.getActivo()
        );
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /** Lista todos los servicios activos ordenados por nombre — para la pantalla POS. */
    @Transactional(readOnly = true)
    public List<CatalogoServicioResponse> listarActivos() {
        return catalogoServicioRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoServicioResponse> listarTodos() {
        return catalogoServicioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogoServicioResponse buscarPorId(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    @Transactional
    public CatalogoServicioResponse crear(CatalogoServicioRequest request) {
        if (catalogoServicioRepository.existsByNombreIgnoreCaseAndActivoTrue(request.getNombre())) {
            throw new BusinessException(
                    "Ya existe un servicio activo con el nombre '" + request.getNombre() + "'");
        }

        // Si no se envía comisión, aplicar el valor por defecto
        BigDecimal comision = request.getPorcentajeComision() != null
                ? request.getPorcentajeComision()
                : COMISION_DEFAULT;

        CatalogoServicio servicio = new CatalogoServicio(
                request.getNombre(),
                request.getPrecio(),
                comision
        );

        CatalogoServicio guardado = catalogoServicioRepository.save(servicio);
        log.info("Servicio creado: id={}, nombre='{}', precio={}",
                guardado.getId(), guardado.getNombre(), guardado.getPrecio());
        return toResponse(guardado);
    }

    @Transactional
    public CatalogoServicioResponse actualizar(Long id, CatalogoServicioRequest request) {
        CatalogoServicio servicio = findOrThrow(id);

        // Validar nombre único excluyendo el propio registro
        catalogoServicioRepository.findByNombreIgnoreCaseAndActivoTrue(request.getNombre())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BusinessException(
                                "Ya existe un servicio con el nombre '" + request.getNombre() + "'");
                    }
                });

        servicio.setNombre(request.getNombre());
        servicio.setPrecio(request.getPrecio());
        if (request.getPorcentajeComision() != null) {
            servicio.setPorcentajeComision(request.getPorcentajeComision());
        }

        log.info("Servicio actualizado: id={}", id);
        return toResponse(catalogoServicioRepository.save(servicio));
    }

    @Transactional
    public void desactivar(Long id) {
        CatalogoServicio servicio = findOrThrow(id);
        servicio.setActivo(false);
        catalogoServicioRepository.save(servicio);
        log.info("Servicio desactivado: id={}", id);
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    CatalogoServicio findOrThrow(Long id) {
        return catalogoServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio del catálogo", "id", id));
    }
}
