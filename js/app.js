// ============================================================
// APLICACIÓN PRINCIPAL & ORQUESTADOR
// ============================================================

const App = {
    sedes: [],

    async init() {
        this.setupTouchAndSidebar();
        this.bindEvents();

        // Verificar si ya hay una sesión iniciada
        const profile = await Auth.checkSession();
        if (profile) {
            this.iniciarApp();
        } else {
            this.mostrarLogin();
        }
    },

    mostrarLogin() {
        document.getElementById('app-view').classList.add('hidden');
        document.getElementById('login-view').classList.remove('hidden');
    },

    mostrarApp() {
        document.getElementById('login-view').classList.add('hidden');
        document.getElementById('app-view').classList.remove('hidden');
    },

    async iniciarApp() {
        this.mostrarApp();

        // Mostrar datos de usuario en la barra lateral
        document.getElementById('user-name-display').textContent = Auth.currentProfile.nombre;
        document.getElementById('user-role-display').textContent = Auth.isAdmin() ? 'Administradora' : 'Trabajadora';

        // Mostrar u ocultar opciones exclusivas de Admin
        const adminSections = document.getElementById('menu-admin-sections');
        if (Auth.isAdmin()) {
            adminSections.classList.remove('hidden');
        } else {
            adminSections.classList.add('hidden');
        }

        // Cargar sedes activas
        await this.cargarSedes();

        // Iniciar POS
        await POS.cargarCatalogo();
        await POS.verificarJornadaActiva();

        // Ir a la vista por defecto (Dashboard POS)
        this.cambiarVista('dashboard');
    },

    async cargarSedes() {
        const { data, error } = await db
            .from('sedes')
            .select('*')
            .eq('activa', true)
            .order('id');

        if (error) {
            console.error('Error al cargar sedes:', error);
            return;
        }

        this.sedes = data || [];

        const select = document.getElementById('select-sede-jornada');
        if (select) {
            const sedePrincipalId = Auth.currentProfile?.sede_principal_id;
            select.innerHTML = this.sedes.map(s =>
                `<option value="${s.id}" ${s.id === sedePrincipalId ? 'selected' : ''}>${s.nombre}</option>`
            ).join('');
        }
    },

    cambiarVista(nombreVista) {
        // Desactivar canal Realtime si salimos de la vista de caja
        if (nombreVista !== 'caja') {
            Caja.desactivarRealtime();
        }

        // Ocultar todas las secciones
        document.querySelectorAll('.view-section').forEach(el => el.classList.add('hidden'));

        // Mostrar la vista seleccionada
        const target = document.getElementById(`view-${nombreVista}`);
        if (target) target.classList.remove('hidden');

        // Actualizar navegación activa
        document.querySelectorAll('.nav-link').forEach(link => {
            if (link.dataset.view === nombreVista) {
                link.classList.add('bg-pink-600', 'text-white');
                link.classList.remove('text-gray-300', 'hover:bg-gray-800');
            } else {
                link.classList.remove('bg-pink-600', 'text-white');
                link.classList.add('text-gray-300', 'hover:bg-gray-800');
            }
        });

        // Cerrar sidebar en dispositivos móviles
        this.cerrarSidebar();

        // Inicializar vistas según corresponda
        if (nombreVista === 'catalogo') Catalogo.iniciar();
        if (nombreVista === 'caja') Caja.iniciar(this.sedes);
    },

    /**
     * Solución robusta de eventos Touch y Click para el Sidebar Móvil
     */
    setupTouchAndSidebar() {
        const sidebar = document.getElementById('sidebar');
        const backdrop = document.getElementById('sidebar-backdrop');
        const btnOpen = document.getElementById('btn-open-sidebar');
        const btnClose = document.getElementById('btn-close-sidebar');

        const abrir = (e) => {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            sidebar.classList.add('open');
            backdrop.classList.add('active');
        };

        const cerrar = (e) => {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            sidebar.classList.remove('open');
            backdrop.classList.remove('active');
        };

        this.cerrarSidebar = cerrar;

        // Botón abrir: soporte táctil y click
        if (btnOpen) {
            btnOpen.addEventListener('touchend', abrir, { passive: false });
            btnOpen.addEventListener('click', abrir);
        }

        // Botón cerrar: soporte táctil y click
        if (btnClose) {
            btnClose.addEventListener('touchend', cerrar, { passive: false });
            btnClose.addEventListener('click', cerrar);
        }

        // Tocar fuera del sidebar en móvil cierra el menú
        if (backdrop) {
            backdrop.addEventListener('touchend', cerrar, { passive: false });
            backdrop.addEventListener('click', cerrar);
        }
    },

    bindEvents() {
        // Formulario de login
        const loginForm = document.getElementById('login-form');
        if (loginForm) {
            loginForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const username = document.getElementById('login-username').value;
                const password = document.getElementById('login-password').value;

                try {
                    await Auth.signIn(username, password);
                    UI.showAlert(`¡Bienvenida ${Auth.currentProfile.nombre}!`);
                    this.iniciarApp();
                } catch (err) {
                    UI.showAlert(err.message, 'error');
                }
            });
        }

        // Botón Logout
        const btnLogout = document.getElementById('btn-logout');
        if (btnLogout) {
            btnLogout.addEventListener('click', async () => {
                await Auth.signOut();
                this.mostrarLogin();
            });
        }

        // Navegación entre vistas
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                this.cambiarVista(link.dataset.view);
            });
        });

        // Eventos POS
        const btnAbrirJor = document.getElementById('btn-abrir-jornada');
        if (btnAbrirJor) btnAbrirJor.addEventListener('click', () => POS.abrirJornada());

        const btnCerrarJor = document.getElementById('btn-cerrar-jornada');
        if (btnCerrarJor) btnCerrarJor.addEventListener('click', () => POS.cerrarJornada());

        // Eventos Catálogo Modal
        const btnNuevoServ = document.getElementById('btn-nuevo-servicio');
        if (btnNuevoServ) {
            btnNuevoServ.addEventListener('click', () => {
                document.getElementById('form-servicio').reset();
                document.getElementById('servicio-id').value = '';
                document.getElementById('modal-servicio-title').textContent = 'Nuevo Servicio';
                document.getElementById('modal-servicio').classList.remove('hidden');
            });
        }

        document.querySelectorAll('.btn-close-modal').forEach(btn => {
            btn.addEventListener('click', () => {
                document.getElementById('modal-servicio').classList.add('hidden');
            });
        });

        const formServicio = document.getElementById('form-servicio');
        if (formServicio) formServicio.addEventListener('submit', (e) => Catalogo.guardar(e));

        // Eventos Reportes
        const formRep = document.getElementById('form-reporte');
        if (formRep) {
            const hoy = new Date().toISOString().split('T')[0];
            const repInicio = document.getElementById('rep-inicio');
            const repFin = document.getElementById('rep-fin');
            if (repInicio) repInicio.value = hoy;
            if (repFin) repFin.value = hoy;

            formRep.addEventListener('submit', (e) => Reportes.descargarExcel(e));
        }
    }
};

// Iniciar al cargar el DOM
document.addEventListener('DOMContentLoaded', () => App.init());
