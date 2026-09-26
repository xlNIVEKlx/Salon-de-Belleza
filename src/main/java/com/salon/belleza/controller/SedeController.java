package com.salon.belleza.controller;

import com.salon.belleza.dto.request.SedeRequest;
import com.salon.belleza.dto.response.SedeResponse;
import com.salon.belleza.service.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint para gestión de Sedes.
 * Según SecurityConfig, todos los métodos requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    public ResponseEntity<List<SedeResponse>> listarTodas() {
        return ResponseEntity.ok(sedeService.listarTodas());
    }

    @GetMapping("/activas")
    public ResponseEntity<List<SedeResponse>> listarActivas() {
        return ResponseEntity.ok(sedeService.listarActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(sedeService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sedeService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SedeResponse> actualizar(@PathVariable Long id,
                                                   @Valid @RequestBody SedeRequest request) {
        return ResponseEntity.ok(sedeService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        sedeService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
