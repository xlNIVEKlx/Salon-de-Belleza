// ============================================================
// MÓDULO: REPORTES EXCEL (ADMIN)
// Generación 100% en cliente con SheetJS
// ============================================================

const Reportes = {
    async descargarExcel(e) {
        e.preventDefault();

        const inicio = document.getElementById('rep-inicio').value;
        const fin = document.getElementById('rep-fin').value;

        if (!inicio || !fin) {
            UI.showAlert('Selecciona el rango de fechas.', 'warning');
            return;
        }

        UI.showAlert('Consultando datos para el reporte...', 'warning');

        // Consultar servicios realizados en el rango
        const { data, error } = await db
            .from('servicios_realizados')
            .select(`
                id,
                fecha_registro,
                cantidad,
                precio_unitario,
                porcentaje_comision_aplicado,
                total_cobrado,
                comision_empleada,
                total_caja_salon,
                catalogo_servicios(nombre),
                jornadas(
                    sedes(nombre),
                    perfiles(nombre, username)
                )
            `)
            .gte('fecha_registro', `${inicio}T00:00:00`)
            .lte('fecha_registro', `${fin}T23:59:59`)
            .order('fecha_registro', { ascending: true });

        if (error) {
            UI.showAlert('Error al generar reporte: ' + error.message, 'error');
            return;
        }

        if (!data || data.length === 0) {
            UI.showAlert('No hay registros en el rango seleccionado.', 'warning');
            return;
        }

        // Formatear filas para SheetJS
        const filas = data.map(item => ({
            'Fecha y Hora': new Date(item.fecha_registro).toLocaleString('es-CO'),
            'Sede': item.jornadas?.sedes?.nombre || 'N/A',
            'Trabajadora': item.jornadas?.perfiles?.nombre || 'N/A',
            'Servicio': item.catalogo_servicios?.nombre || 'N/A',
            'Cantidad': item.cantidad,
            'Precio Unitario': item.precio_unitario,
            '% Comisión': `${item.porcentaje_comision_aplicado}%`,
            'Total Cobrado': item.total_cobrado,
            'Comisión Empleada': item.comision_empleada,
            'Caja Salón': item.total_caja_salon
        }));

        // Crear hoja y descargar archivo Excel
        const worksheet = XLSX.utils.json_to_sheet(filas);
        const workbook = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(workbook, worksheet, 'Reporte Ventas');

        const fileName = `Reporte_Salon_${inicio}_al_${fin}.xlsx`;
        XLSX.writeFile(workbook, fileName);

        UI.showAlert('Reporte descargado exitosamente');
    }
};
