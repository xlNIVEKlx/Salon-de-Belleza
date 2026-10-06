// ============================================================
// MÓDULO: CAJA (ADMIN Y TRABAJADORAS - SOLO LECTURA)
// Selector de sede y fecha, tiempo real y descarga en Excel
// ============================================================

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
            <button id="caja-excel" type="button"
                    class="bg-green-600 text-white rounded-lg px-4 py-2 text-sm font-semibold">
                <i class="fa-solid fa-file-excel mr-1"></i> Descargar Excel
            </button>
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
        document.getElementById('caja-excel').addEventListener('click', () => this.descargarExcel());
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
     * Descarga en Excel lo que se está viendo (sede y fecha elegidas)
     */
    descargarExcel() {
        if (typeof XLSX === 'undefined') {
            UI.showAlert('No se pudo cargar la librería de Excel.', 'error');
            return;
        }
        if (!this.ultimoResumen.length) {
            UI.showAlert('No hay datos para descargar.', 'error');
            return;
        }

        const filas = this.ultimoResumen.map(c => ({
            'Sede': c.sede_nombre,
            'Fecha': this.fecha,
            'Total Producido': Number(c.total_producido || 0),
            'Comisiones': Number(c.total_comisiones || 0),
            'Efectivo en Caja (Neto)': Number(c.total_caja_salon || 0)
        }));

        if (filas.length > 1) {
            filas.push({
                'Sede': 'TOTAL',
                'Fecha': this.fecha,
                'Total Producido': filas.reduce((a, f) => a + f['Total Producido'], 0),
                'Comisiones': filas.reduce((a, f) => a + f['Comisiones'], 0),
                'Efectivo en Caja (Neto)': filas.reduce((a, f) => a + f['Efectivo en Caja (Neto)'], 0)
            });
        }

        const hoja = XLSX.utils.json_to_sheet(filas);
        const libro = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(libro, hoja, 'Caja');
        XLSX.writeFile(libro, `caja_${this.fecha}.xlsx`);
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