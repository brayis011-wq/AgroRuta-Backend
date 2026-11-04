package com.agroruta.report.infrastructure.pdf;

import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Barra;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Celda;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Columna;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Segmento;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Tarjeta;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.agroruta.report.infrastructure.pdf.FormatoReporte.*;
import static com.agroruta.report.infrastructure.pdf.PdfLienzo.*;

/** PDF de los reportes de nómina y de cultivos, con resumen, gráficas y tabla de detalle. */
@Service
public class PdfReportService {

    /** Trabajadores que se muestran en la gráfica (la tabla los muestra todos) */
    private static final int MAX_BARRAS = 10;

    private static final Map<String, Color> COLOR_ETAPA = new LinkedHashMap<>();
    static {
        COLOR_ETAPA.put("GERMINACION", new Color(0xA5, 0xD6, 0xA7));
        COLOR_ETAPA.put("CRECIMIENTO", new Color(0x66, 0xBB, 0x6A));
        COLOR_ETAPA.put("PRODUCCION", new Color(0x2E, 0x7D, 0x32));
        COLOR_ETAPA.put("COSECHA", new Color(0xF9, 0xA8, 0x25));
        COLOR_ETAPA.put("FINALIZADO", new Color(0xB0, 0xB8, 0xB2));
    }

    public byte[] generatePayrollPdf(PayrollReport report) {
        return generatePayrollPdf(report, null);
    }

    public byte[] generateCropPdf(CropReport report) {
        return generateCropPdf(report, null);
    }

    // ══ Nómina ═════════════════════════════════════════════════════════════

    public byte[] generatePayrollPdf(PayrollReport r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Nómina", "Pagos realizados a tus trabajadores y lo que queda por pagar")) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);

            List<WorkerPayrollDetail> ws = r.workerDetails();
            int jornales = ws.stream().map(WorkerPayrollDetail::totalJornales).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            int pagos = ws.stream().map(WorkerPayrollDetail::totalPagos).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            BigDecimal promedio = ws.isEmpty() ? BigDecimal.ZERO
                    : r.totalAccumulated().divide(BigDecimal.valueOf(ws.size()), 0, RoundingMode.HALF_UP);

            pdf.tarjetas(List.of(
                    new Tarjeta("Total pagado", pesos(r.totalAccumulated()), pagos + (pagos == 1 ? " pago" : " pagos"), VERDE),
                    new Tarjeta("Pendiente por pagar", pesos(r.totalPendiente()),
                            r.nominasPendientes() + (r.nominasPendientes() == 1 ? " nómina" : " nóminas"), AMBAR),
                    new Tarjeta("Trabajadores pagados", String.valueOf(ws.size()), "Promedio " + pesos(promedio), new Color(0x1F, 0x5F, 0xA8)),
                    new Tarjeta("Jornales liquidados", numero(jornales, 0), "en las nóminas pagadas", new Color(0x81, 0xC7, 0x84))));

            if (!r.pagosPorMes().isEmpty()) {
                pdf.seccion("Pagos por mes");
                List<MonthlyPayment> meses = r.pagosPorMes();
                List<Barra> barras = new ArrayList<>();
                for (int i = 0; i < meses.size(); i++) {
                    MonthlyPayment m = meses.get(i);
                    boolean cambiaAnio = i > 0 && !m.mes().substring(0, 4).equals(meses.get(i - 1).mes().substring(0, 4));
                    barras.add(new Barra(mesCorto(m.mes(), cambiaAnio), m.total().doubleValue(), pesosCorto(m.total()), null));
                }
                if (barras.stream().allMatch(b -> b.valor() == 0)) pdf.nota("No hubo pagos en los últimos " + meses.size() + " meses.");
                else pdf.barrasVerticales(barras, 120);
            }

            pdf.seccion("Pagado por trabajador");
            if (ws.isEmpty()) {
                pdf.nota("Todavía no hay pagos registrados.");
            } else {
                double total = r.totalAccumulated().doubleValue();
                pdf.barrasHorizontales(ws.stream().limit(MAX_BARRAS).map(w -> new Barra(
                        w.workerName(), valor(w.totalPaid()), pesos(w.totalPaid()),
                        total > 0 ? numero(valor(w.totalPaid()) * 100 / total, 0) + " %" : null)).toList());
                if (ws.size() > MAX_BARRAS) pdf.nota("Y " + (ws.size() - MAX_BARRAS) + " trabajadores más en la tabla.");

                pdf.seccion("Detalle por trabajador");
                pdf.tabla(List.of(
                        new Columna("Trabajador", 165, false),
                        new Columna("Período pagado", 150, false),
                        new Columna("Jornales", 60, true),
                        new Columna("Pagos", 50, true),
                        new Columna("Total pagado", 90, true)),
                        ws.stream().map(w -> List.of(
                                Celda.negrita(w.workerName()),
                                Celda.de(periodo(w.periodoInicio(), w.periodoFin())),
                                Celda.de(numero(w.totalJornales() != null ? w.totalJornales() : 0, 0)),
                                Celda.de(String.valueOf(w.totalPagos() != null ? w.totalPagos() : 0)),
                                new Celda(pesos(w.totalPaid()), null, VERDE_OSCURO, true))).toList());
            }
            return pdf.terminar();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando PDF de nómina", e);
        }
    }

    // ══ Cultivos ═══════════════════════════════════════════════════════════

    public byte[] generateCropPdf(CropReport r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Cultivos", "Siembras, etapas y cosechas de tus fincas")) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);

            List<CropDetail> cs = r.cropDetails();
            long activas = cs.stream().filter(c -> !"FINALIZADO".equals(c.estado())).count();
            double kg = cs.stream().mapToDouble(c -> c.totalKgCosechado() != null ? c.totalKgCosechado() : 0).sum();
            int cosechas = cs.stream().mapToInt(CropDetail::totalCosechas).sum();
            long tipos = cs.stream().map(PdfReportService::cultivoDe).distinct().count();

            pdf.tarjetas(List.of(
                    new Tarjeta("Siembras", String.valueOf(cs.size()), tipos + (tipos == 1 ? " cultivo" : " cultivos"), VERDE),
                    new Tarjeta("Activas", String.valueOf(activas), (cs.size() - activas) + " finalizadas", new Color(0x66, 0xBB, 0x6A)),
                    new Tarjeta("Kg cosechados", numero(kg, kg % 1 == 0 ? 0 : 1) + " kg", "en total", AMBAR),
                    new Tarjeta("Cosechas", String.valueOf(cosechas),
                            cs.isEmpty() ? "—" : numero(kg / Math.max(1, cosechas), 1) + " kg por cosecha", new Color(0x1F, 0x5F, 0xA8))));

            if (cs.isEmpty()) {
                pdf.seccion("Siembras");
                pdf.nota("Todavía no hay siembras registradas.");
                return pdf.terminar();
            }

            pdf.seccion("Siembras por etapa");
            Map<String, Integer> porEtapa = new LinkedHashMap<>();
            COLOR_ETAPA.keySet().forEach(k -> porEtapa.put(k, 0));
            cs.forEach(c -> porEtapa.merge(c.estado(), 1, Integer::sum));
            pdf.barraApilada(porEtapa.entrySet().stream()
                    .map(e -> new Segmento(etapa(e.getKey()), e.getValue(), COLOR_ETAPA.getOrDefault(e.getKey(), TINTA_3)))
                    .toList());

            Map<String, Double> kgPorCultivo = new LinkedHashMap<>();
            cs.forEach(c -> kgPorCultivo.merge(cultivoDe(c), c.totalKgCosechado() != null ? c.totalKgCosechado() : 0, Double::sum));
            if (kgPorCultivo.values().stream().anyMatch(v -> v > 0)) {
                pdf.seccion("Kg cosechados por cultivo");
                pdf.barrasHorizontales(kgPorCultivo.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(MAX_BARRAS)
                        .map(e -> new Barra(e.getKey(), e.getValue(), numero(e.getValue(), e.getValue() % 1 == 0 ? 0 : 1) + " kg",
                                kg > 0 ? numero(e.getValue() * 100 / kg, 0) + " %" : null))
                        .toList());
            }

            pdf.seccion("Detalle de siembras");
            pdf.tabla(List.of(
                    new Columna("Cultivo · variedad", 128, false),
                    new Columna("Finca · lote", 108, false),
                    new Columna("Etapa", 76, false),
                    new Columna("Sembrado", 72, false),
                    new Columna("Días", 34, true),
                    new Columna("Kg", 44, true),
                    new Columna("Cosechas", 53, true)),
                    cs.stream().map(c -> List.of(
                            Celda.negrita(c.variedad()),
                            Celda.de(ubicacion(c)),
                            new Celda(etapa(c.estado()), suave(COLOR_ETAPA.get(c.estado())), oscuro(c.estado()), true),
                            Celda.de(fecha(c.fechaSiembra())),
                            Celda.de(numero(c.diasDesdeSiembra(), 0)),
                            Celda.de(kg(c.totalKgCosechado())),
                            Celda.de(String.valueOf(c.totalCosechas())))).toList());
            return pdf.terminar();
        } catch (IOException e) {
            throw new IllegalStateException("Error generando PDF de cultivos", e);
        }
    }

    // ── Apoyo ──────────────────────────────────────────────────────────────

    /** 420,5 o 1.240 (sin decimales cuando es exacto) */
    private static String kg(Double v) {
        double x = v != null ? v : 0;
        return numero(x, x % 1 == 0 ? 0 : 1);
    }

    private static double valor(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    /** Cultivo para agrupar: el tipo si existe, si no lo que va antes de " · " */
    private static String cultivoDe(CropDetail c) {
        if (c.cultivo() != null && !c.cultivo().isBlank()) return c.cultivo();
        String v = c.variedad() != null ? c.variedad() : "—";
        int i = v.indexOf(" · ");
        return i > 0 ? v.substring(0, i) : v;
    }

    private static String ubicacion(CropDetail c) {
        if (c.fincaNombre() == null && c.loteNombre() == null) return "—";
        if (c.fincaNombre() == null) return c.loteNombre();
        if (c.loteNombre() == null) return c.fincaNombre();
        return c.fincaNombre() + " · " + c.loteNombre();
    }

    /** Versión clara del color de una etapa, para el fondo de la píldora */
    private static Color suave(Color c) {
        if (c == null) return VERDE_SUAVE;
        return new Color((c.getRed() + 255 * 3) / 4, (c.getGreen() + 255 * 3) / 4, (c.getBlue() + 255 * 3) / 4);
    }

    private static Color oscuro(String etapa) {
        return switch (etapa == null ? "" : etapa) {
            case "COSECHA" -> new Color(0x8A, 0x5A, 0x00);
            case "FINALIZADO" -> TINTA_3;
            default -> VERDE_OSCURO;
        };
    }
}
