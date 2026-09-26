package com.salon.belleza.service;

import com.salon.belleza.config.SecurityUtils;
import com.salon.belleza.dto.request.ServicioRealizadoRequest;
import com.salon.belleza.dto.response.ResumenJornadaResponse;
import com.salon.belleza.dto.response.ResumenServicioResponse;
import com.salon.belleza.dto.response.ServicioRealizadoResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ForbiddenException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.*;
import com.salon.belleza.repository.CatalogoServicioRepository;
import com.salon.belleza.repository.JornadaRepository;
import com.salon.belleza.repository.ServicioRealizadoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service para el registro y consulta de servicios realizados.
 *
 * LÓGICA CRÍTICA:
 * ─────────────────────────────────────────────────────────────────────────────
 * Al registrar un servicio:
 *   1. Verifica que la jornada del usuario esté ABIERTA.
 *   2. Resuelve la jornada activa automáticamente desde el usuario autenticado
 *      (no requiere que el frontend envíe el jornadaId).
 *   3. Delega el cálculo financiero al constructor de ServicioRealizado,
 *      que copia precio y comisión del catálogo en ese instante exacto.
 *   4. Los valores quedan inmutables en BD — historial protegido.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class ServicioRealizadoService {

    private static final Logger log = LoggerFactory.getLogger(ServicioRealizadoService.class);

    private final ServicioRealizadoRepository servicioRealizadoRepository;
    private final JornadaRepository jornadaRepository;
    private final CatalogoServicioRepository catalogoServicioRepository;
    private final SecurityUtils securityUtils;

    // ── Mapeo ────────────────────────────────────────────────────────────────

    public ServicioRealizadoResponse toResponse(ServicioRealizado sr) {
        return new ServicioRealizadoResponse(
                sr.getId(),
                sr.getJornada().getId(),
                sr.getServicio().getId(),
                sr.getServicio().getNombre(),
                sr.getCantidad(),
                sr.getPrecioUnitario(),
                sr.getPorcentajeComisionAplicado(),
                sr.getTotalCobrado(),
                sr.getComisionEmpleada(),
                sr.getTotalCajaSalon(),
                sr.getFechaRegistro()
        );
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    /**
     * Registra un servicio realizado en la jornada activa del usuario autenticado.
     *
     * El frontend solo envía { servicioId, cantidad }.
     * El backend resuelve automáticamente la jornada activa del usuario.
     */
    @Transactional
    public ServicioRealizadoResponse registrar(ServicioRealizadoRequest request) {
        Usuario usuario = securityUtils.getUsuarioAutenticado();

        // Resolver jornada: usar jornadaId del request si viene, o la jornada activa del usuario
        Jornada jornada;
        if (request.getJornadaId() != null) {
            jornada = jornadaRepository.findById(request.getJornadaId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Jornada", "id", request.getJornadaId()));
            // Solo ADMIN puede registrar en jornada de otro usuario
            if (!jornada.getUsuario().getId().equals(usuario.getId())
                    && !securityUtils.esAdmin()) {
                throw new ForbiddenException(
                        "No puedes registrar servicios en la jornada de otra trabajadora.");
            }
        } else {
            // Resolver automáticamente desde el usuario autenticado
            jornada = jornadaRepository
                    .findJornadaAbiertaByUsuario(usuario.getId())
                    .orElseThrow(() -> new BusinessException(
                            "No tienes una jornada abierta. Abre tu turno antes de registrar servicios."));
        }

        // Validar que la jornada esté abierta
        if (jornada.getEstado() == EstadoJornada.CERRADA) {
            throw new BusinessException(
                    "La jornada ya está cerrada. No se pueden registrar más servicios en ella.");
        }

        // Buscar el servicio en el catálogo
        CatalogoServicio servicio = catalogoServicioRepository
                .findById(request.getServicioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio", "id", request.getServicioId()));

        if (!servicio.getActivo()) {
            throw new BusinessException(
                    "El servicio '" + servicio.getNombre() + "' está desactivado y no puede registrarse.");
        }

        // El constructor de ServicioRealizado calcula y congela todos los valores financieros
        ServicioRealizado sr = new ServicioRealizado(jornada, servicio, request.getCantidad());
        ServicioRealizado guardado = servicioRealizadoRepository.save(sr);

        log.info("Servicio registrado: id={}, jornada={}, servicio='{}', cantidad={}, " +
                 "totalCobrado={}, comision={}, caja={}",
                guardado.getId(),
                jornada.getId(),
                servicio.getNombre(),
                request.getCantidad(),
                guardado.getTotalCobrado(),
                guardado.getComisionEmpleada(),
                guardado.getTotalCajaSalon()
        );

        return toResponse(guardado);
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /**
     * Lista todos los servicios de una jornada específica.
     */
    @Transactional(readOnly = true)
    public List<ServicioRealizadoResponse> listarPorJornada(Long jornadaId) {
        Jornada jornada = jornadaRepository.findById(jornadaId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada", "id", jornadaId));

        // Validar acceso
        if (!securityUtils.esAdminOPropietario(jornada.getUsuario().getId())) {
            throw new ForbiddenException("No puedes ver los servicios de la jornada de otra trabajadora.");
        }

        return servicioRealizadoRepository.findByJornadaId(jornadaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Genera el resumen completo de la jornada para la pantalla de cierre de caja.
     *
     * Devuelve:
     * - Desglose por tipo de servicio (cuántos de cada uno)
     * - Totales financieros: total cobrado, comisión empleada, total caja salón
     */
    @Transactional(readOnly = true)
    public ResumenJornadaResponse obtenerResumenJornada(Long jornadaId) {
        Jornada jornada = jornadaRepository.findById(jornadaId)
                .orElseThrow(() -> new ResourceNotFoundException("Jornada", "id", jornadaId));

        if (!securityUtils.esAdminOPropietario(jornada.getUsuario().getId())) {
            throw new ForbiddenException("No puedes ver el resumen de la jornada de otra trabajadora.");
        }

        // Consulta JPQL agrupada por tipo de servicio
        List<Object[]> rawData = servicioRealizadoRepository
                .resumenPorServicioEnJornada(jornadaId);

        List<ResumenServicioResponse> servicios = rawData.stream()
                .map(row -> {
                    String nombreServicio       = (String)     row[0];
                    Long   cantidadTotal        = (Long)       row[1];
                    BigDecimal totalCobrado     = (BigDecimal) row[2];
                    BigDecimal totalComision    = (BigDecimal) row[3];
                    BigDecimal totalCajaSalon   = totalCobrado.subtract(totalComision);

                    return new ResumenServicioResponse(
                            nombreServicio,
                            cantidadTotal,
                            totalCobrado,
                            totalComision,
                            totalCajaSalon
                    );
                })
                .toList();

        // Calcular grandes totales sumando los parciales
        BigDecimal granTotal        = servicios.stream()
                .map(ResumenServicioResponse::getTotalCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal granComision     = servicios.stream()
                .map(ResumenServicioResponse::getTotalComision)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal granCaja         = servicios.stream()
                .map(ResumenServicioResponse::getTotalCajaSalon)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenJornadaResponse(
                jornadaId,
                jornada.getUsuario().getNombre(),
                jornada.getSede().getNombre(),
                jornada.getFechaApertura().toLocalDate(),
                servicios,
                granTotal,
                granComision,
                granCaja
        );
    }

    /**
     * Obtiene el resumen de la jornada activa del usuario autenticado.
     * Atajo para la pantalla principal de la trabajadora.
     */
    @Transactional(readOnly = true)
    public ResumenJornadaResponse obtenerResumenJornadaPropia() {
        Usuario usuario = securityUtils.getUsuarioAutenticado();
        Jornada jornada = jornadaRepository
                .findJornadaAbiertaByUsuario(usuario.getId())
                .orElseThrow(() -> new BusinessException(
                        "No tienes una jornada abierta. Abre tu turno para ver el resumen."));
        return obtenerResumenJornada(jornada.getId());
    }
}
