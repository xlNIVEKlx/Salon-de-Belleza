package com.salon.belleza.controller;

import com.salon.belleza.config.SecurityUtils;
import com.salon.belleza.dto.request.LoginRequest;
import com.salon.belleza.dto.response.ErrorResponse;
import com.salon.belleza.dto.response.LoginResponse;
import com.salon.belleza.model.EstadoJornada;
import com.salon.belleza.model.Jornada;
import com.salon.belleza.model.Usuario;
import com.salon.belleza.repository.JornadaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controlador de autenticación.
 *
 * POST /api/auth/login  → inicia sesión y devuelve datos del usuario + estado de jornada
 * POST /api/auth/logout → invalida la sesión HTTP
 * GET  /api/auth/me     → devuelve el usuario autenticado actual (para refrescar estado)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityUtils securityUtils;
    private final JornadaRepository jornadaRepository;

    /**
     * Inicia sesión con username y password.
     * Al autenticarse correctamente, devuelve los datos del usuario y si tiene
     * una jornada activa, para que el frontend inicialice su estado de una sola vez.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Guardar en el SecurityContext de la sesión
            SecurityContextHolder.getContext().setAuthentication(authentication);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT",
                    SecurityContextHolder.getContext());

            // Cargar datos completos del usuario desde la BD
            Usuario usuario = securityUtils.getUsuarioAutenticado();

            // Verificar si tiene jornada abierta
            Optional<Jornada> jornadaActiva = jornadaRepository
                    .findJornadaAbiertaByUsuario(usuario.getId());

            LoginResponse response = new LoginResponse(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getUsername(),
                    usuario.getRol(),
                    usuario.getSedePrincipal().getId(),
                    usuario.getSedePrincipal().getNombre(),
                    jornadaActiva.isPresent(),
                    jornadaActiva.map(Jornada::getId).orElse(null),
                    jornadaActiva.map(j -> j.getSede().getNombre()).orElse(null)
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(401, "Usuario o contraseña incorrectos"));
        }
    }

    /**
     * Cierra la sesión e invalida la cookie de sesión.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve el perfil del usuario autenticado con su estado de jornada.
     * Útil para que el frontend refresque el estado al recargar la página.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me() {
        Usuario usuario = securityUtils.getUsuarioAutenticado();

        Optional<Jornada> jornadaActiva = jornadaRepository
                .findJornadaAbiertaByUsuario(usuario.getId());

        LoginResponse response = new LoginResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getUsername(),
                usuario.getRol(),
                usuario.getSedePrincipal().getId(),
                usuario.getSedePrincipal().getNombre(),
                jornadaActiva.isPresent(),
                jornadaActiva.map(Jornada::getId).orElse(null),
                jornadaActiva.map(j -> j.getSede().getNombre()).orElse(null)
        );

        return ResponseEntity.ok(response);
    }
}
