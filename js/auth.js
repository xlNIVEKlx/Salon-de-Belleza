// ============================================================
// MÓDULO DE AUTENTICACIÓN FLEXIBLE
// Acepta username (ej: "admin") o email completo ("admin@salon.app")
// ============================================================

const Auth = {
    currentUser: null,
    currentProfile: null,

    /**
     * Inicia sesión con username o email + contraseña
     */
    async signIn(inputUsuario, password) {
        const inputLimpio = inputUsuario.toLowerCase().trim();
        // Si el usuario no escribió un arroba, le agregamos el dominio por defecto @salon.app
        const email = inputLimpio.includes('@') ? inputLimpio : `${inputLimpio}@salon.app`;

        const { data, error } = await db.auth.signInWithPassword({
            email,
            password
        });

        if (error) {
            console.error('Error de Supabase Auth:', error);
            if (error.message.includes('Invalid login credentials')) {
                throw new Error('Usuario o contraseña incorrectos. Verifica que la contraseña sea correcta en Supabase.');
            }
            if (error.message.includes('Email not confirmed')) {
                throw new Error('El correo no ha sido confirmado en Supabase Auth.');
            }
            throw new Error(error.message);
        }

        // Buscar el perfil
        let { data: profile, error: profErr } = await db
            .from('perfiles')
            .select('*, sedes:sede_principal_id(id, nombre)')
            .eq('id', data.user.id)
            .maybeSingle();

        // Si el perfil no existe aún, crearlo automáticamente para evitar bloqueos
        if (!profile) {
            const { data: primerSede } = await db.from('sedes').select('id').limit(1).single();
            const sedeId = primerSede ? primerSede.id : 1;
            const rol = inputLimpio.includes('admin') ? 'ADMIN' : 'TRABAJADORA';
            const nombre = data.user.user_metadata?.nombre || (inputLimpio.charAt(0).toUpperCase() + inputLimpio.slice(1));

            const { data: nuevoPerfil, error: insertErr } = await db
                .from('perfiles')
                .insert([{
                    id: data.user.id,
                    nombre: nombre,
                    username: inputLimpio.split('@')[0],
                    rol: rol,
                    sede_principal_id: sedeId,
                    activo: true
                }])
                .select('*, sedes:sede_principal_id(id, nombre)')
                .single();

            if (insertErr) {
                console.warn('Aviso al autogenerar perfil:', insertErr.message);
            }
            profile = nuevoPerfil || {
                id: data.user.id,
                nombre: nombre,
                rol: rol,
                sede_principal_id: sedeId,
                activo: true
            };
        }

        if (profile && !profile.activo) {
            await db.auth.signOut();
            throw new Error('Tu usuario está inactivo en el sistema.');
        }

        this.currentUser = data.user;
        this.currentProfile = profile;
        return profile;
    },

    async signOut() {
        await db.auth.signOut();
        this.currentUser = null;
        this.currentProfile = null;
    },

    async checkSession() {
        const { data: { session } } = await db.auth.getSession();
        if (!session) return null;

        const { data: profile, error } = await db
            .from('perfiles')
            .select('*, sedes:sede_principal_id(id, nombre)')
            .eq('id', session.user.id)
            .maybeSingle();

        if (error || !profile) {
            // Si hay sesión pero no perfil, crear objeto básico
            this.currentUser = session.user;
            this.currentProfile = {
                id: session.user.id,
                nombre: session.user.email.split('@')[0],
                rol: session.user.email.includes('admin') ? 'ADMIN' : 'TRABAJADORA',
                activo: true
            };
            return this.currentProfile;
        }

        this.currentUser = session.user;
        this.currentProfile = profile;
        return profile;
    },

    isAdmin() {
        return this.currentProfile?.rol === 'ADMIN';
    },

    getUserId() {
        return this.currentUser?.id;
    }
};
