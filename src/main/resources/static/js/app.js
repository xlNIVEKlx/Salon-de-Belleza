// ==========================================
// ESTADO GLOBAL
// ==========================================
const AppState = {
    user: null,         // Datos del usuario logueado
    sedes: [],          // Lista de sedes activas
    catalogo: []        // Lista de servicios activos
};

// ==========================================
// UTILIDADES & API FETCH
// ==========================================
const API_BASE = '/api';

async function apiFetch(endpoint, options = {}) {
    // Incluir credenciales (cookies de sesión JSESSIONID)
    options.credentials = 'same-origin';
    if (!options.headers) {
        options.headers = { 'Content-Type': 'application/json' };
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        
        // Si el body está vacío (ej. 204 No Content), retornar null
        const contentType = response.headers.get("content-type");
        const isJson = contentType && contentType.includes("application/json");
        const data = isJson ? await response.json() : null;

        if (!response.ok) {
            // Manejar error de autenticación centralizado
            if (response.status === 401 && endpoint !== '/auth/me' && endpoint !== '/auth/login') {
                showLogin();
                showAlert('Sesión expirada. Ingresa de nuevo.', 'error');
                throw new Error('Unauthorized');
            }
            throw new Error(data?.mensaje || `Error HTTP: ${response.status}`);
        }
        return data;
    } catch (err) {
        throw err;
    }
}

// Formateador de moneda colombiana
const formatCurrency = (val) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(val);

// Sistema de alertas flotantes
function showAlert(message, type = 'success') {
    const container = document.getElementById('alert-container');
    const alertEl = document.createElement('div');
    const bgColor = type === 'error' ? 'bg-red-500' : 'bg-green-500';
    
    alertEl.className = `${bgColor} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 transform transition-all translate-x-full opacity-0 duration-300`;
    alertEl.innerHTML = `
        <i class="fa-solid ${type === 'error' ? 'fa-circle-exclamation' : 'fa-check-circle'}"></i>
        <p class="font-medium text-sm">${message}</p>
    `;
    
    container.appendChild(alertEl);
    
    // Animar entrada
    setTimeout(() => { alertEl.classList.remove('translate-x-full', 'opacity-0'); }, 10);
    // Remover luego de 4s
    setTimeout(() => {
        alertEl.classList.add('opacity-0', 'translate-x-full');
        setTimeout(() => alertEl.remove(), 300);
    }, 4000);
}

// ==========================================
// INICIALIZACIÓN
// ==========================================
document.addEventListener('DOMContentLoaded', async () => {
    bindEvents();
    
    try {
        // Verificar sesión activa
        const user = await apiFetch('/auth/me');
        AppState.user = user;
        initializeApp();
    } catch (e) {
        showLogin();
    }
});

// ==========================================
// NAVEGACIÓN Y LOGIN
// ==========================================
function showLogin() {
    document.getElementById('app-view').classList.add('hidden');
    document.getElementById('login-view').classList.remove('hidden');
}

function showApp() {
    document.getElementById('login-view').classList.add('hidden');
    document.getElementById('app-view').classList.remove('hidden');
}

function switchView(viewName) {
    // Ocultar todas las secciones
    document.querySelectorAll('.view-section').forEach(el => el.classList.add('hidden'));
    // Mostrar la seleccionada
    const viewEl = document.getElementById(`view-${viewName}`);
    if(viewEl) viewEl.classList.remove('hidden');
    
    // Actualizar estilo activo en sidebar
    document.querySelectorAll('.nav-link').forEach(el => {
        if(el.dataset.view === viewName) {
            el.classList.add('bg-pink-600', 'text-white');
            el.classList.remove('text-gray-300', 'hover:bg-gray-800');
        } else {
            el.classList.remove('bg-pink-600', 'text-white');
            el.classList.add('text-gray-300', 'hover:bg-gray-800');
        }
    });

    // Cerrar sidebar en móvil
    document.getElementById('sidebar').classList.add('-translate-x-full');
    
    // Cargar datos específicos de la vista
    if (viewName === 'catalogo') loadCatalogoAdmin();
    if (viewName === 'caja') loadCajaSedes();
}

async function initializeApp() {
    showApp();
    
    // Setup Header UI
    document.getElementById('user-name-display').textContent = AppState.user.nombre;
    document.getElementById('user-role-display').textContent = 
        AppState.user.rol === 'ADMIN' ? 'Administradora' : 'Trabajadora';

    // Permisos Admin
    if (AppState.user.rol === 'ADMIN') {
        document.getElementById('menu-admin-sections').classList.remove('hidden');
    }

    // Cargar datos base
    await Promise.all([
        loadSedes(),
        loadCatalogoActivos()
    ]);

    // Ir al POS (Dashboard)
    switchView('dashboard');
    updateDashboardUI();
}

function bindEvents() {
    // Form Login
    document.getElementById('login-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('login-username').value;
        const password = document.getElementById('login-password').value;
        try {
            const data = await apiFetch('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password })
            });
            AppState.user = data;
            initializeApp();
        } catch (e) {
            showAlert(e.message, 'error');
        }
    });

    // Logout
    document.getElementById('btn-logout').addEventListener('click', async () => {
        try {
            await apiFetch('/auth/logout', { method: 'POST' });
            AppState.user = null;
            showLogin();
        } catch (e) {}
    });

    // Menu Navigation
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            switchView(e.currentTarget.dataset.view);
        });
    });

    // Mobile Sidebar toggle
    const sidebar = document.getElementById('sidebar');
    document.getElementById('btn-open-sidebar').addEventListener('click', () => {
        sidebar.classList.remove('-translate-x-full');
    });
    document.getElementById('btn-close-sidebar').addEventListener('click', () => {
        sidebar.classList.add('-translate-x-full');
    });

    // POS Events
    document.getElementById('btn-abrir-jornada').addEventListener('click', handleAbrirJornada);
    document.getElementById('btn-cerrar-jornada').addEventListener('click', handleCerrarJornada);

    // Modal Catálogo
    document.getElementById('btn-nuevo-servicio').addEventListener('click', () => {
        document.getElementById('form-servicio').reset();
        document.getElementById('servicio-id').value = '';
        document.getElementById('modal-servicio-title').textContent = 'Nuevo Servicio';
        document.getElementById('modal-servicio').classList.remove('hidden');
    });
    document.querySelectorAll('.btn-close-modal').forEach(btn => {
        btn.addEventListener('click', () => document.getElementById('modal-servicio').classList.add('hidden'));
    });
    document.getElementById('form-servicio').addEventListener('submit', handleGuardarServicio);

    // Reportes
    const formRep = document.getElementById('form-reporte');
    const hoy = new Date().toISOString().split('T')[0];
    document.getElementById('rep-inicio').value = hoy;
    document.getElementById('rep-fin').value = hoy;
    
    formRep.addEventListener('submit', (e) => {
        e.preventDefault();
        const ini = document.getElementById('rep-inicio').value;
        const fin = document.getElementById('rep-fin').value;
        // Descarga directa abriendo URL (autenticada por cookie)
        window.location.href = `/api/reportes/excel?inicio=${ini}&fin=${fin}`;
    });
}

// ==========================================
// MÓDULO: POS (DASHBOARD)
// ==========================================
async function loadSedes() {
    try {
        AppState.sedes = await apiFetch('/sedes/activas');
        const select = document.getElementById('select-sede-jornada');
        select.innerHTML = AppState.sedes.map(s => 
            `<option value="${s.id}" ${s.id === AppState.user.sedePrincipalId ? 'selected' : ''}>${s.nombre}</option>`
        ).join('');
    } catch (e) {
        console.error("Error al cargar sedes");
    }
}

async function loadCatalogoActivos() {
    try {
        AppState.catalogo = await apiFetch('/catalogo/activos');
        const container = document.getElementById('botones-servicios');
        container.innerHTML = AppState.catalogo.map(s => `
            <button onclick="registrarServicio(${s.id}, '${s.nombre}')" class="pos-btn bg-white border border-pink-100 hover:border-pink-300 p-4 rounded-xl shadow-sm flex flex-col items-center justify-center gap-2 group">
                <div class="bg-pink-50 text-pink-500 rounded-full w-12 h-12 flex items-center justify-center group-hover:bg-pink-100 transition">
                    <i class="fa-solid fa-hand-sparkles text-xl"></i>
                </div>
                <div class="text-center">
                    <p class="font-bold text-gray-800 text-sm leading-tight">${s.nombre}</p>
                    <p class="text-xs text-gray-500 mt-1">${formatCurrency(s.precio)}</p>
                </div>
            </button>
        `).join('');
    } catch (e) {
        console.error("Error al cargar catálogo");
    }
}

function updateDashboardUI() {
    const isAbierta = AppState.user.tieneJornadaAbierta;
    
    if (isAbierta) {
        document.getElementById('estado-jornada-cerrada').classList.add('hidden');
        document.getElementById('estado-jornada-abierta').classList.remove('hidden');
        document.getElementById('pos-container').classList.remove('hidden');
        document.getElementById('lbl-sede-actual').textContent = AppState.user.sedeJornadaActivaNombre;
        
        // Cargar resumen
        refreshResumenDia();
    } else {
        document.getElementById('estado-jornada-cerrada').classList.remove('hidden');
        document.getElementById('estado-jornada-abierta').classList.add('hidden');
        document.getElementById('pos-container').classList.add('hidden');
    }
}

async function handleAbrirJornada() {
    const sedeId = document.getElementById('select-sede-jornada').value;
    try {
        const jor = await apiFetch('/jornadas/abrir', {
            method: 'POST',
            body: JSON.stringify({ sedeId })
        });
        
        AppState.user.tieneJornadaAbierta = true;
        AppState.user.jornadaActivaId = jor.id;
        AppState.user.sedeJornadaActivaNombre = jor.sedeNombre;
        
        showAlert('Jornada abierta exitosamente');
        updateDashboardUI();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function handleCerrarJornada() {
    if (!confirm('¿Seguro que deseas CERRAR TU TURNO de hoy? Ya no podrás registrar más servicios.')) return;
    try {
        await apiFetch(`/jornadas/${AppState.user.jornadaActivaId}/cerrar`, { method: 'POST' });
        
        AppState.user.tieneJornadaAbierta = false;
        AppState.user.jornadaActivaId = null;
        AppState.user.sedeJornadaActivaNombre = null;
        
        showAlert('Turno finalizado correctamente');
        updateDashboardUI();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function registrarServicio(servicioId, nombre) {
    try {
        await apiFetch('/servicios/registrar', {
            method: 'POST',
            body: JSON.stringify({ servicioId: servicioId, cantidad: 1 })
        });
        showAlert(`¡Registrado: 1 x ${nombre}!`);
        refreshResumenDia();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function refreshResumenDia() {
    try {
        const resumen = await apiFetch('/servicios/mi-resumen');
        
        // Update labels
        document.getElementById('lbl-gran-total').textContent = formatCurrency(resumen.granTotalCobrado);
        document.getElementById('lbl-gran-comision').textContent = formatCurrency(resumen.granTotalComision);
        document.getElementById('lbl-gran-caja').textContent = formatCurrency(resumen.granTotalCajaSalon);
        
        // Update list
        const list = document.getElementById('resumen-servicios-lista');
        if (resumen.servicios.length === 0) {
            list.innerHTML = `<p class="text-gray-400 text-sm text-center py-4">Aún no has registrado servicios hoy.</p>`;
        } else {
            list.innerHTML = resumen.servicios.map(s => `
                <div class="bg-gray-50 rounded-lg p-3 border border-gray-100 flex justify-between items-center">
                    <div>
                        <p class="font-bold text-gray-800 text-sm">${s.servicioNombre}</p>
                        <p class="text-xs text-gray-500">Realizados: ${s.cantidadTotal}</p>
                    </div>
                    <div class="text-right">
                        <p class="font-bold text-pink-600 text-sm">${formatCurrency(s.totalCobrado)}</p>
                        <p class="text-xs text-green-600">Comisión: ${formatCurrency(s.totalComision)}</p>
                    </div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error("Error al obtener resumen", e);
    }
}

// ==========================================
// MÓDULO: CATÁLOGO (ADMIN)
// ==========================================
async function loadCatalogoAdmin() {
    try {
        const todos = await apiFetch('/catalogo');
        const tbody = document.getElementById('tabla-catalogo-body');
        
        tbody.innerHTML = todos.map(s => `
            <tr class="${s.activo ? '' : 'bg-gray-50 opacity-60'}">
                <td class="px-6 py-4 font-medium text-gray-800">${s.nombre}</td>
                <td class="px-6 py-4">${formatCurrency(s.precio)}</td>
                <td class="px-6 py-4 text-gray-500">${s.porcentajeComision}%</td>
                <td class="px-6 py-4 text-center">
                    <span class="px-2 py-1 text-xs font-semibold rounded-full ${s.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                        ${s.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    ${s.activo ? `
                        <button onclick="editarServicio(${s.id})" class="text-blue-600 hover:text-blue-800 mr-3"><i class="fa-solid fa-edit"></i></button>
                        <button onclick="desactivarServicio(${s.id})" class="text-red-600 hover:text-red-800"><i class="fa-solid fa-trash"></i></button>
                    ` : '<span class="text-xs text-gray-400">Desactivado</span>'}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function handleGuardarServicio(e) {
    e.preventDefault();
    const id = document.getElementById('servicio-id').value;
    const body = {
        nombre: document.getElementById('servicio-nombre').value,
        precio: parseFloat(document.getElementById('servicio-precio').value),
        porcentajeComision: parseFloat(document.getElementById('servicio-comision').value)
    };

    try {
        if (id) {
            await apiFetch(`/catalogo/${id}`, { method: 'PUT', body: JSON.stringify(body) });
            showAlert('Servicio actualizado correctamente');
        } else {
            await apiFetch('/catalogo', { method: 'POST', body: JSON.stringify(body) });
            showAlert('Servicio creado correctamente');
        }
        
        document.getElementById('modal-servicio').classList.add('hidden');
        loadCatalogoAdmin();
        loadCatalogoActivos(); // Refresh para POS si está activo
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function editarServicio(id) {
    try {
        const s = await apiFetch(`/catalogo/${id}`);
        document.getElementById('servicio-id').value = s.id;
        document.getElementById('servicio-nombre').value = s.nombre;
        document.getElementById('servicio-precio').value = s.precio;
        document.getElementById('servicio-comision').value = s.porcentajeComision;
        
        document.getElementById('modal-servicio-title').textContent = 'Editar Servicio';
        document.getElementById('modal-servicio').classList.remove('hidden');
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

async function desactivarServicio(id) {
    if(!confirm('¿Estás seguro de desactivar este servicio? Ya no aparecerá en el POS.')) return;
    try {
        await apiFetch(`/catalogo/${id}`, { method: 'DELETE' });
        showAlert('Servicio desactivado');
        loadCatalogoAdmin();
        loadCatalogoActivos();
    } catch (e) {
        showAlert(e.message, 'error');
    }
}

// ==========================================
// MÓDULO: CAJA (ADMIN)
// ==========================================
async function loadCajaSedes() {
    try {
        const container = document.getElementById('cajas-sedes-container');
        container.innerHTML = '<div class="col-span-full text-center py-8 text-gray-500"><i class="fa-solid fa-spinner fa-spin text-2xl mb-2"></i><p>Calculando dinero en caja...</p></div>';
        
        let html = '';
        // Consultar cada sede activa
        for (const sede of AppState.sedes) {
            const data = await apiFetch(`/caja/sede/${sede.id}/hoy`);
            html += `
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 relative overflow-hidden">
                    <div class="absolute top-0 right-0 p-4 opacity-10">
                        <i class="fa-solid fa-building text-6xl text-pink-600"></i>
                    </div>
                    
                    <h3 class="text-lg font-bold text-gray-800 mb-1">${data.sedeNombre}</h3>
                    <p class="text-sm text-gray-500 mb-4"><i class="fa-solid fa-user-tie text-pink-500 mr-1"></i> Trabajadoras activas ahora: <b>${data.trabajadorasActivas}</b></p>
                    
                    <div class="space-y-3 mt-4">
                        <div class="flex justify-between items-center border-b pb-2">
                            <span class="text-sm text-gray-500">Total Producido Hoy</span>
                            <span class="font-bold text-gray-800">${formatCurrency(data.totalProducido)}</span>
                        </div>
                        <div class="flex justify-between items-center border-b pb-2">
                            <span class="text-sm text-gray-500">A pagar (Comisiones)</span>
                            <span class="font-bold text-green-600">${formatCurrency(data.totalComisiones)}</span>
                        </div>
                        <div class="flex justify-between items-center bg-pink-50 p-3 rounded-lg border border-pink-100">
                            <span class="font-bold text-gray-800">Efectivo en Caja (Neto)</span>
                            <span class="font-bold text-pink-600 text-xl">${formatCurrency(data.totalCajaSalon)}</span>
                        </div>
                    </div>
                </div>
            `;
        }
        container.innerHTML = html;
        
    } catch (e) {
        showAlert(e.message, 'error');
    }
}
