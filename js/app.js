// ============================================================
// MÓDULO PRINCIPAL: APP / ROUTER
// Gestiona la autenticación, navegación en vivo y vistas
// ============================================================

const App = {
    sedes: [],

    async iniciar() {
        // 1. Verificar si hay una sesión activa en Supabase
        const { data: { session }, error } = await db.auth.getSession();

        if (error || !session) {
            // Si no hay sesión, redirigir al login o mostrar pantalla de acceso
            this.mostrarLogin();
            return;
        }

        // Si hay sesión activa, cargar el sistema normal
        this.mostrarSistema();
        await this.cargarSedesGlobales();
        this.vincularNavegacion();

        // Vista inicial por defecto
        this.cambiarVista('pos');
    },

    mostrarLogin() {
        const loginContainer = document.getElementById('login-container');
        const appContainer = document.getElementById('app-container');

        if (loginContainer) loginContainer.classList.remove('hidden');
        if (appContainer) appContainer.classList.add('hidden');
    },

    mostrarSistema() {
        const loginContainer = document.getElementById('login-container');
        const appContainer = document.getElementById('app-container');

        if (loginContainer) loginContainer.classList.add('hidden');
        if (appContainer) appContainer.classList.remove('hidden');
    },

    async cargarSedesGlobales() {
        try {
            const { data, error } = await db.from('sedes').select('*').order('id');
            if (error) throw error;
            this.sedes = data || [];
        } catch (err) {
            console.error('Error al cargar sedes globales:', err);
        }
    },

    vincularNavegacion() {
        document.querySelectorAll('[data-view]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const vista = item.getAttribute('data-view');
                this.cambiarVista(vista);
            });
        });
    },

    cambiarVista(nombreVista) {
        document.querySelectorAll('.view-section').forEach(el => {
            el.classList.add('hidden');
        });

        const vistaActiva = document.getElementById(`view-${nombreVista}`);
        if (vistaActiva) {
            vistaActiva.classList.remove('hidden');
        }

        document.querySelectorAll('.nav-item').forEach(el => {
            el.classList.remove('active', 'bg-pink-100', 'text-pink-600');
        });
        const navBtn = document.querySelector(`[data-view="${nombreVista}"]`);
        if (navBtn) {
            navBtn.classList.add('active', 'bg-pink-100', 'text-pink-600');
        }

        // Refrescar datos en vivo según la pestaña
        if (nombreVista === 'catalogo') {
            if (typeof Catalogo !== 'undefined') Catalogo.iniciar();
        } else if (nombreVista === 'pos') {
            if (typeof POS !== 'undefined') {
                POS.cargarCatalogo();
                POS.verificarJornadaActiva();
            }
        } else if (nombreVista === 'caja') {
            if (typeof Caja !== 'undefined') Caja.iniciar(this.sedes);
        }
    }
};

// Escuchar cambios de autenticación de Supabase en tiempo real
if (typeof db !== 'undefined' && db.auth) {
    db.auth.onAuthStateChange((event, session) => {
        if (event === 'SIGNED_IN') {
            App.iniciar();
        } else if (event === 'SIGNED_OUT') {
            App.mostrarLogin();
        }
    });
}

// Arrancar la aplicación automáticamente al cargar el documento
document.addEventListener('DOMContentLoaded', () => {
    App.iniciar();
});