package com.salon.belleza.service;

import com.salon.belleza.dto.response.CajaSedeResponse;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.EstadoJornada;
import com.salon.belleza.model.Sede;
import com.salon.belleza.repository.JornadaRepository;
import com.salon.belleza.repository.SedeRepository;
import com.salon.belleza.repository.ServicioRealizadoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service para el cálculo de caja en tiempo real por sede.
 *
 * LÓGICA DE NEGOCIO:
 * ─────────────────────────────────────────────────────────────────────────────
 * El endpoint de caja suma el total_caja_salon de TODOS los servicios
 * registrados HOY en una sede, independientemente de qué trabajadora los hizo.
 *
 * Esto permite al dueño/a del salón saber cuánto dinero físico
 * debe haber en la caja de cada sede en cualquier momento del día.
 *
 * También suma las comisiones totales acumuladas para saber cuánto
 * se deberá pagar a las trabajadoras al final del turno.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class CajaService {

    private static final Logger log = LoggerFactory.getLogger(CajaService.class);

    private final ServicioRealizadoRepository servicioRealizadoRepository;
    private final JornadaRepository jornadaRepository;
    private final SedeRepository sedeRepository;

    /**
     * Calcula el estado de caja de una sede para el día de hoy.
     *
     * Suma:
     * - total_cobrado (lo que se le cobró a las clientas)
     * - comision_empleada (lo que se le pagará a las trabajadoras)
     * - total_caja_salon (el dinero físico que debe estar en caja)
     *
     * @param sedeId ID de la sede a consultar
     * @return CajaSedeResponse con todos los totales del día
     */
    @Transactional(readOnly = true)
    public CajaSedeResponse calcularCajaHoy(Long sedeId) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", sedeId));

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);

        // Suma total de caja (solo para el salón) — query en ServicioRealizadoRepository
        BigDecimal totalCajaSalon = servicioRealizadoRepository
                .sumTotalCajaSalonPorSedeYFecha(sedeId, inicioDia, finDia);

        // Calcular total cobrado y comisiones sumando sobre todas las jornadas de la sede hoy
        // Obtenemos todas las jornadas activas o ya cerradas de hoy en esta sede
        BigDecimal totalProducido = calcularTotalProducidoHoy(sedeId, inicioDia, finDia);
        BigDecimal totalComisiones = totalProducido.subtract(totalCajaSalon);

        // Número de trabajadoras con jornada ABIERTA en este momento en la sede
        int trabajadorasActivas = jornadaRepository
                .findBySedeIdAndEstado(sedeId, EstadoJornada.ABIERTA)
                .size();

        log.debug("Caja calculada — sede='{}', totalProducido={}, comisiones={}, caja={}, trabajadoras={}",
                sede.getNombre(), totalProducido, totalComisiones, totalCajaSalon, trabajadorasActivas);

        return new CajaSedeResponse(
                sedeId,
                sede.getNombre(),
                hoy,
                trabajadorasActivas,
                totalProducido,
                totalComisiones,
                totalCajaSalon
        );
    }

    /**
     * Calcula la caja de una sede para una fecha específica (para reportes históricos).
     *
     * @param sedeId ID de la sede
     * @param fecha  Fecha a consultar
     */
    @Transactional(readOnly = true)
    public CajaSedeResponse calcularCajaPorFecha(Long sedeId, LocalDate fecha) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", sedeId));

        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.atTime(LocalTime.MAX);

        BigDecimal totalCajaSalon = servicioRealizadoRepository
                .sumTotalCajaSalonPorSedeYFecha(sedeId, inicioDia, finDia);

        BigDecimal totalProducido = calcularTotalProducidoHoy(sedeId, inicioDia, finDia);
        BigDecimal totalComisiones = totalProducido.subtract(totalCajaSalon);

        return new CajaSedeResponse(
                sedeId,
                sede.getNombre(),
                fecha,
                0, // En fechas pasadas no hay trabajadoras "activas"
                totalProducido,
                totalComisiones,
                totalCajaSalon
        );
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    /**
     * Suma el total_cobrado de todos los servicios de una sede en un rango de fechas.
     * Calcula totalCobrado = totalCajaSalon + comisiones en un solo paso
     * usando la relación: totalCobrado = caja + comisión por cada registro.
     */
    private BigDecimal calcularTotalProducidoHoy(Long sedeId,
                                                  LocalDateTime inicio,
                                                  LocalDateTime fin) {
        // Sumamos totalCobrado directamente desde los servicios realizados
        // usando JPQL que filtra por sede a través de la jornada
        return jornadaRepository
                .findBySedeIdAndEstado(sedeId, EstadoJornada.ABIERTA)
                .stream()
                .map(j -> servicioRealizadoRepository.sumTotalCobradoByJornada(j.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(
                    // Sumar también jornadas cerradas hoy (trabajadoras que ya terminaron)
                    jornadaRepository
                        .findJornadasAbiertasHoyEnSede(sedeId, inicio)
                        .stream()
                        .map(j -> servicioRealizadoRepository.sumTotalCobradoByJornada(j.getId()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                );
    }
}
