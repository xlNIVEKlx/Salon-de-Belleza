// ============================================================
// MÓDULO: CATÁLOGO DE SERVICIOS
// Gestión de servicios con precios por sede y enteros limpios
// ============================================================

const Catalogo = {
    servicios: [],

    async iniciar() {
        this.vincularEventos();
        await this.cargarServicios();
    },

    vincularEventos() {
        const btnNuevo = document.getElementById('btn-nuevo-servicio');
        if (btnNuevo) btnNuevo.addEventListener('click', () => this.abrirModal());

        document.querySelectorAll('.btn-close-modal').forEach(btn => {
            btn.addEventListener('click', () => this.cerrarModal());
        });

        const form = document.getElementById('form-servicio');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this.guardarServicio();
            });
        }
    },

    async cargarServicios() {
        try {
            const { data, error } = await db.from('catalogo_servicios').select('*').order('nombre');
            if (error) throw error;
            this.servicios = data || [];
            this.renderizarTabla();
        } catch (err) {
            console.error('Error al cargar catálogo:', err);
            UI.showAlert('Error al cargar el catálogo: ' + err.message, 'error');
        }
    },

    renderizarTabla() {
        const tbody = document.getElementById('tabla-catalogo-body');
        if (!tbody) return;

        if (this.servicios.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="px-6 py-8 text-center text-gray-400">No hay servicios registrados en el catálogo.</td></tr>';
            return;
        }

        // Detectar sede actual del POS o usar Sede 1 por defecto
        let sedeActual = "1";
        if (typeof POS !== 'undefined' && POS.jornadaActiva && POS.jornadaActiva.sede_id) {
            sedeActual = String(POS.jornadaActiva.sede_id);
        }

        const nombresSedes = {
            "1": "Aeropuerto",
            "2": "Torcoroma",
            "3": "Patios",
            "4": "Chapinero"
        };

        try {
            tbody.innerHTML = this.servicios.map(s => {
                const p = s.precios_por_sede || {};
                const precioSedeActual = p[sedeActual] ?? s.precio ?? 0;
                const nombreSedeTexto = nombresSedes[sedeActual] || "Sede";

                // Comisión estricta en formato entero (sin decimales)
                const comisionEntera = Math.round(Number(s.porcentaje_comision ?? 0));

                return `
                <tr class="hover:bg-pink-50/30 transition border-b border-gray-50 last:border-0">
                    <td class="px-6 py-4 font-bold text-gray-800">${s.nombre || 'Sin nombre'}</td>
                    <td class="px-6 py-4">
                        <div class="text-base font-bold text-gray-900">${UI.formatCurrency(precioSedeActual)}</div>
                        <div class="text-xs text-pink-600 font-medium">Sede: ${nombreSedeTexto}</div>
                    </td>
                    <td class="px-6 py-4 font-medium text-pink-600">${comisionEntera}%</td>
                    <td class="px-6 py-4 text-center">
                        <span class="px-2 py-1 bg-green-100 text-green-700 text-xs rounded-lg font-bold">Activo</span>
                    </td>
                    <td class="px-6 py-4 text-right space-x-3">
                        <button onclick="Catalogo.abrirModal('${s.id}')" class="text-blue-500 hover:text-blue-700 transition" title="Editar">
                            <i class="fa-solid fa-pen-to-square text-lg"></i>
                        </button>
                        <button onclick="Catalogo.eliminarServicio('${s.id}')" class="text-red-500 hover:text-red-700 transition" title="Eliminar">
                            <i class="fa-solid fa-trash-can text-lg"></i>
                        </button>
                    </td>
                </tr>`;
            }).join('');
        } catch (err) {
            console.error('Error dibujando la tabla:', err);
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-red-500 py-4">Ocurrió un error al mostrar los servicios.</td></tr>';
        }
    },

    abrirModal(id = null) {
        const form = document.getElementById('form-servicio');
        form.reset();
        document.getElementById('servicio-id').value = '';
        document.getElementById('modal-servicio-title').innerText = id ? 'Editar Servicio y Precios por Sede' : 'Nuevo Servicio';

        if (id) {
            const s = this.servicios.find(x => String(x.id) === String(id));
            if (s) {
                document.getElementById('servicio-id').value = s.id;
                document.getElementById('servicio-nombre').value = s.nombre;

                // Cargar comisión como número entero limpio en el input
                document.getElementById('servicio-comision').value = Math.round(Number(s.porcentaje_comision ?? 0));

                const p = s.precios_por_sede || {};
                document.getElementById('precio-sede-1').value = p["1"] ?? s.precio ?? '';
                document.getElementById('precio-sede-2').value = p["2"] ?? s.precio ?? '';
                document.getElementById('precio-sede-3').value = p["3"] ?? s.precio ?? '';
                document.getElementById('precio-sede-4').value = p["4"] ?? s.precio ?? '';
            }
        }

        document.getElementById('modal-servicio').classList.remove('hidden');
    },

    cerrarModal() {
        document.getElementById('modal-servicio').classList.add('hidden');
    },

    async guardarServicio() {
        const id = document.getElementById('servicio-id').value;
        const p1 = Number(document.getElementById('precio-sede-1').value) || 0;

        // Capturar comisión estrictamente como entero
        const comisionValor = parseInt(document.getElementById('servicio-comision').value, 10) || 0;

        const payload = {
            nombre: document.getElementById('servicio-nombre').value,
            porcentaje_comision: comisionValor,
            precio: p1,
            precios_por_sede: {
                "1": p1,
                "2": Number(document.getElementById('precio-sede-2').value) || 0,
                "3": Number(document.getElementById('precio-sede-3').value) || 0,
                "4": Number(document.getElementById('precio-sede-4').value) || 0
            },
            activo: true
        };

        let res;
        if (id) {
            res = await db.from('catalogo_servicios').update(payload).eq('id', id);
        } else {
            res = await db.from('catalogo_servicios').insert([payload]);
        }

        if (res.error) {
            console.error('Error al guardar servicio:', res.error);
            UI.showAlert('Error al guardar: ' + res.error.message, 'error');
        } else {
            UI.showAlert('Servicio guardado exitosamente', 'success');
            this.cerrarModal();
            this.cargarServicios();
        }
    },

    async eliminarServicio(id) {
        const servicio = this.servicios.find(x => String(x.id) === String(id));
        if (!servicio) return;

        const confirmar = await UI.confirm({
            title: 'Eliminar Servicio',
            message: '¿Estás segura de que deseas eliminar este servicio?',
            itemName: servicio.nombre,
            confirmText: 'Eliminar'
        });

        if (!confirmar) return;

        const { error } = await db.from('catalogo_servicios').delete().eq('id', id);

        if (error) {
            console.error('Error al eliminar:', error);
            UI.showAlert('Error al eliminar el servicio', 'error');
        } else {
            UI.showAlert('Servicio eliminado', 'success');
            this.cargarServicios();
        }
    }
};