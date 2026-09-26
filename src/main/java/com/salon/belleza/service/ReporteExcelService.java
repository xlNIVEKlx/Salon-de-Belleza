package com.salon.belleza.service;

import com.salon.belleza.exception.ResourceNotFoundException;
import com.salon.belleza.model.ServicioRealizado;
import com.salon.belleza.model.Sede;
import com.salon.belleza.repository.SedeRepository;
import com.salon.belleza.repository.ServicioRealizadoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service para generación de reportes Excel con Apache POI.
 *
 * ESTRUCTURA DEL EXCEL:
 * ─────────────────────────────────────────────────────────────────────────────
 * Hoja 1 → "Resumen General": todos los servicios de todas las sedes en el rango
 * Hoja N → una por cada sede con datos: "Sede Centro", "Sede Norte", etc.
 *
 * Cada hoja contiene:
 *   - Encabezado con nombre de sede y rango de fechas
 *   - Tabla con: Fecha | Trabajadora | Servicio | Cantidad | Precio Unit.
 *               | % Comisión | Total Cobrado | Comisión Empleada | Caja Salón
 *   - Fila de TOTALES al final con suma de columnas monetarias
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
public class ReporteExcelService {

    private static final Logger log = LoggerFactory.getLogger(ReporteExcelService.class);
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ServicioRealizadoRepository servicioRealizadoRepository;
    private final SedeRepository sedeRepository;

    // ── Headers de columnas ──────────────────────────────────────────────────

    private static final String[] HEADERS = {
        "Fecha", "Trabajadora", "Sede", "Servicio", "Cantidad",
        "Precio Unit.", "% Comisión", "Total Cobrado", "Comisión Emp.", "Caja Salón"
    };

    // ── Método principal ─────────────────────────────────────────────────────

    /**
     * Genera el archivo Excel completo en memoria y lo devuelve como byte[].
     *
     * @param fechaInicio Inicio del rango (inclusive). Si null → hoy.
     * @param fechaFin    Fin del rango (inclusive). Si null → hoy.
     * @param sedeId      Filtro opcional por sede. Si null → todas las sedes.
     * @return El archivo .xlsx como array de bytes, listo para enviar como descarga.
     */
    @Transactional(readOnly = true)
    public byte[] generarReporte(LocalDate fechaInicio, LocalDate fechaFin, Long sedeId)
            throws IOException {

        // Valores por defecto
        if (fechaInicio == null) fechaInicio = LocalDate.now();
        if (fechaFin == null)    fechaFin    = LocalDate.now();

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin    = fechaFin.atTime(LocalTime.MAX);

        log.info("Generando reporte Excel: fechas={} a {}, sedeId={}",
                fechaInicio, fechaFin, sedeId);

        // Cargar datos (una sola consulta con FETCH JOIN para evitar N+1)
        List<ServicioRealizado> datos;
        if (sedeId != null) {
            // Validar que la sede existe
            sedeRepository.findById(sedeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", sedeId));
            datos = servicioRealizadoRepository
                    .findParaReportePorSedeYRango(sedeId, inicio, fin);
        } else {
            datos = servicioRealizadoRepository
                    .findParaReporteEnRango(inicio, fin);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Estilos reutilizables
            EstilosExcel estilos = new EstilosExcel(workbook);

            // ── Hoja 1: Resumen General ──────────────────────────────────────
            Sheet hojaGeneral = workbook.createSheet("Resumen General");
            crearHoja(hojaGeneral, "RESUMEN GENERAL — TODAS LAS SEDES",
                    fechaInicio, fechaFin, datos, estilos);

            // ── Hojas por Sede ───────────────────────────────────────────────
            // Agrupar datos por sede usando LinkedHashMap para mantener orden
            Map<String, List<ServicioRealizado>> porSede = datos.stream()
                    .collect(Collectors.groupingBy(
                            sr -> sr.getJornada().getSede().getNombre(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (Map.Entry<String, List<ServicioRealizado>> entry : porSede.entrySet()) {
                // Truncar nombre de hoja a 31 chars (límite de Excel)
                String nombreHoja = entry.getKey().length() > 31
                        ? entry.getKey().substring(0, 31)
                        : entry.getKey();

                Sheet hojaSede = workbook.createSheet(nombreHoja);
                crearHoja(hojaSede, entry.getKey().toUpperCase(),
                        fechaInicio, fechaFin, entry.getValue(), estilos);
            }

            // Serializar a bytes en memoria
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Reporte generado: {} hojas, {} registros totales",
                    workbook.getNumberOfSheets(), datos.size());
            return out.toByteArray();
        }
    }

    // ── Construcción de hoja ─────────────────────────────────────────────────

    private void crearHoja(Sheet sheet, String titulo, LocalDate inicio, LocalDate fin,
                           List<ServicioRealizado> datos, EstilosExcel estilos) {

        int rowNum = 0;

        // ── Título ───────────────────────────────────────────────────────────
        Row rowTitulo = sheet.createRow(rowNum++);
        Cell cellTitulo = rowTitulo.createCell(0);
        cellTitulo.setCellValue(titulo);
        cellTitulo.setCellStyle(estilos.titulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        // ── Sub-título con rango de fechas ───────────────────────────────────
        Row rowFechas = sheet.createRow(rowNum++);
        Cell cellFechas = rowFechas.createCell(0);
        cellFechas.setCellValue("Período: " +
                inicio.format(FECHA_CORTA) + " al " + fin.format(FECHA_CORTA));
        cellFechas.setCellStyle(estilos.subTitulo);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));

        rowNum++; // Fila vacía de separación

        // ── Headers de columnas ──────────────────────────────────────────────
        Row rowHeaders = sheet.createRow(rowNum++);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = rowHeaders.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(estilos.header);
        }

        // ── Filas de datos ───────────────────────────────────────────────────
        BigDecimal sumTotalCobrado   = BigDecimal.ZERO;
        BigDecimal sumComision       = BigDecimal.ZERO;
        BigDecimal sumCajaSalon      = BigDecimal.ZERO;

        for (ServicioRealizado sr : datos) {
            Row row = sheet.createRow(rowNum++);

            // Col 0: Fecha
            crearCelda(row, 0, sr.getFechaRegistro().format(FECHA_FMT), estilos.normal);
            // Col 1: Trabajadora
            crearCelda(row, 1, sr.getJornada().getUsuario().getNombre(), estilos.normal);
            // Col 2: Sede
            crearCelda(row, 2, sr.getJornada().getSede().getNombre(), estilos.normal);
            // Col 3: Servicio
            crearCelda(row, 3, sr.getServicio().getNombre(), estilos.normal);
            // Col 4: Cantidad
            crearCeldaNumero(row, 4, sr.getCantidad().doubleValue(), estilos.numero);
            // Col 5: Precio unitario
            crearCeldaMoneda(row, 5, sr.getPrecioUnitario(), estilos.moneda);
            // Col 6: % Comisión
            crearCelda(row, 6, sr.getPorcentajeComisionAplicado().toPlainString() + "%", estilos.normal);
            // Col 7: Total cobrado
            crearCeldaMoneda(row, 7, sr.getTotalCobrado(), estilos.moneda);
            // Col 8: Comisión empleada
            crearCeldaMoneda(row, 8, sr.getComisionEmpleada(), estilos.moneda);
            // Col 9: Caja salón
            crearCeldaMoneda(row, 9, sr.getTotalCajaSalon(), estilos.monedaDestacada);

            // Acumuladores para totales
            sumTotalCobrado = sumTotalCobrado.add(sr.getTotalCobrado());
            sumComision     = sumComision.add(sr.getComisionEmpleada());
            sumCajaSalon    = sumCajaSalon.add(sr.getTotalCajaSalon());
        }

        // ── Fila de TOTALES ──────────────────────────────────────────────────
        Row rowTotal = sheet.createRow(rowNum);
        crearCelda(rowTotal, 0, "TOTALES", estilos.totalLabel);
        // Celdas vacías 1-6
        for (int i = 1; i <= 6; i++) {
            rowTotal.createCell(i).setCellStyle(estilos.totalLabel);
        }
        crearCeldaMoneda(rowTotal, 7, sumTotalCobrado, estilos.totalMoneda);
        crearCeldaMoneda(rowTotal, 8, sumComision,     estilos.totalMoneda);
        crearCeldaMoneda(rowTotal, 9, sumCajaSalon,    estilos.totalMonedaDestacada);

        // ── Auto-ajuste de columnas ──────────────────────────────────────────
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
            // Asegurar mínimo de 12 caracteres de ancho
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }

    // ── Helpers para crear celdas ────────────────────────────────────────────

    private void crearCelda(Row row, int col, String valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor : "");
        cell.setCellStyle(estilo);
    }

    private void crearCeldaNumero(Row row, int col, double valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor);
        cell.setCellStyle(estilo);
    }

    private void crearCeldaMoneda(Row row, int col, BigDecimal valor, CellStyle estilo) {
        Cell cell = row.createCell(col);
        cell.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        cell.setCellStyle(estilo);
    }

    // ── Clase interna para estilos ───────────────────────────────────────────

    /**
     * Encapsula todos los estilos del workbook para reutilizarlos eficientemente.
     * POI recomienda crear estilos una vez y reutilizarlos (límite de 64K estilos por workbook).
     */
    private static class EstilosExcel {
        final CellStyle titulo;
        final CellStyle subTitulo;
        final CellStyle header;
        final CellStyle normal;
        final CellStyle moneda;
        final CellStyle monedaDestacada;
        final CellStyle numero;
        final CellStyle totalLabel;
        final CellStyle totalMoneda;
        final CellStyle totalMonedaDestacada;

        EstilosExcel(Workbook wb) {
            // Fuentes
            Font fuenteTitulo = wb.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());

            Font fuenteHeader = wb.createFont();
            fuenteHeader.setBold(true);
            fuenteHeader.setColor(IndexedColors.WHITE.getIndex());

            Font fuenteTotal = wb.createFont();
            fuenteTotal.setBold(true);

            Font fuenteSubTitulo = wb.createFont();
            fuenteSubTitulo.setItalic(true);
            fuenteSubTitulo.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            // Formato de moneda colombiana
            DataFormat fmt = wb.createDataFormat();
            short fmtMoneda = fmt.getFormat("#,##0.00");
            short fmtNumero = fmt.getFormat("#,##0");

            // ── Estilo: título ──
            titulo = wb.createCellStyle();
            titulo.setFont(fuenteTitulo);
            titulo.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            titulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titulo.setAlignment(HorizontalAlignment.CENTER);
            titulo.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(titulo, BorderStyle.THIN);

            // ── Estilo: sub-título ──
            subTitulo = wb.createCellStyle();
            subTitulo.setFont(fuenteSubTitulo);
            subTitulo.setAlignment(HorizontalAlignment.CENTER);

            // ── Estilo: header de columnas ──
            header = wb.createCellStyle();
            header.setFont(fuenteHeader);
            header.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setWrapText(true);
            setBorder(header, BorderStyle.THIN);

            // ── Estilo: celda normal ──
            normal = wb.createCellStyle();
            setBorder(normal, BorderStyle.HAIR);

            // ── Estilo: moneda ──
            moneda = wb.createCellStyle();
            moneda.setDataFormat(fmtMoneda);
            moneda.setAlignment(HorizontalAlignment.RIGHT);
            setBorder(moneda, BorderStyle.HAIR);

            // ── Estilo: moneda destacada (columna caja salón) ──
            monedaDestacada = wb.createCellStyle();
            monedaDestacada.setDataFormat(fmtMoneda);
            monedaDestacada.setAlignment(HorizontalAlignment.RIGHT);
            monedaDestacada.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            monedaDestacada.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(monedaDestacada, BorderStyle.HAIR);

            // ── Estilo: número entero ──
            numero = wb.createCellStyle();
            numero.setDataFormat(fmtNumero);
            numero.setAlignment(HorizontalAlignment.CENTER);
            setBorder(numero, BorderStyle.HAIR);

            // ── Estilo: fila de totales (etiqueta) ──
            totalLabel = wb.createCellStyle();
            totalLabel.setFont(fuenteTotal);
            totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorder(totalLabel, BorderStyle.MEDIUM);

            // ── Estilo: total moneda ──
            totalMoneda = wb.createCellStyle();
            totalMoneda.setFont(fuenteTotal);
            totalMoneda.setDataFormat(fmtMoneda);
            totalMoneda.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalMoneda.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalMoneda.setAlignment(HorizontalAlignment.RIGHT);
            setBorder(totalMoneda, BorderStyle.MEDIUM);

            // ── Estilo: total caja salón (destacado verde) ──
            totalMonedaDestacada = wb.createCellStyle();
            totalMonedaDestacada.setFont(fuenteTotal);
            totalMonedaDestacada.setDataFormat(fmtMoneda);
            totalMonedaDestacada.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            totalMonedaDestacada.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font fuenteBlanca = wb.createFont();
            fuenteBlanca.setBold(true);
            fuenteBlanca.setColor(IndexedColors.WHITE.getIndex());
            totalMonedaDestacada.setFont(fuenteBlanca);
            totalMonedaDestacada.setAlignment(HorizontalAlignment.RIGHT);
            setBorder(totalMonedaDestacada, BorderStyle.MEDIUM);
        }

        private void setBorder(CellStyle style, BorderStyle bs) {
            style.setBorderTop(bs);
            style.setBorderBottom(bs);
            style.setBorderLeft(bs);
            style.setBorderRight(bs);
        }
    }
}
