-- ============================================================
-- SALON DE BELLEZA - Scripts SQL complementarios para PostgreSQL
-- Ejecutar DESPUÉS de que Spring Boot/Hibernate cree las tablas
-- con ddl-auto=update
-- ============================================================

-- ------------------------------------------------------------
-- RESTRICCIÓN DE UNICIDAD PARCIAL (PostgreSQL):
-- Garantiza que un usuario solo tenga UNA jornada ABIERTA a la vez.
-- Esta restricción no puede expresarse solo con JPA/Hibernate,
-- requiere un índice parcial de PostgreSQL.
-- ------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_jornada_usuario_abierta
    ON jornadas (usuario_id)
    WHERE estado = 'ABIERTA';

-- ------------------------------------------------------------
-- DATOS INICIALES DE PRUEBA
-- Descomenta y ajusta según tu entorno de desarrollo.
-- La contraseña '1234' en BCrypt con strength 10:
-- $2a$10$7EqJtq98hPqEX7fNZaFWoO6bAoRfYnMZ3gvPxKt5kY2C5tqHE7Xyy
-- ------------------------------------------------------------

-- Sedes de ejemplo
INSERT INTO sedes (nombre, direccion, activa)
VALUES
    ('Sede Centro',    'Calle 10 # 5-20, Centro',     true),
    ('Sede Norte',     'Av. 68 # 120-30, Usaquén',    true),
    ('Sede Occidente', 'Cra. 80 # 15-40, Fontibón',   true)
ON CONFLICT DO NOTHING;

-- Usuario Admin (password: admin123)
-- BCrypt hash de 'admin123' con strength 10:
-- $2a$10$slYQmyNdgTY18LGvgxBeIuZmMmQ.XEe4NKJM9v1bX3cVJoTpjCKrS
INSERT INTO usuarios (nombre, username, password_hash, rol, sede_principal_id, activo)
SELECT 'Administradora', 'admin', '$2a$10$slYQmyNdgTY18LGvgxBeIuZmMmQ.XEe4NKJM9v1bX3cVJoTpjCKrS', 'ADMIN', id, true
FROM sedes WHERE nombre = 'Sede Centro'
ON CONFLICT (username) DO NOTHING;

-- Trabajadoras de ejemplo (password: 1234)
-- BCrypt hash de '1234': $2a$10$7EqJtq98hPqEX7fNZaFWoO6bAoRfYnMZ3gvPxKt5kY2C5tqHE7Xyy
INSERT INTO usuarios (nombre, username, password_hash, rol, sede_principal_id, activo)
SELECT 'María García', 'maria.garcia', '$2a$10$7EqJtq98hPqEX7fNZaFWoO6bAoRfYnMZ3gvPxKt5kY2C5tqHE7Xyy', 'TRABAJADORA', id, true
FROM sedes WHERE nombre = 'Sede Centro'
ON CONFLICT (username) DO NOTHING;

INSERT INTO usuarios (nombre, username, password_hash, rol, sede_principal_id, activo)
SELECT 'Ana Martínez', 'ana.martinez', '$2a$10$7EqJtq98hPqEX7fNZaFWoO6bAoRfYnMZ3gvPxKt5kY2C5tqHE7Xyy', 'TRABAJADORA', id, true
FROM sedes WHERE nombre = 'Sede Norte'
ON CONFLICT (username) DO NOTHING;

-- Catálogo de servicios con precios y comisiones de ejemplo
INSERT INTO catalogo_servicios (nombre, precio, porcentaje_comision, activo)
VALUES
    ('Uñas Acrílicas',      85000.00, 50.00, true),
    ('Uñas en Gel',         70000.00, 50.00, true),
    ('Manicure',            25000.00, 50.00, true),
    ('Pedicure',            30000.00, 50.00, true),
    ('Cejas (Depilación)',  15000.00, 50.00, true),
    ('Cejas (Diseño)',      20000.00, 50.00, true),
    ('Depilación Facial',   20000.00, 50.00, true),
    ('Depilación Cuerpo',   40000.00, 50.00, true),
    ('Pestañas (Lifting)',  60000.00, 55.00, true),
    ('Pestañas (Extensión)',90000.00, 55.00, true),
    ('Tinte de Cejas',      25000.00, 50.00, true)
ON CONFLICT DO NOTHING;

-- ============================================================
-- VERIFICACIÓN DE CONSISTENCIA
-- ============================================================
-- Consulta para verificar que no hay jornadas abiertas duplicadas:
-- SELECT usuario_id, COUNT(*) FROM jornadas WHERE estado = 'ABIERTA'
-- GROUP BY usuario_id HAVING COUNT(*) > 1;
