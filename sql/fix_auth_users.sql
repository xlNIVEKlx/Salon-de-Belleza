-- ============================================================
-- SCRIPT DE CORRECCIÓN DEFINITIVA DE USUARIOS Y AUTENTICACIÓN
-- Ejecuta este script completo en el SQL Editor de Supabase
-- ============================================================

-- 1. Eliminar triggers o políticas conflictivas anteriores
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user CASCADE;

DROP POLICY IF EXISTS "perfiles_lectura" ON public.perfiles;
DROP POLICY IF EXISTS "perfiles_update_propio" ON public.perfiles;
DROP POLICY IF EXISTS "perfiles_lectura_autenticados" ON public.perfiles;
DROP POLICY IF EXISTS "perfiles_modificar" ON public.perfiles;
DROP POLICY IF EXISTS "perfiles_insert_propio" ON public.perfiles;

-- 2. Función is_admin() a prueba de recursión
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 
        FROM public.perfiles 
        WHERE id = auth.uid() 
          AND rol = 'ADMIN'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE SET search_path = public;

-- 3. Políticas RLS limpias para 'perfiles'
CREATE POLICY "perfiles_select_all"
    ON public.perfiles FOR SELECT
    TO authenticated
    USING (true);

CREATE POLICY "perfiles_insert_all"
    ON public.perfiles FOR INSERT
    TO authenticated
    WITH CHECK (true);

CREATE POLICY "perfiles_update_all"
    ON public.perfiles FOR UPDATE
    TO authenticated
    USING (id = auth.uid() OR public.is_admin());

-- 4. Trigger seguro para nuevos registros
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_sede_id BIGINT;
    v_rol VARCHAR(20);
    v_nombre VARCHAR(150);
    v_username VARCHAR(50);
BEGIN
    SELECT id INTO v_sede_id FROM public.sedes WHERE activa = true ORDER BY id LIMIT 1;
    v_username := split_part(NEW.email, '@', 1);
    v_rol := CASE WHEN v_username ILIKE '%admin%' THEN 'ADMIN' ELSE 'TRABAJADORA' END;
    v_nombre := COALESCE(NEW.raw_user_meta_data ->> 'nombre', initcap(v_username));

    INSERT INTO public.perfiles (id, nombre, username, rol, sede_principal_id, activo)
    VALUES (
        NEW.id,
        v_nombre,
        v_username,
        COALESCE(NEW.raw_user_meta_data ->> 'rol', v_rol),
        COALESCE((NEW.raw_user_meta_data ->> 'sede_principal_id')::bigint, COALESCE(v_sede_id, 1)),
        true
    )
    ON CONFLICT (id) DO UPDATE SET
        nombre = EXCLUDED.nombre,
        username = EXCLUDED.username;

    RETURN NEW;
EXCEPTION WHEN OTHERS THEN
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- 5. Crear extensión pgcrypto si no existe
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 6. Limpiar y recrear usuarios de prueba garantizando email confirmado y password correcto
DO $$
DECLARE
    v_admin_id UUID := gen_random_uuid();
    v_maria_id UUID := gen_random_uuid();
    v_ana_id   UUID := gen_random_uuid();
    v_sede_1   BIGINT;
    v_sede_2   BIGINT;
BEGIN
    -- Obtener sedes
    SELECT id INTO v_sede_1 FROM public.sedes ORDER BY id LIMIT 1;
    SELECT id INTO v_sede_2 FROM public.sedes ORDER BY id DESC LIMIT 1;
    IF v_sede_1 IS NULL THEN v_sede_1 := 1; END IF;
    IF v_sede_2 IS NULL THEN v_sede_2 := 1; END IF;

    -- Borrar usuarios previos si existían en auth.users
    DELETE FROM auth.users WHERE email IN ('admin@salon.app', 'maria.garcia@salon.app', 'ana.martinez@salon.app');

    -- Insertar Administradora (Password: admin123)
    INSERT INTO auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
        created_at, updated_at, confirmation_token, recovery_token
    ) VALUES (
        '00000000-0000-0000-0000-000000000000',
        v_admin_id, 'authenticated', 'authenticated', 'admin@salon.app',
        crypt('admin123', gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}',
        '{"nombre":"Administradora","rol":"ADMIN"}',
        now(), now(), '', ''
    );

    INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
    VALUES (
        gen_random_uuid(), v_admin_id,
        jsonb_build_object('sub', v_admin_id::text, 'email', 'admin@salon.app'),
        'email', v_admin_id::text, now(), now(), now()
    );

    INSERT INTO public.perfiles (id, nombre, username, rol, sede_principal_id, activo)
    VALUES (v_admin_id, 'Administradora', 'admin', 'ADMIN', v_sede_1, true)
    ON CONFLICT (id) DO UPDATE SET rol = 'ADMIN';

    -- Insertar María García (Password: 1234)
    INSERT INTO auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
        created_at, updated_at, confirmation_token, recovery_token
    ) VALUES (
        '00000000-0000-0000-0000-000000000000',
        v_maria_id, 'authenticated', 'authenticated', 'maria.garcia@salon.app',
        crypt('1234', gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}',
        '{"nombre":"María García","rol":"TRABAJADORA"}',
        now(), now(), '', ''
    );

    INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
    VALUES (
        gen_random_uuid(), v_maria_id,
        jsonb_build_object('sub', v_maria_id::text, 'email', 'maria.garcia@salon.app'),
        'email', v_maria_id::text, now(), now(), now()
    );

    INSERT INTO public.perfiles (id, nombre, username, rol, sede_principal_id, activo)
    VALUES (v_maria_id, 'María García', 'maria.garcia', 'TRABAJADORA', v_sede_1, true)
    ON CONFLICT (id) DO UPDATE SET rol = 'TRABAJADORA';

    -- Insertar Ana Martínez (Password: 1234)
    INSERT INTO auth.users (
        instance_id, id, aud, role, email, encrypted_password,
        email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
        created_at, updated_at, confirmation_token, recovery_token
    ) VALUES (
        '00000000-0000-0000-0000-000000000000',
        v_ana_id, 'authenticated', 'authenticated', 'ana.martinez@salon.app',
        crypt('1234', gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}',
        '{"nombre":"Ana Martínez","rol":"TRABAJADORA"}',
        now(), now(), '', ''
    );

    INSERT INTO auth.identities (id, user_id, identity_data, provider, provider_id, last_sign_in_at, created_at, updated_at)
    VALUES (
        gen_random_uuid(), v_ana_id,
        jsonb_build_object('sub', v_ana_id::text, 'email', 'ana.martinez@salon.app'),
        'email', v_ana_id::text, now(), now(), now()
    );

    INSERT INTO public.perfiles (id, nombre, username, rol, sede_principal_id, activo)
    VALUES (v_ana_id, 'Ana Martínez', 'ana.martinez', 'TRABAJADORA', v_sede_2, true)
    ON CONFLICT (id) DO UPDATE SET rol = 'TRABAJADORA';

END $$;

-- 7. Asegurar permisos
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO anon, authenticated, service_role;
