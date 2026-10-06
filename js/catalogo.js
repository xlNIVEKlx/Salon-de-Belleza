// ============================================================
// MÓDULO: CATÁLOGO DE SERVICIOS (ADMIN)
// ============================================================

const Catalogo = {
    async cargarLista() {
        const { data, error } = await db
            .from('catalogo_servicios')
            .select('*')
            .order('id', { ascending: true });

        if (error) {
            UI.showAlert('Error al cargar catálogo: ' + error.message, 'error');
            return;
        }

        const tbody = document.getElementById('tabla-catalogo-body');
        if (!tbody) return;

        tbody.innerHTML = (data || []).map(s => `
            <tr class="${s.activo ? '' : 'bg-gray-50 opacity-60'}">
                <td class="px-6 py-4 font-medium text-gray-800">${s.nombre}</td>
                <td class="px-6 py-4">${UI.formatCurrency(s.precio)}</td>
                <td class="px-6 py-4 text-gray-500">${s.porcentaje_comision}%</td>
                <td class="px-6 py-4 text-center">
                    <span class="px-2 py-1 text-xs font-semibold rounded-full ${s.activo ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}">
                        ${s.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td class="px-6 py-4 text-right">
                    ${s.activo ? `
                        <button onclick="Catalogo.editarServicio(${s.id})" class="text-blue-600 hover:text-blue-800 mr-3"><i class="fa-solid fa-edit"></i></button>
                        <button onclick="Catalogo.desactivarServicio(${s.id})" class="text-red-600 hover:text-red-800"><i class="fa-solid fa-trash"></i></button>
                    ` : '<span class="text-xs text-gray-400">Desactivado</span>'}
                </td>
            </tr>
        `).join('');
    },

    async guardar(e) {
        e.preventDefault();
        const id = document.getElementById('servicio-id').value;
        const nombre = document.getElementById('servicio-nombre').value;
        const precio = parseFloat(document.getElementById('servicio-precio').value);
        const porcentaje_comision = parseFloat(document.getElementById('servicio-comision').value);

        const payload = { nombre, precio, porcentaje_comision };

        let result;
        if (id) {
            result = await db.from('catalogo_servicios').update(payload).eq('id', id);
        } else {
            result = await db.from('catalogo_servicios').insert([payload]);
        }

        if (result.error) {
            UI.showAlert('Error al guardar: ' + result.error.message, 'error');
            return;
        }

        UI.showAlert(id ? 'Servicio actualizado' : 'Servicio creado exitosamente');
        document.getElementById('modal-servicio').classList.add('hidden');
        this.cargarLista();
        POS.cargarCatalogo();
    },

    async editarServicio(id) {
        const { data: s, error } = await db
            .from('catalogo_servicios')
            .select('*')
            .eq('id', id)
            .single();

        if (error) {
            UI.showAlert('Error al obtener servicio: ' + error.message, 'error');
            return;
        }

        document.getElementById('servicio-id').value = s.id;
        document.getElementById('servicio-nombre').value = s.nombre;
        document.getElementById('servicio-precio').value = s.precio;
        document.getElementById('servicio-comision').value = s.porcentaje_comision;

        document.getElementById('modal-servicio-title').textContent = 'Editar Servicio';
        document.getElementById('modal-servicio').classList.remove('hidden');
    },

    async desactivarServicio(id) {
        if (!confirm('¿Estás seguro de desactivar este servicio? Ya no aparecerá en el POS.')) return;

        const { error } = await db
            .from('catalogo_servicios')
            .update({ activo: false })
            .eq('id', id);

        if (error) {
            UI.showAlert('Error al desactivar: ' + error.message, 'error');
            return;
        }

        UI.showAlert('Servicio desactivado correctamente');
        this.cargarLista();
        POS.cargarCatalogo();
    }
};
