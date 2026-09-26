package com.salon.belleza.service;

import com.salon.belleza.dto.request.UsuarioRequest;
import com.salon.belleza.dto.response.UsuarioResponse;
import com.salon.belleza.exception.BusinessException;
import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.Sede;
import com.salon.belleza.model.Usuario;
import com.salon.belleza.repository.SedeRepository;
import com.salon.belleza.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service para gestión de Usuarios (trabajadoras y admins).
 * Solo accesible por ADMIN, excepto la consulta del propio perfil.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Mapeo ────────────────────────────────────────────────────────────────

    public UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getUsername(),
                u.getRol(),
                u.getSedePrincipal().getId(),
                u.getSedePrincipal().getNombre(),
                u.getActivo()
        );
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPorSede(Long sedeId) {
        return usuarioRepository.findBySedePrincipalIdAndActivoTrue(sedeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ── Comandos ─────────────────────────────────────────────────────────────

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        // Validar username único
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                    "Ya existe un usuario con el username '" + request.getUsername() + "'");
        }

        Sede sede = sedeRepository.findById(request.getSedePrincipalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede", "id", request.getSedePrincipalId()));

        Usuario usuario = new Usuario(
                request.getNombre(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()), // ← BCrypt aquí
                request.getRol(),
                sede
        );

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado: id={}, username='{}', rol={}",
                guardado.getId(), guardado.getUsername(), guardado.getRol());
        return toResponse(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = findOrThrow(id);

        // Validar username único (excluir el propio registro)
        usuarioRepository.findByUsername(request.getUsername()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException(
                        "Ya existe un usuario con el username '" + request.getUsername() + "'");
            }
        });

        Sede sede = sedeRepository.findById(request.getSedePrincipalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede", "id", request.getSedePrincipalId()));

        usuario.setNombre(request.getNombre());
        usuario.setUsername(request.getUsername());
        usuario.setSedePrincipal(sede);
        usuario.setRol(request.getRol());

        // Solo actualizar contraseña si viene en el request
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        log.info("Usuario actualizado: id={}", id);
        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = findOrThrow(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario desactivado: id={}", id);
    }

    // ── Helpers internos ─────────────────────────────────────────────────────

    Usuario findOrThrow(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    Usuario findByUsernameOrThrow(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "username", username));
    }
}
