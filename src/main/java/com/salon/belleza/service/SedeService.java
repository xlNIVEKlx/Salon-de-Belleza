package com.salon.belleza.service;

import com.salon.belleza.dto.request.SedeRequest;
import com.salon.belleza.dto.response.SedeResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.Sede;
import com.salon.belleza.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service para la gestión de Sedes.
 * Solo accesible por ADMIN.
 */
@Service
@RequiredArgsConstructor
public class SedeService {

    private static final Logger log = LoggerFactory.getLogger(SedeService.class);

    private final SedeRepository sedeRepository;

    // ── Mapeo entidad → DTO ──────────────────────────────────────────────────

    public SedeResponse toResponse(Sede sede) {
        return new SedeResponse(
                sede.getId(),
                sede.getNombre(),
                sede.getDireccion(),
                sede.getActiva()
        );
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SedeResponse> listarActivas() {
        return sedeRepository.findByActivaTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SedeResponse> listarTodas() {
        return sedeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SedeResponse buscarPorId(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    @Transactional
    public SedeResponse crear(SedeRequest request) {
        validarNombreUnico(request.getNombre(), null);

        Sede sede = new Sede(request.getNombre(), request.getDireccion());
        Sede guardada = sedeRepository.save(sede);
        log.info("Sede creada: id={}, nombre='{}'", guardada.getId(), guardada.getNombre());
        return toResponse(guardada);
    }

    @Transactional
    public SedeResponse actualizar(Long id, SedeRequest request) {
        Sede sede = findOrThrow(id);
        validarNombreUnico(request.getNombre(), id);

        sede.setNombre(request.getNombre());
        sede.setDireccion(request.getDireccion());
        log.info("Sede actualizada: id={}", id);
        return toResponse(sedeRepository.save(sede));
    }

    @Transactional
    public void desactivar(Long id) {
        Sede sede = findOrThrow(id);
        sede.setActiva(false);
        sedeRepository.save(sede);
        log.info("Sede desactivada: id={}", id);
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    /**
     * Busca una sede por ID o lanza ResourceNotFoundException.
     * Método package-visible para que otros Services puedan usarlo.
     */
    Sede findOrThrow(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", id));
    }

    private void validarNombreUnico(String nombre, Long excludeId) {
        sedeRepository.findByNombreIgnoreCase(nombre).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new BusinessException(
                        "Ya existe una sede con el nombre '" + nombre + "'");
            }
        });
    }
}
