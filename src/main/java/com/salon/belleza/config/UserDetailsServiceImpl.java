package com.salon.belleza.config;

import com.salon.belleza.model.Usuario;
import com.salon.belleza.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 *
 * Spring Security llama a este servicio automáticamente al procesar
 * cada request autenticado. Carga el usuario desde la BD por su
 * username y construye el objeto UserDetails con su rol como authority.
 *
 * El rol se mapea como "ROLE_ADMIN" o "ROLE_TRABAJADORA" siguiendo
 * la convención de Spring Security para @PreAuthorize.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        // Verificar que la cuenta está activa
        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException(
                    "La cuenta de usuario '" + username + "' está desactivada");
        }

        // Mapear el Rol del dominio a GrantedAuthority de Spring Security
        // Convención: "ROLE_" + nombre del enum → ROLE_ADMIN, ROLE_TRABAJADORA
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())
        );

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!usuario.getActivo())
                .credentialsExpired(false)
                .disabled(!usuario.getActivo())
                .build();
    }
}
