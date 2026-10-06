// ============================================================
// MÓDULO: CAJA EN TIEMPO REAL (ADMIN)
// Usa Supabase Realtime para actualizar la caja automáticamente
// ============================================================

const Caja = {
    realtimeSubscription: null,
    sedes: [],

    /**
     * Inicializa la vista de caja y la suscripción en tiempo real
     */
    async iniciar(sedes) {
        this.sedes = sedes || [];
        await this.cargarCajaSedes();
        this.activarRealtime();
    },

    /**
     * Consulta la caja de todas las sedes activas
     */
    async cargarCajaSedes() {
        const container = document.getElementById('cajas-sedes-container');
        if (!container) return;

        container.innerHTML = '<div class="col-span-full text-center py-8 text-gray-500"><i class="fa-solid fa-spinner fa-spin text-2xl mb-2"></i><p>Calculando dinero en caja...</p></div>';

        let html = '';
        for (const sede of this.sedes) {
            const { data, error } = await db.rpc('resumen_caja_sede_hoy', { p_sede_id: sede.id });

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

            html += `
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 relative overflow-hidden" id="caja-sede-${sede.id}">
                    <div class="absolute top-0 right-0 p-4 opacity-10">
                        <i class="fa-solid fa-building text-6xl text-pink-600"></i>
                    </div>
                    
                    <h3 class="text-lg font-bold text-gray-800 mb-1">${caja.sede_nombre}</h3>
                    <p class="text-sm text-gray-500 mb-4">
                        <i class="fa-solid fa-user-tie text-pink-500 mr-1"></i> Trabajadoras activas ahora: <b>${caja.trabajadoras_activas}</b>
                    </p>
                    
                    <div class="space-y-3 mt-4">
                        <div class="flex justify-between items-center border-b pb-2">
                            <span class="text-sm text-gray-500">Total Producido Hoy</span>
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

        container.innerHTML = html || '<p class="text-gray-400 text-center col-span-full">No se encontraron sedes activas.</p>';
    },

    /**
     * Activa la suscripción Realtime a la tabla servicios_realizados
     */
    activarRealtime() {
        if (this.realtimeSubscription) return; // Ya activa

        this.realtimeSubscription = db
            .channel('caja-realtime')
            .on(
                'postgres_changes',
                { event: 'INSERT', schema: 'public', table: 'servicios_realizados' },
                () => {
                    // Recalcular caja cuando cualquier trabajadora registra un servicio
                    this.cargarCajaSedes();
                    UI.showAlert('🔔 Caja actualizada en tiempo real.', 'success');
                }
            )
            .subscribe();
    },

    /**
     * Desuscribirse de cambios al salir de la vista
     */
    desactivarRealtime() {
        if (this.realtimeSubscription) {
            db.removeChannel(this.realtimeSubscription);
            this.realtimeSubscription = null;
        }
    }
};
