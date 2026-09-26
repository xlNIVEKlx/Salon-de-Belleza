package com.salon.belleza.service;

import com.salon.belleza.config.SecurityUtils;
import com.salon.belleza.dto.request.CitaRequest;
import com.salon.belleza.dto.response.CitaResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ForbiddenException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.*;
import com.salon.belleza.repository.CitaCalendarioRepository;
import com.salon.belleza.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service para la gestión de citas en el calendario.
 *
 * Reglas de acceso:
 * - TRABAJADORA: puede crear citas y ver las propias (filtradas por su usuario).
 * - ADMIN: puede ver todas las citas de cualquier sede y usuario.
 * - Editar/cancelar: solo el creador de la cita o un ADMIN.
 */
@Service
@RequiredArgsConstructor
public class CitaService {

    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    private final CitaCalendarioRepository citaRepository;
    private final SedeRepository sedeRepository;
    private final SecurityUtils securityUtils;

    // ── Mapeo ────────────────────────────────────────────────────────────────

    public CitaResponse toResponse(CitaCalendario c) {
        return new CitaResponse(
                c.getId(),
                c.getUsuario().getId(),
                c.getUsuario().getNombre(),
                c.getSede().getId(),
                c.getSede().getNombre(),
                c.getNombreClienta(),
                c.getTelefonoClienta(),
                c.getFechaHoraCita(),
                c.getNotas(),
                c.getEstado(),
                c.getFechaCreacion()
        );
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * Lista las citas del usuario autenticado para un mes completo.
     * Filtro principal para la vista de calendario de la trabajadora.
     */
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitasPropiasPorMes(int year, int month) {
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        LocalDateTime inicio = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime fin = inicio.toLocalDate().withDayOfMonth(
                inicio.toLocalDate().lengthOfMonth()).atTime(LocalTime.MAX);

        return citaRepository
                .findByUsuarioIdAndFechaHoraCitaBetween(usuario.getId(), inicio, fin)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lista todas las citas de una sede para un mes (solo ADMIN).
     */
    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitasPorSedePorMes(Long sedeId, int year, int month) {
        LocalDateTime inicio = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime fin = inicio.toLocalDate().withDayOfMonth(
                inicio.toLocalDate().lengthOfMonth()).atTime(LocalTime.MAX);

        return citaRepository
                .findBySedeIdAndFechaHoraCitaBetween(sedeId, inicio, fin)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lista las próximas citas del usuario autenticado (pendientes y confirmadas).
     * Para mostrar en el dashboard como recordatorios.
     */
    @Transactional(readOnly = true)
    public List<CitaResponse> listarProximasCitasPropia() {
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        return citaRepository
                .findCitasProximasByUsuario(usuario.getId(), LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse buscarPorId(Long id) {
        CitaCalendario cita = findOrThrow(id);
        validarAcceso(cita);
        return toResponse(cita);
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    @Transactional
    public CitaResponse crear(CitaRequest request) {
        Usuario usuario = securityUtils.getUsuarioAutenticado();

        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede", "id", request.getSedeId()));

        CitaCalendario cita = new CitaCalendario(
                usuario,
                sede,
                request.getNombreClienta(),
                request.getTelefonoClienta(),
                request.getFechaHoraCita(),
                request.getNotas()
        );

        if (request.getEstado() != null) {
            cita.setEstado(request.getEstado());
        }

        CitaCalendario guardada = citaRepository.save(cita);
        log.info("Cita creada: id={}, clienta='{}', fecha={}, usuario='{}'",
                guardada.getId(), guardada.getNombreClienta(),
                guardada.getFechaHoraCita(), usuario.getUsername());
        return toResponse(guardada);
    }

    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest request) {
        CitaCalendario cita = findOrThrow(id);
        validarAcceso(cita);

        // No se puede editar una cita completada o cancelada
        if (cita.getEstado() == EstadoCita.COMPLETADA || cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessException(
                    "No se puede modificar una cita en estado " + cita.getEstado() + ".");
        }

        Sede sede = sedeRepository.findById(request.getSedeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede", "id", request.getSedeId()));

        cita.setSede(sede);
        cita.setNombreClienta(request.getNombreClienta());
        cita.setTelefonoClienta(request.getTelefonoClienta());
        cita.setFechaHoraCita(request.getFechaHoraCita());
        cita.setNotas(request.getNotas());
        if (request.getEstado() != null) {
            cita.setEstado(request.getEstado());
        }

        log.info("Cita actualizada: id={}", id);
        return toResponse(citaRepository.save(cita));
    }

    /**
     * Cancela una cita cambiando su estado a CANCELADA.
     * Operación más segura que eliminar — preserva el historial.
     */
    @Transactional
    public CitaResponse cancelar(Long id) {
        CitaCalendario cita = findOrThrow(id);
        validarAcceso(cita);

        if (cita.getEstado() == EstadoCita.COMPLETADA) {
            throw new BusinessException("No se puede cancelar una cita que ya fue completada.");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessException("La cita ya está cancelada.");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        log.info("Cita cancelada: id={}", id);
        return toResponse(citaRepository.save(cita));
    }

    /**
     * Marca una cita como COMPLETADA.
     */
    @Transactional
    public CitaResponse completar(Long id) {
        CitaCalendario cita = findOrThrow(id);
        validarAcceso(cita);

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessException("No se puede completar una cita cancelada.");
        }
        if (cita.getEstado() == EstadoCita.COMPLETADA) {
            throw new BusinessException("La cita ya está marcada como completada.");
        }

        cita.setEstado(EstadoCita.COMPLETADA);
        log.info("Cita completada: id={}", id);
        return toResponse(citaRepository.save(cita));
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    private CitaCalendario findOrThrow(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", id));
    }

    /**
     * Valida que el usuario autenticado sea el creador de la cita o sea ADMIN.
     */
    private void validarAcceso(CitaCalendario cita) {
        if (!securityUtils.esAdminOPropietario(cita.getUsuario().getId())) {
            throw new ForbiddenException(
                    "No tienes permisos para acceder a la cita de otra trabajadora.");
        }
    }
}
