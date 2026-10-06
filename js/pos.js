// ============================================================
// MÓDULO: POS (PUNTO DE VENTA Y JORNADA)
// Maneja turnos, precios por sede en tiempo real y registro de servicios
// ============================================================

const POS = {
    jornadaActiva: null,
    catalogo: [],

    async iniciar() {
        await this.cargarCatalogo();
        await this.verificarJornadaActiva();
    },

    async cargarCatalogo() {
        const { data, error } = await db
            .from('catalogo_servicios')
            .select('*')
            .eq('activo', true)
            .order('nombre');

        if (error) {
            console.error('Error al cargar catálogo:', error);
            return;
        }

        this.catalogo = data || [];
        this.renderBotones();
    },

    /**
     * Renderiza los botones del POS detectando dinámicamente la sede del turno activo
     */
    renderBotones() {
        const container = document.getElementById('botones-servicios');
        if (!container) return;

        if (this.catalogo.length === 0) {
            container.innerHTML = '<p class="text-gray-400 text-sm col-span-full text-center py-6">No hay servicios disponibles.</p>';
            return;
        }

        // Obtener de forma robusta la sede activa del turno
        let sedeActual = "1";
        if (this.jornadaActiva) {
            if (this.jornadaActiva.sede_id !== undefined && this.jornadaActiva.sede_id !== null) {
                sedeActual = String(this.jornadaActiva.sede_id);
            }
        }

        container.innerHTML = this.catalogo.map(s => {
            // El JSONB precios_por_sede guarda las llaves como "1", "2", "3", "4"
            const precios = s.precios_por_sede || {};

            // Extraer el precio de la sede actual, respaldado por el precio general
            const precioSede = Number(precios[sedeActual] ?? precios[String(sedeActual)] ?? s.precio ?? 0);

            return `
            <button onclick="POS.registrarServicio(${s.id}, '${s.nombre}')" class="pos-btn group">
                <div class="bg-pink-50 text-pink-600 rounded-full w-12 h-12 flex items-center justify-center mb-2 group-hover:bg-pink-100 transition">
                    <i class="fa-solid fa-hand-sparkles text-xl"></i>
                </div>
                <p class="font-bold text-gray-800 text-sm leading-tight">${s.nombre}</p>
                <p class="text-xs text-pink-600 font-semibold mt-1">${UI.formatCurrency(precioSede)}</p>
            </button>
        `;
        }).join('');
    },

    async verificarJornadaActiva() {
        const userId = Auth.getUserId();
        if (!userId) return;

        const { data, error } = await db
            .from('jornadas')
            .select('*, sedes(nombre)')
            .eq('usuario_id', userId)
            .eq('estado', 'ABIERTA')
            .maybeSingle();

        if (error) {
            console.error('Error al verificar jornada:', error);
            return;
        }

        this.jornadaActiva = data;
        this.actualizarUI();
        this.renderBotones();

        if (this.jornadaActiva) {
            this.actualizarResumen();
        }
    },

    actualizarUI() {
        const cerradaEl = document.getElementById('estado-jornada-cerrada');
        const abiertaEl = document.getElementById('estado-jornada-abierta');
        const posEl = document.getElementById('pos-container');
        const sedeLabel = document.getElementById('lbl-sede-actual');

        if (this.jornadaActiva) {
            if (cerradaEl) cerradaEl.classList.add('hidden');
            if (abiertaEl) abiertaEl.classList.remove('hidden');
            if (posEl) posEl.classList.remove('hidden');
            if (sedeLabel) sedeLabel.textContent = this.jornadaActiva.sedes?.nombre || 'Sede';
        } else {
            if (cerradaEl) cerradaEl.classList.remove('hidden');
            if (abiertaEl) abiertaEl.classList.add('hidden');
            if (posEl) posEl.classList.add('hidden');
        }
    },

    async abrirJornada() {
        const select = document.getElementById('select-sede-jornada');
        const sedeId = select ? select.value : null;

        if (!sedeId) {
            UI.showAlert('Selecciona una sede para comenzar.', 'warning');
            return;
        }

        const userId = Auth.getUserId();

        const { data, error } = await db
            .from('jornadas')
            .insert([{
                usuario_id: userId,
                sede_id: sedeId,
                estado: 'ABIERTA',
                fecha_apertura: new Date().toISOString()
            }])
            .select('*, sedes(nombre)')
            .single();

        if (error) {
            UI.showAlert('Error al abrir turno: ' + error.message, 'error');
            return;
        }

        this.jornadaActiva = data;
        UI.showAlert('Turno abierto exitosamente', 'success');
        this.actualizarUI();
        this.renderBotones();
        this.actualizarResumen();
    },

    async cerrarJornada() {
        if (!this.jornadaActiva) return;
        const confirmar = await UI.confirm({
            title: 'Cerrar Turno',
            message: '¿Seguro que deseas CERRAR TU TURNO?',
            confirmText: 'Cerrar Turno'
        });
        if (!confirmar) return;

        const { error } = await db
            .from('jornadas')
            .update({
                estado: 'CERRADA',
                fecha_cierre: new Date().toISOString()
            })
            .eq('id', this.jornadaActiva.id);

        if (error) {
            UI.showAlert('Error al cerrar turno: ' + error.message, 'error');
            return;
        }

        this.jornadaActiva = null;
        UI.showAlert('Turno finalizado con éxito', 'success');
        this.actualizarUI();
        this.renderBotones();
    },

    async registrarServicio(servicioId, nombre) {
        if (!this.jornadaActiva) {
            UI.showAlert('Debes tener un turno abierto para registrar servicios.', 'warning');
            return;
        }

        const servicio = this.catalogo.find(s => s.id === servicioId);
        if (!servicio) return;

        // Extraer precio exacto de la sede activa del turno para guardarlo en la BD
        const sedeActivaId = String(this.jornadaActiva.sede_id);
        const precios = servicio.precios_por_sede || {};
        const precioCobrar = Number(precios[sedeActivaId] ?? precios[String(sedeActivaId)] ?? servicio.precio) || 0;

        const porcentajeComision = Number(servicio.porcentaje_comision) || 0;
        const valorComision = precioCobrar * (porcentajeComision / 100);
        const valorCaja = precioCobrar - valorComision;

        const { error } = await db
            .from('servicios_realizados')
            .insert([{
                jornada_id: this.jornadaActiva.id,
                servicio_id: servicioId,
                cantidad: 1,
                precio_unitario: precioCobrar,
                porcentaje_comision_aplicado: porcentajeComision,
                total_cobrado: precioCobrar,
                comision_empleada: valorComision,
                total_caja_salon: valorCaja
            }]);

        if (error) {
            UI.showAlert('Error al registrar servicio: ' + error.message, 'error');
            return;
        }

        UI.showAlert(`¡Registrado: 1 x ${nombre} (${UI.formatCurrency(precioCobrar)})!`, 'success');
        this.actualizarResumen();
    },

    async actualizarResumen() {
        const { data, error } = await db.rpc('mi_resumen_jornada');

        if (error) {
            console.error('Error al obtener resumen de jornada:', error);
            return;
        }

        const resumen = data || { servicios: [], granTotalCobrado: 0, granTotalComision: 0, granTotalCajaSalon: 0 };

        const totalEl = document.getElementById('lbl-gran-total');
        const comisionEl = document.getElementById('lbl-gran-comision');
        const cajaEl = document.getElementById('lbl-gran-caja');

        if (totalEl) totalEl.textContent = UI.formatCurrency(resumen.granTotalCobrado);
        if (comisionEl) comisionEl.textContent = UI.formatCurrency(resumen.granTotalComision);
        if (cajaEl) cajaEl.textContent = UI.formatCurrency(resumen.granTotalCajaSalon);

        const list = document.getElementById('resumen-servicios-lista');
        if (!list) return;

        if (!resumen.servicios || resumen.servicios.length === 0) {
            list.innerHTML = '<p class="text-gray-400 text-sm text-center py-4">Aún no has registrado servicios hoy.</p>';
            return;
        }

        list.innerHTML = resumen.servicios.map(s => {
            const servObj = this.catalogo.find(c => c.nombre.toLowerCase() === s.servicioNombre.toLowerCase());
            const servIdArg = servObj ? servObj.id : 'null';
            const servNombreEscaped = s.servicioNombre.replace(/'/g, "\\'");

            return `
            <div class="bg-gray-50 rounded-lg p-3 border border-gray-100 flex justify-between items-center">
                <div>
                    <p class="font-bold text-gray-800 text-sm">${s.servicioNombre}</p>
                    <p class="text-xs text-gray-500">Realizados: ${s.cantidadTotal}</p>
                </div>
                <div class="flex items-center gap-3">
                    <div class="text-right">
                        <p class="font-bold text-pink-600 text-sm">${UI.formatCurrency(s.totalCobrado)}</p>
                        <p class="text-xs text-green-600">Comisión: ${UI.formatCurrency(s.totalComision)}</p>
                    </div>
                    <button onclick="POS.confirmarEliminarServicio(${servIdArg}, '${servNombreEscaped}')"
                        class="text-gray-400 hover:text-red-600 p-1.5 rounded-lg hover:bg-red-50 transition"
                        title="Eliminar un servicio">
                        <i class="fa-solid fa-trash-can text-sm"></i>
                    </button>
                </div>
            </div>
            `;
        }).join('');
    },

    async confirmarEliminarServicio(servicioId, servicioNombre) {
        const confirmar = await UI.confirm({
            title: 'Eliminar servicio registrado',
            message: '¿Seguro que quieres eliminar este servicio?',
            itemName: servicioNombre,
            confirmText: 'Eliminar'
        });
        if (!confirmar) return;
        await this.eliminarUnServicio(servicioId, servicioNombre);
    },

    async eliminarUnServicio(servicioId, servicioNombre) {
        if (!this.jornadaActiva) return;

        if (!servicioId) {
            const serv = this.catalogo.find(c => c.nombre.toLowerCase() === servicioNombre.toLowerCase());
            if (serv) servicioId = serv.id;
        }

        if (!servicioId) return;

        const { data: reg } = await db
            .from('servicios_realizados')
            .select('id')
            .eq('jornada_id', this.jornadaActiva.id)
            .eq('servicio_id', servicioId)
            .order('id', { ascending: false })
            .limit(1)
            .maybeSingle();

        if (!reg) return;

        await db.from('servicios_realizados').delete().eq('id', reg.id);
        UI.showAlert(`Se eliminó 1 servicio de ${servicioNombre}`, 'success');
        await this.actualizarResumen();
    }
};