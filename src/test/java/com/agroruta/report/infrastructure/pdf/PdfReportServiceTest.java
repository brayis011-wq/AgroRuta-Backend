package com.agroruta.report.infrastructure.pdf;

import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.WorkerPayrollDetail;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PdfReportService")
class PdfReportServiceTest {

    private PdfReportService service;

    // Fecha fija reutilizable en todos los tests
    private static final LocalDateTime AHORA = LocalDateTime.of(2024, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        service = new PdfReportService();
    }

    // =========================================================================
    // generatePayrollPdf
    // =========================================================================

    @Nested
    @DisplayName("generatePayrollPdf")
    class GeneratePayrollPdf {

        @Test
        @DisplayName("retorna un arreglo de bytes no nulo ni vacío")
        void retornaByteArrayNoVacio() {
            byte[] result = service.generatePayrollPdf(payrollConUnTrabajador());

            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("el PDF generado tiene firma válida (%PDF)")
        void tieneEncabezadoPdf() {
            byte[] result = service.generatePayrollPdf(payrollConUnTrabajador());

            assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("el PDF contiene el título 'Reporte de Nómina'")
        void contieneTitulo() throws IOException {
            byte[] result = service.generatePayrollPdf(payrollConUnTrabajador());

            assertThat(extraerTexto(result)).contains("Reporte de Nómina");
        }

        @Test
        @DisplayName("el PDF contiene el total acumulado del reporte")
        void contieneTotalAcumulado() throws IOException {
            PayrollReport report = new PayrollReport(AHORA, new BigDecimal("4500.00"), List.of());

            byte[] result = service.generatePayrollPdf(report);

            assertThat(extraerTexto(result)).contains("$4.500");
        }

        @Test
        @DisplayName("el PDF contiene el nombre del trabajador")
        void contieneNombreTrabajador() throws IOException {
            byte[] result = service.generatePayrollPdf(payrollConUnTrabajador());

            assertThat(extraerTexto(result)).contains("Carlos Pérez");
        }

        @Test
        @DisplayName("el PDF contiene el total pagado del trabajador")
        void contieneTotalPagadoTrabajador() throws IOException {
            byte[] result = service.generatePayrollPdf(payrollConUnTrabajador());

            assertThat(extraerTexto(result)).contains("$1.200,50");
        }

        @Test
        @DisplayName("funciona con lista vacía de trabajadores sin lanzar excepción")
        void funcionaConListaVacia() {
            PayrollReport report = new PayrollReport(AHORA, BigDecimal.ZERO, List.of());

            assertThatCode(() -> service.generatePayrollPdf(report))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("funciona con múltiples trabajadores y contiene todos sus nombres")
        void funcionaConMultiplesTrabajadores() throws IOException {
            List<WorkerPayrollDetail> trabajadores = List.of(
                    trabajador(1L, "Ana Gómez",    3, new BigDecimal("900.00")),
                    trabajador(2L, "Luis Torres",  5, new BigDecimal("1500.00")),
                    trabajador(3L, "María Ruiz",   2, new BigDecimal("600.00"))
            );
            PayrollReport report = new PayrollReport(AHORA, new BigDecimal("3000.00"), trabajadores);

            String texto = extraerTexto(service.generatePayrollPdf(report));

            assertThat(texto)
                    .contains("Ana Gómez")
                    .contains("Luis Torres")
                    .contains("María Ruiz");
        }
    }

    // =========================================================================
    // generateCropPdf
    // =========================================================================

    @Nested
    @DisplayName("generateCropPdf")
    class GenerateCropPdf {

        @Test
        @DisplayName("retorna un arreglo de bytes no nulo ni vacío")
        void retornaByteArrayNoVacio() {
            byte[] result = service.generateCropPdf(cropConUnDetalle());

            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("el PDF generado tiene firma válida (%PDF)")
        void tieneEncabezadoPdf() {
            byte[] result = service.generateCropPdf(cropConUnDetalle());

            assertThat(new String(result, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("el PDF contiene el título 'Reporte de Cultivos'")
        void contieneTitulo() throws IOException {
            byte[] result = service.generateCropPdf(cropConUnDetalle());

            assertThat(extraerTexto(result)).contains("Reporte de Cultivos");
        }

        @Test
        @DisplayName("el PDF contiene la variedad del cultivo")
        void contieneVariedad() throws IOException {
            byte[] result = service.generateCropPdf(cropConUnDetalle());

            assertThat(extraerTexto(result)).contains("Café Arábica");
        }

        @Test
        @DisplayName("el PDF contiene el estado del cultivo")
        void contieneEstado() throws IOException {
            byte[] result = service.generateCropPdf(cropConUnDetalle());

            assertThat(extraerTexto(result)).contains("ACTIVO");
        }

        @Test
        @DisplayName("cuando totalKgCosechado es null muestra '0' sin lanzar excepción")
        void kgNullMuestraCero() throws IOException {
            // Long id, String variedad, String estado, LocalDate fechaSiembra,
            // long diasDesdeSiembra, Double totalKgCosechado, int totalCosechas
            CropDetail sinKg = new CropDetail(
                    1L, "Maíz", "EN_PROCESO",
                    LocalDate.of(2024, 1, 10), 45L, null, 0
            );
            CropReport report = new CropReport(AHORA, List.of(sinKg));

            byte[] result = service.generateCropPdf(report);

            assertThat(extraerTexto(result)).contains("0");
        }

        @Test
        @DisplayName("funciona con lista vacía de cultivos sin lanzar excepción")
        void funcionaConListaVacia() {
            CropReport report = new CropReport(AHORA, List.of());

            assertThatCode(() -> service.generateCropPdf(report))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("funciona con múltiples cultivos y contiene todas las variedades")
        void funcionaConMultiplesCultivos() throws IOException {
            List<CropDetail> cultivos = List.of(
                    cultivo(1L, "Café Arábica", "ACTIVO",    150L, 200.5,  3),
                    cultivo(2L, "Plátano",       "COSECHADO", 360L, 450.0,  5),
                    cultivo(3L, "Cacao",          "ACTIVO",    80L,  null,   0)
            );
            CropReport report = new CropReport(AHORA, cultivos);

            String texto = extraerTexto(service.generateCropPdf(report));

            assertThat(texto)
                    .contains("Café Arábica")
                    .contains("Plátano")
                    .contains("Cacao");
        }
    }

    // =========================================================================
    // Helpers de construcción
    // =========================================================================

    private PayrollReport payrollConUnTrabajador() {
        return new PayrollReport(
                AHORA,
                new BigDecimal("1200.50"),
                List.of(trabajador(1L, "Carlos Pérez", 4, new BigDecimal("1200.50")))
        );
    }

    private CropReport cropConUnDetalle() {
        return new CropReport(AHORA, List.of(
                cultivo(1L, "Café Arábica", "ACTIVO", 150L, 200.50, 3)
        ));
    }

    /**
     * WorkerPayrollDetail(Long id, String workerName, Integer totalPagos,
     *                     BigDecimal totalPaid, LocalDate fechaInicio,
     *                     LocalDate fechaFin, Integer diasTrabajados)
     */
    private WorkerPayrollDetail trabajador(Long id, String nombre, int pagos, BigDecimal total) {
        return new WorkerPayrollDetail(
                id, nombre, pagos, total,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 5, 31),
                150
        );
    }

    /**
     * CropDetail(Long id, String variedad, String estado, LocalDate fechaSiembra,
     *            long diasDesdeSiembra, Double totalKgCosechado, int totalCosechas)
     */
    private CropDetail cultivo(Long id, String variedad, String estado,
                               long dias, Double kg, int cosechas) {
        return new CropDetail(
                id, variedad, estado,
                LocalDate.of(2024, 1, 1),
                dias, kg, cosechas
        );
    }

    /** Extrae todo el texto del PDF usando PDFBox para verificar contenido. */
    private String extraerTexto(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    // =========================================================================
    // Diseño nuevo
    // =========================================================================

    @Nested
    @DisplayName("diseño del PDF")
    class Diseno {

        @Test
        @DisplayName("muestra para quién se generó y la fecha en formato colombiano")
        void encabezado() throws IOException {
            String texto = extraerTexto(service.generatePayrollPdf(payrollConUnTrabajador(), "Ana Rodríguez"));

            assertThat(texto).contains("Para Ana Rodríguez").contains("1 jun 2024, 10:00 a. m.");
        }

        @Test
        @DisplayName("incluye pendiente por pagar y los meses de la gráfica")
        void pendienteYMeses() throws IOException {
            PayrollReport r = new PayrollReport(AHORA, new BigDecimal("900000"),
                    List.of(trabajador(1L, "Ana Gómez", 2, new BigDecimal("900000"))),
                    List.of(new com.agroruta.report.domain.MonthlyPayment("2024-05", new BigDecimal("400000"), 1),
                            new com.agroruta.report.domain.MonthlyPayment("2024-06", new BigDecimal("500000"), 1)),
                    new BigDecimal("1500000"), 3);

            String texto = extraerTexto(service.generatePayrollPdf(r));

            assertThat(texto).contains("Pendiente por pagar").contains("$1.500.000").contains("3 nóminas")
                    .contains("may").contains("jun").contains("Pagos por mes");
        }

        @Test
        @DisplayName("una tabla larga pasa a otra página y numera las páginas")
        void paginacion() throws IOException {
            List<WorkerPayrollDetail> muchos = new java.util.ArrayList<>();
            for (int i = 0; i < 60; i++) muchos.add(trabajador((long) i, "Trabajador " + i, 1, new BigDecimal("1000")));
            byte[] pdf = service.generatePayrollPdf(new PayrollReport(AHORA, new BigDecimal("60000"), muchos));

            try (PDDocument doc = Loader.loadPDF(pdf)) {
                assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            }
            assertThat(extraerTexto(pdf)).contains("Página 1 de").contains("Trabajador 59");
        }

        @Test
        @DisplayName("las etapas salen en español y la finca y el lote aparecen")
        void etapasYUbicacion() throws IOException {
            CropDetail c = new CropDetail(1L, "Café · Castillo", "PRODUCCION", LocalDate.of(2024, 1, 10),
                    140, 860.0, 3, "Café", "Lote Alto", "El Recuerdo");

            String texto = extraerTexto(service.generateCropPdf(new CropReport(AHORA, List.of(c))));

            assertThat(texto).contains("Producción").contains("El Recuerdo · Lote Alto").contains("Siembras por etapa")
                    .contains("Kg cosechados por cultivo").contains("860 kg");
        }

        @Test
        @DisplayName("caracteres que la fuente no tiene no rompen el PDF")
        void caracteresRaros() {
            assertThatCode(() -> service.generatePayrollPdf(new PayrollReport(AHORA, BigDecimal.ONE,
                    List.of(trabajador(1L, "José 🌱 → Nuñez", 1, BigDecimal.ONE))))).doesNotThrowAnyException();
        }
    }
}
