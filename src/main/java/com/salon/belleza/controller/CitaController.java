package com.salon.belleza.controller;

import com.salon.belleza.dto.request.CitaRequest;
import com.salon.belleza.dto.response.CitaResponse;
import com.salon.belleza.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint para gestión de citas del calendario.
 */
@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @GetMapping("/mias")
    public ResponseEntity<List<CitaResponse>> listarMiasPorMes(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(citaService.listarCitasPropiasPorMes(year, month));
    }

    @GetMapping("/proximas")
    public ResponseEntity<List<CitaResponse>> listarProximas() {
        return ResponseEntity.ok(citaService.listarProximasCitasPropia());
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<CitaResponse>> listarPorSedeMes(
            @PathVariable Long sedeId, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(citaService.listarCitasPorSedePorMes(sedeId, year, month));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CitaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitaResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody CitaRequest request) {
        return ResponseEntity.ok(citaService.actualizar(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<CitaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.cancelar(id));
    }

    @PatchMapping("/{id}/completar")
    public ResponseEntity<CitaResponse> completar(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.completar(id));
    }
}
