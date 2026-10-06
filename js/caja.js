const Caja = {
    realtimeSubscription: null,
    sedes: [],
    sedeSeleccionada: 'todas',
    fecha: null,
    ultimoResumen: [],

    // Fecha de hoy en hora de Colombia (YYYY-MM-DD)
    hoy() {
        return new Date().toLocaleDateString('en-CA', { timeZone: 'America/Bogota' });
    },

    async iniciar(sedes) {
        this.sedes = sedes || [];
        if (!this.fecha) this.fecha = this.hoy();
        this.dibujarControles();
        await this.cargarCajaSedes();
        this.activarRealtime();
    },

    // ---------- SOLO ADMIN: ver y borrar ventas de la sede y fecha elegidas ----------
    ventas: [],

    esc(texto) {
        return String(texto ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
        }[c]));
    },

    async cargarVentasAdmin(sedesAMostrar) {
        let cont = document.getElementById('caja-ventas-admin');

        if (!Auth.isAdmin()) {
            if (cont) cont.remove();
            return;
        }

        if (!cont) {
            cont = document.createElement('div');
            cont.id = 'caja-ventas-admin';
            cont.className = 'bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-4';
            document.getElementById('cajas-sedes-container').insertAdjacentElement('afterend', cont);
        }

        if (sedesAMostrar.length !== 1) {
            this.ventas = [];
            cont.innerHTML = '<p class="text-sm text-gray-500">Elige una sola sede para ver y borrar sus ventas.</p>';
            return;
        }

        const sede = sedesAMostrar[0];
        const desde = new Date(`${this.fecha}T00:00:00-05:00`);
        const hasta = new Date(desde.getTime() + 24 * 60 * 60 * 1000);

        // 1. Jornadas de la sede
        const { data: jornadas, error: errJ } = await db
            .from('jornadas')
            .select('id, usuario_id, estado')
            .eq('sede_id', sede.id);
        if (errJ) {
            console.error('Error al cargar jornadas:', errJ);
            cont.innerHTML = '<p class="text-sm text-red-600">No se pudieron cargar las ventas.</p>';
            return;
        }
        const mapaJornadas = Object.fromEntries((jornadas || []).map(j => [j.id, j]));
        const idsJornadas = Object.keys(mapaJornadas).map(Number);

        // 2. Ventas de ese día (hora de Colombia)
        let ventas = [];
        if (idsJornadas.length) {
            const { data, error } = await db
                .from('servicios_realizados')
                .select('id, jornada_id, servicio_id, cantidad, total_cobrado, comision_empleada, fecha_registro')
                .in('jornada_id', idsJornadas)
                .gte('fecha_registro', desde.toISOString())
                .lt('fecha_registro', hasta.toISOString())
                .order('fecha_registro', { ascending: true });
            if (error) {
                console.error('Error al cargar ventas:', error);
                cont.innerHTML = '<p class="text-sm text-red-600">No se pudieron cargar las ventas.</p>';
                return;
            }
            ventas = data || [];
        }

        // 3. Nombres de servicios y de trabajadoras
        const idsServ = [...new Set(ventas.map(v => v.servicio_id))];
        const idsUsr = [...new Set(ventas.map(v => mapaJornadas[v.jornada_id]?.usuario_id).filter(Boolean))];
        const nombresServ = {};
        const nombresUsr = {};
        if (idsServ.length) {
            const { data } = await db.from('catalogo_servicios').select('id, nombre').in('id', idsServ);
            (data || []).forEach(s => { nombresServ[s.id] = s.nombre; });
        }
        if (idsUsr.length) {
            const { data } = await db.from('perfiles').select('id, nombre').in('id', idsUsr);
            (data || []).forEach(p => { nombresUsr[p.id] = p.nombre; });
        }

        this.ventas = ventas.map(v => {
            const j = mapaJornadas[v.jornada_id] || {};
            return {
                ...v,
                servicio: nombresServ[v.servicio_id] || 'Servicio',
                trabajadora: nombresUsr[j.usuario_id] || 'Trabajadora',
                cerrada: j.estado === 'CERRADA'
            };
        });

        // 4. Dibujar la lista
        const filas = this.ventas.map(v => {
            const hora = new Date(v.fecha_registro).toLocaleTimeString('es-CO', {
                timeZone: 'America/Bogota', hour: '2-digit', minute: '2-digit'
            });
            return `
                <div class="flex justify-between items-center bg-gray-50 rounded-lg p-3 border border-gray-100">
                    <div>
                        <p class="font-bold text-gray-800 text-sm">${this.esc(v.servicio)}${v.cantidad > 1 ? ' x' + v.cantidad : ''}</p>
                        <p class="text-xs text-gray-500">${hora} · ${this.esc(v.trabajadora)}${v.cerrada ? ' · Turno cerrado' : ''}</p>
                    </div>
                    <div class="flex items-center gap-3">
                        <div class="text-right">
                            <p class="font-bold text-pink-600 text-sm">${UI.formatCurrency(v.total_cobrado)}</p>
                            <p class="text-xs text-green-600">Comisión: ${UI.formatCurrency(v.comision_empleada)}</p>
                        </div>
                        <button onclick="Caja.borrarVenta(${v.id})"
                            class="text-gray-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition"
                            title="Eliminar esta venta">
                            <i class="fa-solid fa-trash-can text-sm"></i>
                        </button>
                    </div>
                </div>`;
        }).join('');

        cont.innerHTML = `
            <h3 class="text-lg font-bold text-gray-800 mb-1">Ventas de ${this.esc(sede.nombre)}</h3>
            <p class="text-sm text-gray-500 mb-4">${this.fecha}</p>
            <div class="space-y-2">${filas || '<p class="text-sm text-gray-400">No hay ventas en esta fecha.</p>'}</div>`;
    },

    async borrarVenta(id) {
        const v = this.ventas.find(x => x.id === id);
        if (!v) return;

        const confirmar = await UI.confirm({
            title: 'Eliminar venta',
            message: '¿Seguro que quieres eliminar esta venta?',
            itemName: `${v.servicio} - ${UI.formatCurrency(v.total_cobrado)}`,
            confirmText: 'Eliminar'
        });
        if (!confirmar) return;

        const { data, error } = await db
            .from('servicios_realizados')
            .delete()
            .eq('id', id)
            .select();

        if (error) {
            console.error('Error al eliminar venta:', error);
            UI.showAlert('Error al eliminar: ' + error.message, 'error');
            return;
        }
        if (!data || data.length === 0) {
            UI.showAlert('No se pudo eliminar, revisa permisos', 'error');
            return;
        }

        UI.showAlert('Venta eliminada', 'success');
        await this.cargarCajaSedes();
    }

};

/**
 * Dibuja (una sola vez) la barra con sede, fecha y botón de Excel
 */
dibujarControles() {
    const container = document.getElementById('cajas-sedes-container');
    if (!container || document.getElementById('caja-controles')) return;

    const opciones = ['<option value="todas">Todas las sedes</option>']
        .concat(this.sedes.map(s => `<option value="${s.id}">${s.nombre}</option>`))
        .join('');

    const barra = document.createElement('div');
    barra.id = 'caja-controles';
    barra.className = 'flex flex-wrap items-end gap-3 mb-4';
    barra.innerHTML = `
            <div>
                <label class="block text-xs text-gray-500 mb-1">Sede</label>
                <select id="caja-sede" class="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white">${opciones}</select>
            </div>
            <div>
                <label class="block text-xs text-gray-500 mb-1">Fecha</label>
                <input id="caja-fecha" type="date" value="${this.fecha}" max="${this.hoy()}"
                       class="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white">
            </div>
        `;
    container.parentElement.insertBefore(barra, container);

    document.getElementById('caja-sede').addEventListener('change', (e) => {
        this.sedeSeleccionada = e.target.value;
        this.cargarCajaSedes();
    });
    document.getElementById('caja-fecha').addEventListener('change', (e) => {
        this.fecha = e.target.value || this.hoy();
        this.cargarCajaSedes();
    });
},

    /**
     * Consulta la caja de la sede elegida (o de todas) en la fecha elegida
     */
    async cargarCajaSedes() {
    const container = document.getElementById('cajas-sedes-container');
    if (!container) return;

    container.innerHTML = '<div class="col-span-full text-center py-8 text-gray-500"><i class="fa-solid fa-spinner fa-spin text-2xl mb-2"></i><p>Calculando dinero en caja...</p></div>';

    const esHoy = this.fecha === this.hoy();
    const sedesAMostrar = this.sedeSeleccionada === 'todas'
        ? this.sedes
        : this.sedes.filter(s => String(s.id) === String(this.sedeSeleccionada));

    const resumen = [];
    let html = '';

    for (const sede of sedesAMostrar) {
        const { data, error } = await db.rpc('resumen_caja_sede_hoy', {
            p_sede_id: sede.id,
            p_fecha: this.fecha
        });

        if (error) {
            console.error(`Error al cargar caja de sede ${sede.nombre}:`, error);
            continue;
        }

        const caja = (data && data[0]) ? data[0] : {
            sede_nombre: sede.nombre,
            trabajadoras_activas: 0,
            total_producido: 0,
            total_comisiones: 0,
            total_caja_salon: 0
        };
        resumen.push(caja);

        html += `
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 relative overflow-hidden" id="caja-sede-${sede.id}">
                    <div class="absolute top-0 right-0 p-4 opacity-10">
                        <i class="fa-solid fa-building text-6xl text-pink-600"></i>
                    </div>

                    <h3 class="text-lg font-bold text-gray-800 mb-1">${caja.sede_nombre}</h3>
                    ${esHoy ? `
                    <p class="text-sm text-gray-500 mb-4">
                        <i class="fa-solid fa-user-tie text-pink-500 mr-1"></i> Trabajadoras activas ahora: <b>${caja.trabajadoras_activas}</b>
                    </p>` : `<p class="text-sm text-gray-500 mb-4">${this.fecha}</p>`}

                    <div class="space-y-3 mt-4">
                        <div class="flex justify-between items-center border-b pb-2">
                            <span class="text-sm text-gray-500">Total Producido ${esHoy ? 'Hoy' : ''}</span>
                            <span class="font-bold text-gray-800">${UI.formatCurrency(caja.total_producido)}</span>
                        </div>
                        <div class="flex justify-between items-center border-b pb-2">
                            <span class="text-sm text-gray-500">A pagar (Comisiones)</span>
                            <span class="font-bold text-green-600">${UI.formatCurrency(caja.total_comisiones)}</span>
                        </div>
                        <div class="flex justify-between items-center bg-pink-50 p-3 rounded-lg border border-pink-100">
                            <span class="font-bold text-gray-800">Efectivo en Caja (Neto)</span>
                            <span class="font-bold text-pink-600 text-xl">${UI.formatCurrency(caja.total_caja_salon)}</span>
                        </div>
                    </div>
                </div>
            `;
    }

    // Tarjeta de totales acumulados cuando se ven varias sedes
    if (resumen.length > 1) {
        const suma = (campo) => resumen.reduce((acc, c) => acc + Number(c[campo] || 0), 0);
        html += `
                <div class="bg-pink-50 rounded-xl border border-pink-200 p-6 col-span-full">
                    <h3 class="text-lg font-bold text-gray-800 mb-3">Total de todas las sedes</h3>
                    <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        <div><p class="text-xs text-gray-500">Total Producido</p><p class="font-bold text-gray-800">${UI.formatCurrency(suma('total_producido'))}</p></div>
                        <div><p class="text-xs text-gray-500">Comisiones</p><p class="font-bold text-green-600">${UI.formatCurrency(suma('total_comisiones'))}</p></div>
                        <div><p class="text-xs text-gray-500">Efectivo en Caja (Neto)</p><p class="font-bold text-pink-600 text-xl">${UI.formatCurrency(suma('total_caja_salon'))}</p></div>
                    </div>
                </div>
            `;
    }

    this.ultimoResumen = resumen;
    container.innerHTML = html || '<p class="text-gray-400 text-center col-span-full">No se encontraron sedes activas.</p>';

},

/**
 * Realtime: recalcula cuando se registra o se borra un servicio
 */
activarRealtime() {
    if (this.realtimeSubscription) return;

    this.realtimeSubscription = db
        .channel('caja-realtime')
        .on(
            'postgres_changes',
            { event: '*', schema: 'public', table: 'servicios_realizados' },
            () => { this.cargarCajaSedes(); }
        )
        .subscribe();
},

desactivarRealtime() {
    if (this.realtimeSubscription) {
        db.removeChannel(this.realtimeSubscription);
        this.realtimeSubscription = null;
    }
}
};