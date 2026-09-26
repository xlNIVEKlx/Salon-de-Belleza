package com.salon.belleza.controller;

import com.salon.belleza.dto.request.CatalogoServicioRequest;
import com.salon.belleza.dto.response.CatalogoServicioResponse;
import com.salon.belleza.service.CatalogoServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint para el catálogo de servicios.
 * ADMIN gestiona, TRABAJADORA solo puede ver activos (/activos).
 */
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoServicioController {

    private final CatalogoServicioService catalogoService;

    /** Público para ambos roles (configurado en SecurityConfig) */
    @GetMapping("/activos")
    public ResponseEntity<List<CatalogoServicioResponse>> listarActivos() {
        return ResponseEntity.ok(catalogoService.listarActivos());
    }

    // --- Los siguientes requieren ADMIN ---

    @GetMapping
    public ResponseEntity<List<CatalogoServicioResponse>> listarTodos() {
        return ResponseEntity.ok(catalogoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogoServicioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<CatalogoServicioResponse> crear(@Valid @RequestBody CatalogoServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogoServicioResponse> actualizar(@PathVariable Long id,
                                                               @Valid @RequestBody CatalogoServicioRequest request) {
        return ResponseEntity.ok(catalogoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        catalogoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
