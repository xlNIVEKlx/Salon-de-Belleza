package com.salon.belleza.config;

import com.salon.belleza.model.*;
import com.salon.belleza.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoServicioRepository catalogoServicioRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile("dev")
    public CommandLineRunner initData() {
        return args -> {
            log.info("=== Inicializando datos de prueba (perfil: dev) ===");

            // --- SEDES ---
            if (sedeRepository.count() == 0) {
                Sede aeropuerto = sedeRepository.save(new Sede("Sede Aeropuerto", "Barrio Aeropuerto"));
                Sede torcoroma = sedeRepository.save(new Sede("Sede Torcoroma", "Barrio Torcoroma"));
                Sede patios = sedeRepository.save(new Sede("Sede Patios", "Los Patios"));
                Sede chapinero = sedeRepository.save(new Sede("Sede Chapinero", "Barrio Chapinero"));
                log.info("✓ Sedes creadas: Aeropuerto, Torcoroma, Patios, Chapinero");

                // --- USUARIOS ---
                if (usuarioRepository.count() == 0) {
                    // Administrador general
                    usuarioRepository.save(new Usuario(
                            "Administradora",
                            "admin",
                            passwordEncoder.encode("admin123"),
                            Rol.ADMIN,
                            aeropuerto));

                    usuarioRepository.save(new Usuario(
                            "Nathalya",
                            "nathalya",
                            passwordEncoder.encode("1234"),
                            Rol.TRABAJADORA,
                            aeropuerto));

                    log.info("✓ Usuarios creados: admin, nathalya");
                }
            }

            // --- CATÁLOGO DE SERVICIOS ---
            if (catalogoServicioRepository.count() == 0) {
                BigDecimal comision50 = new BigDecimal("50.00");
                BigDecimal comision55 = new BigDecimal("55.00");

                catalogoServicioRepository.saveAll(java.util.List.of(
                        new CatalogoServicio("Uñas Acrílicas", new BigDecimal("85000"), comision50),
                        new CatalogoServicio("Uñas en Gel", new BigDecimal("70000"), comision50),
                        new CatalogoServicio("Manicure", new BigDecimal("25000"), comision50),
                        new CatalogoServicio("Pedicure", new BigDecimal("30000"), comision50),
                        new CatalogoServicio("Cejas (Depilación)", new BigDecimal("15000"), comision50),
                        new CatalogoServicio("Cejas (Diseño)", new BigDecimal("20000"), comision50),
                        new CatalogoServicio("Depilación Facial", new BigDecimal("20000"), comision50),
                        new CatalogoServicio("Depilación Cuerpo", new BigDecimal("40000"), comision50),
                        new CatalogoServicio("Pestañas (Lifting)", new BigDecimal("60000"), comision55),
                        new CatalogoServicio("Pestañas (Extensión)", new BigDecimal("90000"), comision55),
                        new CatalogoServicio("Tinte de Cejas", new BigDecimal("25000"), comision50)));

                log.info("✓ Catálogo de servicios creado: 11 servicios");
            }

            log.info("=== Inicialización completada ===");
        };
    }
}