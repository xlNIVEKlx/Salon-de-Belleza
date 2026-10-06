// ============================================================
// MÓDULO PRINCIPAL: APP / ROUTER
// Gestiona la navegación en vivo y la actualización automática de vistas
// ============================================================

const App = {
    sedes: [],

    async iniciar() {
        await this.cargarSedesGlobales();
        this.vincularNavegacion();

        // Vista inicial por defecto al entrar al sistema
        this.cambiarVista('pos');
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
        // Escuchar clics en los elementos del menú que tengan data-view
        document.querySelectorAll('[data-view]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const vista = item.getAttribute('data-view');
                this.cambiarVista(vista);
            });
        });
    },

    cambiarVista(nombreVista) {
        // 1. Ocultar todas las secciones de vistas
        document.querySelectorAll('.view-section').forEach(el => {
            el.classList.add('hidden');
        });

        // 2. Mostrar la sección seleccionada
        const vistaActiva = document.getElementById(`view-${nombreVista}`);
        if (vistaActiva) {
            vistaActiva.classList.remove('hidden');
        }

        // 3. Actualizar estilos visuales del menú activo
        document.querySelectorAll('.nav-item').forEach(el => {
            el.classList.remove('active', 'bg-pink-100', 'text-pink-600');
        });
        const navBtn = document.querySelector(`[data-view="${nombreVista}"]`);
        if (navBtn) {
            navBtn.classList.add('active', 'bg-pink-100', 'text-pink-600');
        }

        // 4. Inicializar o refrescar datos EN VIVO según la pestaña seleccionada
        if (nombreVista === 'catalogo') {
            if (typeof Catalogo !== 'undefined') {
                Catalogo.iniciar();
            }
        } else if (nombreVista === 'pos') {
            if (typeof POS !== 'undefined') {
                POS.cargarCatalogo();
                POS.verificarJornadaActiva();
            }
        } else if (nombreVista === 'caja') {
            if (typeof Caja !== 'undefined') {
                Caja.iniciar(this.sedes);
            }
        }
    }
};

// Arrancar la aplicación automáticamente al cargar el documento
document.addEventListener('DOMContentLoaded', () => {
    App.iniciar();
});