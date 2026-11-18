package com.agroruta.report.infrastructure.pdf;

import com.agroruta.report.domain.CostosTemporada;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.ReporteFinanciero;
import com.agroruta.report.domain.TemporadaFinanzas;
import com.agroruta.report.domain.WorkerPayrollDetail;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PdfReportService - costos, rentabilidad y reporte completo")
class PdfReportesFinancierosTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 9, 30);
    private final PdfReportService service = new PdfReportService();

    private static CropDetail siembra(long id, long loteId, String lote, LocalDate fecha, String estado) {
        return new CropDetail(id, "Café · Castillo", estado, fecha, 100, 500.0, 3, "Café", lote, "La Esperanza",
                false, loteId, 2.0, null);
    }

    private static ReporteFinanciero finanzas() {
        return new ReporteFinanciero(AHORA, List.of(
                new TemporadaFinanzas(siembra(1, 4, "Lote Alto", LocalDate.of(2025, 2, 1), "FINALIZADO"),
                        new CostosTemporada(800_000, 16, 300_000, 100_000, 1_200_000, 2_000_000, 500, 500, 800_000.0, 2400.0)),
                new TemporadaFinanzas(siembra(2, 4, "Lote Alto", LocalDate.of(2026, 2, 1), "PRODUCCION"),
                        new CostosTemporada(900_000, 18, 400_000, 0, 1_300_000, 1_000_000, 400, 400, -300_000.0, 3250.0)),
                new TemporadaFinanzas(siembra(3, 5, "Lote Bajo", LocalDate.of(2026, 3, 1), "PRODUCCION"),
                        new CostosTemporada(0, 0, 0, 0, 0, 0, 0, 0, null, null)),
                new TemporadaFinanzas(siembra(4, 6, "Lote Perdido", LocalDate.of(2026, 4, 1), "PRODUCCION"), null)));
    }

    private static String texto(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static int paginas(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    @Test
    @DisplayName("el PDF de costos trae totales, en qué se fue la plata y el detalle con pérdidas en negativo")
    void costos() throws IOException {
        String t = texto(service.generateCostsPdf(finanzas(), "Ana"));

        assertThat(t).contains("Reporte de Costos", "Para Ana", "En qué se fue la plata", "Detalle por temporada");
        assertThat(t).contains("$2.500.000"); // costo total
        assertThat(t).contains("$3.000.000"); // ingresos
        assertThat(t).contains("$500.000");   // ganancia total
        assertThat(t).contains("-$300.000");  // pérdida de una temporada
        assertThat(t).contains("Sin precio");
        assertThat(t).contains("Lote Alto");
    }

    @Test
    @DisplayName("el PDF de rentabilidad compara las temporadas del mismo lote")
    void rentabilidad() throws IOException {
        String t = texto(service.generateProfitabilityPdf(finanzas(), null));

        assertThat(t).contains("Reporte de Rentabilidad", "Ganancia por lote", "Temporadas de cada lote");
        assertThat(t).contains("-$1.100.000"); // la segunda temporada ganó 1,1 M menos que la primera
        assertThat(t).contains("(en curso)");
        assertThat(t).doesNotContain("Lote Perdido"); // sin costos no se puede comparar
    }

    @Test
    @DisplayName("sin temporadas los dos reportes salen con un aviso, sin fallar")
    void vacios() throws IOException {
        ReporteFinanciero vacio = new ReporteFinanciero(AHORA, List.of());

        assertThat(texto(service.generateCostsPdf(vacio, null))).contains("Todavía no hay siembras registradas.");
        assertThat(texto(service.generateProfitabilityPdf(vacio, null))).contains("Todavía no hay temporadas con costos");
    }

    @Test
    @DisplayName("el reporte completo junta las cuatro partes, cada una desde una página nueva")
    void completo() throws IOException {
        PayrollReport nomina = new PayrollReport(AHORA, new BigDecimal("350000"), List.of(
                new WorkerPayrollDetail(1L, "Carlos López", 2, new BigDecimal("350000"),
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 7)));
        CropReport cultivos = new CropReport(AHORA, finanzas().temporadas().stream().map(TemporadaFinanzas::siembra).toList());

        byte[] pdf = service.generateCompletePdf(nomina, cultivos, finanzas(), true, true, "Ana");
        String t = texto(pdf);

        assertThat(t).contains("Reporte de la finca", "Nómina, cultivos, costos y rentabilidad");
        assertThat(t).contains("Carlos López", "Siembras por etapa", "En qué se fue la plata", "Ganancia por lote");
        assertThat(paginas(pdf)).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("el reporte completo puede llevar solo algunas partes")
    void soloAlgunas() throws IOException {
        String t = texto(service.generateCompletePdf(null, null, finanzas(), false, true, null));

        assertThat(t).contains("Rentabilidad", "Ganancia por lote").doesNotContain("En qué se fue la plata", "Pagado por trabajador");
        assertThatThrownBy(() -> service.generateCompletePdf(null, null, finanzas(), false, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("muchas temporadas pasan a varias páginas sin fallar")
    void muchasTemporadas() throws IOException {
        List<TemporadaFinanzas> filas = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            filas.add(new TemporadaFinanzas(siembra(i, i % 9, "Lote " + (i % 9), LocalDate.of(2020, 1, 1).plusMonths(i), "FINALIZADO"),
                    new CostosTemporada(1000 * i, i, 0, 0, 1000 * i, 1500 * i, 10, 10, 500.0 * i, 100.0 * i)));
        }
        ReporteFinanciero r = new ReporteFinanciero(AHORA, filas);

        assertThat(paginas(service.generateCostsPdf(r, null))).isGreaterThan(1);
        assertThat(paginas(service.generateProfitabilityPdf(r, null))).isGreaterThan(1);
    }
}
