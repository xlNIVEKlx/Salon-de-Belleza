// ============================================================
// MÓDULO: CATÁLOGO DE SERVICIOS
// Permite gestionar servicios con precios independientes por sede
// ============================================================

const Catalogo = {
    servicios: [],

    async iniciar() {
        this.vincularEventos();
        await this.cargarServicios();
    },

    vincularEventos() {
        // Botón nuevo servicio
        const btnNuevo = document.getElementById('btn-nuevo-servicio');
        if (btnNuevo) btnNuevo.addEventListener('click', () => this.abrirModal());

        // Botones cerrar modal
        document.querySelectorAll('.btn-close-modal').forEach(btn => {
            btn.addEventListener('click', () => this.cerrarModal());
        });

        // Formulario de guardado
        const form = document.getElementById('form-servicio');
        if (form) {
            form.addEventListener('submit', async (e) => {
                e.preventDefault();
                await this.guardarServicio();
            });
        }
    },

    async cargarServicios() {
        const { data, error } = await db.from('catalogo_servicios').select('*').order('nombre');
        if (error) {
            console.error('Error al cargar catálogo:', error);
            UI.showAlert('Error al cargar el catálogo de servicios', 'error');
            return;
        }
        this.servicios = data || [];
        this.renderizarTabla();
    },

    renderizarTabla() {
        const tbody = document.getElementById('tabla-catalogo-body');
        if (!tbody) return;

        tbody.innerHTML = this.servicios.map(s => {
            // Extraer precios independientes o usar el precio base por defecto
            const p = s.precios_por_sede || {};

            // Crear el resumen visual de precios por sede en la tabla
            const preciosResumen = `
                <div class="text-xs text-gray-500 space-y-0.5">
                    <div><span class="font-medium text-gray-700">Aero:</span> ${UI.formatCurrency(p["1"] || s.precio)}</div>
                    <div><span class="font-medium text-gray-700">Torc:</span> ${UI.formatCurrency(p["2"] || s.precio)}</div>
                    <div><span class="font-medium text-gray-700">Pat:</span> ${UI.formatCurrency(p["3"] || s.precio)}</div>
                    <div><span class="font-medium text-gray-700">Chap:</span> ${UI.formatCurrency(p["4"] || s.precio)}</div>
                </div>
            `;

            return `
            <tr class="hover:bg-pink-50/30 transition border-b border-gray-50 last:border-0">
                <td class="px-6 py-4 font-bold text-gray-800">${s.nombre}</td>
                <td class="px-6 py-4">${preciosResumen}</td>
                <td class="px-6 py-4 font-medium text-pink-600">${s.comision_porcentaje}%</td>
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
    },

    abrirModal(id = null) {
        const form = document.getElementById('form-servicio');
        form.reset();
        document.getElementById('servicio-id').value = '';
        document.getElementById('modal-servicio-title').innerText = id ? 'Editar Servicio' : 'Nuevo Servicio';

        if (id) {
            // Forzar comparación como texto para evitar errores de tipo si el ID es numérico o UUID
            const s = this.servicios.find(x => String(x.id) === String(id));
            if (s) {
                document.getElementById('servicio-id').value = s.id;
                document.getElementById('servicio-nombre').value = s.nombre;
                document.getElementById('servicio-comision').value = s.comision_porcentaje;

                // Llenar casillas individualmente sin que se mezclen
                const p = s.precios_por_sede || {};
                document.getElementById('precio-sede-1').value = p["1"] || s.precio || '';
                document.getElementById('precio-sede-2').value = p["2"] || s.precio || '';
                document.getElementById('precio-sede-3').value = p["3"] || s.precio || '';
                document.getElementById('precio-sede-4').value = p["4"] || s.precio || '';
            }
        }

        document.getElementById('modal-servicio').classList.remove('hidden');
    },

    cerrarModal() {
        document.getElementById('modal-servicio').classList.add('hidden');
    },

    async guardarServicio() {
        const id = document.getElementById('servicio-id').value;
        const p1 = Number(document.getElementById('precio-sede-1').value);

        // Empaquetar los datos aislando el precio de cada sede
        const payload = {
            nombre: document.getElementById('servicio-nombre').value,
            comision_porcentaje: Number(document.getElementById('servicio-comision').value),
            precio: p1, // Mantenemos el de la sede 1 como referencia general
            precios_por_sede: {
                "1": p1,
                "2": Number(document.getElementById('precio-sede-2').value),
                "3": Number(document.getElementById('precio-sede-3').value),
                "4": Number(document.getElementById('precio-sede-4').value)
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
            UI.showAlert('Error al guardar el servicio', 'error');
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
            message: '¿Estás segura de que deseas eliminar este servicio de todas las sedes?',
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