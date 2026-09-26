package com.salon.belleza.controller;

import com.salon.belleza.dto.request.AbrirJornadaRequest;
import com.salon.belleza.dto.response.JornadaResponse;
import com.salon.belleza.service.JornadaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint para jornadas.
 * Accesible para TRABAJADORA y ADMIN.
 */
@RestController
@RequestMapping("/api/jornadas")
@RequiredArgsConstructor
public class JornadaController {

    private final JornadaService jornadaService;

    @PostMapping("/abrir")
    public ResponseEntity<JornadaResponse> abrirJornada(@Valid @RequestBody AbrirJornadaRequest request) {
        return ResponseEntity.ok(jornadaService.abrirJornada(request));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<JornadaResponse> cerrarJornada(@PathVariable Long id) {
        return ResponseEntity.ok(jornadaService.cerrarJornada(id));
    }

    @GetMapping("/mia")
    public ResponseEntity<JornadaResponse> obtenerMiJornadaActiva() {
        return jornadaService.buscarJornadaAbiertaPropia()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<JornadaResponse>> listarMiHistorial() {
        return ResponseEntity.ok(jornadaService.listarHistorialPropio());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JornadaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jornadaService.buscarPorId(id));
    }

    @GetMapping("/sede/{sedeId}/abiertas")
    public ResponseEntity<List<JornadaResponse>> listarAbiertasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(jornadaService.listarAbiertasPorSede(sedeId));
    }
}
