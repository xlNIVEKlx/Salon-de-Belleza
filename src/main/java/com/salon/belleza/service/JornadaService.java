package com.salon.belleza.service;

import com.salon.belleza.config.SecurityUtils;
import com.salon.belleza.dto.request.AbrirJornadaRequest;
import com.salon.belleza.dto.response.JornadaResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ForbiddenException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.*;
import com.salon.belleza.repository.JornadaRepository;
import com.salon.belleza.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service para gestión de Jornadas (turnos de trabajo).
 *
 * LÓGICA CRÍTICA DE NEGOCIO:
 * ─────────────────────────────────────────────────────────────────────────────
 * 1. Un usuario solo puede tener UNA jornada ABIERTA a la vez.
 *    → validado a nivel de código (aquí) Y a nivel de BD (índice parcial PostgreSQL).
 *
 * 2. Si una trabajadora solicita abrir una jornada en una SEDE DISTINTA
 *    a la que ya tiene abierta, el sistema:
 *      a) Cierra automáticamente la jornada actual (congela sus valores financieros)
 *      b) Abre una nueva jornada en la nueva sede
 *    Este comportamiento es transparente para el usuario — solo hace clic en "cambiar sede".
 *
 * 3. Si solicita abrir jornada en la MISMA sede que ya tiene abierta → error 409.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class JornadaService {

    private static final Logger log = LoggerFactory.getLogger(JornadaService.class);

    private final JornadaRepository jornadaRepository;
    private final SedeRepository sedeRepository;
    private final SecurityUtils securityUtils;

    // ── Mapeo ────────────────────────────────────────────────────────────────

    public JornadaResponse toResponse(Jornada j) {
        return new JornadaResponse(
                j.getId(),
                j.getUsuario().getId(),
                j.getUsuario().getNombre(),
                j.getSede().getId(),
                j.getSede().getNombre(),
                j.getFechaApertura(),
                j.getFechaCierre(),
                j.getEstado()
        );
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * Obtiene la jornada actualmente abierta del usuario autenticado.
     * Devuelve empty si no tiene jornada activa.
     */
    @Transactional(readOnly = true)
    public Optional<JornadaResponse> buscarJornadaAbiertaPropia() {
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        return jornadaRepository
                .findJornadaAbiertaByUsuario(usuario.getId())
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JornadaResponse buscarPorId(Long id) {
        Jornada jornada = findOrThrow(id);
        validarPropietarioOAdmin(jornada);
        return toResponse(jornada);
    }

    @Transactional(readOnly = true)
    public List<JornadaResponse> listarHistorialPropio() {
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        return jornadaRepository
                .findByUsuarioIdOrderByFechaAperturaDesc(usuario.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JornadaResponse> listarAbiertasPorSede(Long sedeId) {
        return jornadaRepository
                .findBySedeIdAndEstado(sedeId, EstadoJornada.ABIERTA)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    /**
     * Abre una nueva jornada para el usuario autenticado en la sede indicada.
     *
     * FLUJO COMPLETO:
     * 1. Verificar si el usuario tiene una jornada ABIERTA existente.
     * 2a. Si la jornada existente es en la MISMA sede → error (ya tiene una allí).
     * 2b. Si la jornada existente es en DIFERENTE sede → cerrar automáticamente
     *     (congelando todos los ServiciosRealizados ya guardados) y continuar.
     * 3. Abrir nueva jornada en la sede solicitada.
     */
    @Transactional
    public JornadaResponse abrirJornada(AbrirJornadaRequest request) {
        Usuario usuario = securityUtils.getUsuarioAutenticado();

        Sede nuevaSede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede", "id", request.getSedeId()));

        if (!nuevaSede.getActiva()) {
            throw new BusinessException(
                    "La sede '" + nuevaSede.getNombre() + "' está desactivada.");
        }

        // Verificar si tiene jornada abierta
        Optional<Jornada> jornadaExistente = jornadaRepository
                .findJornadaAbiertaByUsuario(usuario.getId());

        if (jornadaExistente.isPresent()) {
            Jornada jornadaActual = jornadaExistente.get();

            if (jornadaActual.getSede().getId().equals(nuevaSede.getId())) {
                // Caso 2a: misma sede → error de negocio
                throw new BusinessException(
                        "Ya tienes una jornada abierta en '" + nuevaSede.getNombre() +
                        "'. Ciérrala antes de abrir una nueva.");
            }

            // Caso 2b: SEDE DIFERENTE → cierre automático
            log.info("Cierre automático de jornada id={} en sede '{}' para usuario '{}' " +
                     "(cambio a sede '{}')",
                    jornadaActual.getId(),
                    jornadaActual.getSede().getNombre(),
                    usuario.getUsername(),
                    nuevaSede.getNombre());

            jornadaActual.cerrar();
            jornadaRepository.save(jornadaActual);
        }

        // Abrir nueva jornada
        Jornada nuevaJornada = new Jornada(usuario, nuevaSede);
        Jornada guardada = jornadaRepository.save(nuevaJornada);

        log.info("Jornada abierta: id={}, usuario='{}', sede='{}'",
                guardada.getId(), usuario.getUsername(), nuevaSede.getNombre());

        return toResponse(guardada);
    }

    /**
     * Cierra manualmente la jornada activa del usuario autenticado.
     * Solo puede cerrar su propia jornada (o ADMIN cualquiera).
     */
    @Transactional
    public JornadaResponse cerrarJornada(Long jornadaId) {
        Jornada jornada = findOrThrow(jornadaId);
        validarPropietarioOAdmin(jornada);

        if (jornada.getEstado() == EstadoJornada.CERRADA) {
            throw new BusinessException(
                    "La jornada ya está cerrada desde " + jornada.getFechaCierre());
        }

        jornada.cerrar();
        Jornada guardada = jornadaRepository.save(jornada);

        log.info("Jornada cerrada manualmente: id={}, usuario='{}'",
                guardada.getId(), jornada.getUsuario().getUsername());

        return toResponse(guardada);
    }

    /**
     * Cierre automático interno usado por otros Services.
     * No requiere verificación de propietario (es llamado por el sistema).
     */
    @Transactional
    public void cerrarJornadaInterno(Long jornadaId) {
        Jornada jornada = findOrThrow(jornadaId);
        if (jornada.getEstado() == EstadoJornada.ABIERTA) {
            jornada.cerrar();
            jornadaRepository.save(jornada);
            log.info("Jornada cerrada internamente: id={}", jornadaId);
        }
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    Jornada findOrThrow(Long id) {
        return jornadaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada", "id", id));
    }

    /**
     * Verifica que el usuario autenticado sea el dueño de la jornada o sea ADMIN.
     */
    private void validarPropietarioOAdmin(Jornada jornada) {
        if (!securityUtils.esAdminOPropietario(jornada.getUsuario().getId())) {
            throw new ForbiddenException(
                    "No tienes permisos para acceder a la jornada de otra trabajadora.");
        }
    }
}
