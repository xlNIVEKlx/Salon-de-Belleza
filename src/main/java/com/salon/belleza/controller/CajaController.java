package com.salon.belleza.controller;

import com.salon.belleza.dto.response.CajaSedeResponse;
import com.salon.belleza.service.CajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Endpoint para consultar el estado de la caja de las sedes.
 */
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final CajaService cajaService;

    @GetMapping("/sede/{sedeId}/hoy")
    public ResponseEntity<CajaSedeResponse> obtenerCajaHoy(@PathVariable Long sedeId) {
        return ResponseEntity.ok(cajaService.calcularCajaHoy(sedeId));
    }

    @GetMapping("/sede/{sedeId}/fecha")
    public ResponseEntity<CajaSedeResponse> obtenerCajaPorFecha(
            @PathVariable Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(cajaService.calcularCajaPorFecha(sedeId, fecha));
    }
}
