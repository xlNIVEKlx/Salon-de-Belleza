package com.salon.belleza.config;

import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.Usuario;
import com.salon.belleza.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utilidad de seguridad para obtener el usuario autenticado
 * desde cualquier capa del backend (Services, Controllers).
 *
 * Evita repetir la misma lógica de extraer el principal
 * del SecurityContext en cada Service. Garantiza que siempre
 * se trabaje con la entidad Usuario de la BD, no solo el username.
 *
 * Uso típico en un Service:
 *   Usuario usuarioActual = securityUtils.getUsuarioAutenticado();
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;

    /**
     * Obtiene la entidad Usuario completa del usuario autenticado en la sesión.
     *
     * @return El Usuario autenticado con todos sus datos de BD
     * @throws ResourceNotFoundException si el usuario del contexto no existe en BD
     *         (situación anómala que indica inconsistencia de datos)
     * @throws IllegalStateException si no hay ningún usuario autenticado
     */
    public Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException(
                    "No hay ningún usuario autenticado en el contexto de seguridad");
        }

        String username = authentication.getName();

        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado", "username", username));
    }

    /**
     * Obtiene solo el username del usuario autenticado sin consultar la BD.
     * Más eficiente cuando solo se necesita el identificador.
     *
     * @return username del usuario autenticado, o null si no hay sesión
     */
    public String getUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * Verifica si el usuario autenticado tiene el rol ADMIN.
     *
     * @return true si es administrador
     */
    public boolean esAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Verifica que el usuario autenticado sea el dueño del recurso
     * o sea ADMIN. Usado para proteger recursos propios de una trabajadora.
     *
     * @param propietarioId ID del usuario dueño del recurso
     * @return true si el usuario autenticado es el propietario o es ADMIN
     */
    public boolean esAdminOPropietario(Long propietarioId) {
        if (esAdmin()) return true;
        Usuario actual = getUsuarioAutenticado();
        return actual.getId().equals(propietarioId);
    }
}
