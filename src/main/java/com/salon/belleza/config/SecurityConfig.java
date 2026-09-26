package com.salon.belleza.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.belleza.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * Estrategia: sesión HTTP clásica con cookie JSESSIONID.
 * No se usa JWT para mantener la implementación simple y adecuada
 * al contexto de una app web interna de salón de belleza.
 *
 * Protección por roles:
 *   - ADMIN: acceso completo
 *   - TRABAJADORA: acceso a su propia jornada, servicios y citas
 *
 * @EnableMethodSecurity habilita @PreAuthorize en los Controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // PASSWORD ENCODER
    // =========================================================================

    /**
     * BCrypt con strength 10 (recomendado para producción).
     * Cada hash generado es único gracias al salt aleatorio incorporado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // =========================================================================
    // AUTHENTICATION PROVIDER
    // =========================================================================

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // =========================================================================
    // CORS
    // =========================================================================

    /**
     * CORS permisivo para desarrollo local.
     * En producción, reemplazar "*" por el dominio real del frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // =========================================================================
    // SECURITY FILTER CHAIN
    // =========================================================================

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF (la app es SPA que usa sesión, pero el token
            // CSRF complica el JS puro; en producción evaluar habilitarlo)
            .csrf(AbstractHttpConfigurer::disable)

            // Habilitar CORS con la config definida arriba
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Gestión de sesión: ALWAYS para mantener la sesión del usuario
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Reglas de autorización por endpoint
            .authorizeHttpRequests(auth -> auth
                // Recursos públicos: archivos estáticos del frontend
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/favicon.ico"
                ).permitAll()

                // Endpoint de autenticación: público
                .requestMatchers("/api/auth/**").permitAll()

                // Solo ADMIN puede gestionar sedes, usuarios y ver reportes globales
                .requestMatchers("/api/sedes/**").hasRole("ADMIN")
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/reportes/**").hasRole("ADMIN")

                // Catálogo: Trabajadoras pueden ver los activos, pero solo ADMIN puede modificar
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/catalogo/activos").hasAnyRole("ADMIN", "TRABAJADORA")
                .requestMatchers("/api/catalogo/**").hasRole("ADMIN")

                // Jornadas, servicios y citas: accesibles a ambos roles
                .requestMatchers("/api/jornadas/**").hasAnyRole("ADMIN", "TRABAJADORA")
                .requestMatchers("/api/servicios/**").hasAnyRole("ADMIN", "TRABAJADORA")
                .requestMatchers("/api/citas/**").hasAnyRole("ADMIN", "TRABAJADORA")
                .requestMatchers("/api/caja/**").hasAnyRole("ADMIN", "TRABAJADORA")

                // Cualquier otro endpoint requiere autenticación
                .anyRequest().authenticated()
            )

            // Handler personalizado para errores 401 (no autenticado)
            // Devuelve JSON en lugar de redirigir al login HTML de Spring
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ErrorResponse error = new ErrorResponse(
                            HttpStatus.UNAUTHORIZED.value(),
                            "Debes iniciar sesión para acceder a este recurso"
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ErrorResponse error = new ErrorResponse(
                            HttpStatus.FORBIDDEN.value(),
                            "No tienes permisos para realizar esta acción"
                    );
                    objectMapper.writeValue(response.getWriter(), error);
                })
            )

            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
