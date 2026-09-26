package com.salon.belleza.controller;

import com.salon.belleza.dto.request.ServicioRealizadoRequest;
import com.salon.belleza.dto.response.ResumenJornadaResponse;
import com.salon.belleza.dto.response.ServicioRealizadoResponse;
import com.salon.belleza.service.ServicioRealizadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint para registrar servicios y consultar resúmenes.
 */
@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioRealizadoController {

    private final ServicioRealizadoService servicioRealizadoService;

    @PostMapping("/registrar")
    public ResponseEntity<ServicioRealizadoResponse> registrar(
            @Valid @RequestBody ServicioRealizadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioRealizadoService.registrar(request));
    }

    @GetMapping("/jornada/{jornadaId}")
    public ResponseEntity<List<ServicioRealizadoResponse>> listarPorJornada(
            @PathVariable Long jornadaId) {
        return ResponseEntity.ok(servicioRealizadoService.listarPorJornada(jornadaId));
    }

    @GetMapping("/jornada/{jornadaId}/resumen")
    public ResponseEntity<ResumenJornadaResponse> obtenerResumenJornada(
            @PathVariable Long jornadaId) {
        return ResponseEntity.ok(servicioRealizadoService.obtenerResumenJornada(jornadaId));
    }

    @GetMapping("/mi-resumen")
    public ResponseEntity<ResumenJornadaResponse> obtenerMiResumenActivo() {
        return ResponseEntity.ok(servicioRealizadoService.obtenerResumenJornadaPropia());
    }
}
