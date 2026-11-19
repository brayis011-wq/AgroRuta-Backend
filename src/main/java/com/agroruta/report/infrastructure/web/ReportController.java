package com.agroruta.report.infrastructure.web;

import com.agroruta.report.application.ports.in.GenerateCropReportUseCase;
import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.in.GenerarReporteFinancieroUseCase;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.Periodo;
import com.agroruta.report.domain.ReporteFinanciero;
import com.agroruta.report.infrastructure.pdf.PdfReportService;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reportes en PDF y su vista previa. Todos aceptan {@code ?desde=2026-01-01&hasta=2026-12-31} (opcionales):
 * la nómina cuenta los pagos de esas fechas y los demás, las temporadas que estuvieron en el campo en ese tiempo.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GeneratePayrollReportUseCase generatePayrollReport;
    private final GenerateCropReportUseCase generateCropReport;
    private final PdfReportService pdfReportService;
    private final GenerarReporteFinancieroUseCase generarReporteFinanciero;

    @GetMapping(value = "/payroll", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payrollReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                Authentication authentication) {
        byte[] pdf = pdfReportService.generatePayrollPdf(payroll(authentication, Periodo.de(desde, hasta)), CurrentUser.nombre(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-nomina.pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/crops", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> cropReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             Authentication authentication) {
        byte[] pdf = pdfReportService.generateCropPdf(crops(authentication, Periodo.de(desde, hasta)), CurrentUser.nombre(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cultivos.pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/costs", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> costsReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              Authentication authentication) {
        byte[] pdf = pdfReportService.generateCostsPdf(finanzas(authentication, Periodo.de(desde, hasta)), CurrentUser.nombre(authentication));
        return adjunto(pdf, "reporte-costos.pdf");
    }

    @GetMapping(value = "/profitability", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> profitabilityReport(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                      Authentication authentication) {
        byte[] pdf = pdfReportService.generateProfitabilityPdf(finanzas(authentication, Periodo.de(desde, hasta)), CurrentUser.nombre(authentication));
        return adjunto(pdf, "reporte-rentabilidad.pdf");
    }

    /**
     * Varias partes en un solo PDF: {@code ?sections=payroll,crops,costs,profitability} (sin el parámetro, todas).
     * Solo se consultan los datos de las partes pedidas.
     */
    @GetMapping(value = "/full", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> fullReport(@RequestParam(required = false) List<String> sections,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             Authentication authentication) {
        Set<String> partes = new HashSet<>();
        if (sections == null || sections.isEmpty()) {
            partes.addAll(PARTES);
        } else {
            for (String s : sections) {
                String p = s.trim().toLowerCase(Locale.ROOT);
                if (!PARTES.contains(p)) throw new DatoInvalidoException("Parte de reporte desconocida: " + s);
                partes.add(p);
            }
        }
        Periodo periodo = Periodo.de(desde, hasta);
        boolean conCostos = partes.contains("costs");
        boolean conRentabilidad = partes.contains("profitability");
        byte[] pdf = pdfReportService.generateCompletePdf(
                partes.contains("payroll") ? payroll(authentication, periodo) : null,
                partes.contains("crops") ? crops(authentication, periodo) : null,
                conCostos || conRentabilidad ? finanzas(authentication, periodo) : null,
                conCostos, conRentabilidad, CurrentUser.nombre(authentication));
        return adjunto(pdf, "reporte-agroruta.pdf");
    }

    private static final List<String> PARTES = List.of("payroll", "crops", "costs", "profitability");

    private static ResponseEntity<byte[]> adjunto(byte[] pdf, String nombre) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/payroll/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PayrollReport> payrollPreview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                        Authentication authentication) {
        return ResponseEntity.ok(payroll(authentication, Periodo.de(desde, hasta)));
    }

    @GetMapping(value = "/crops/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CropReport> cropPreview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                  Authentication authentication) {
        return ResponseEntity.ok(crops(authentication, Periodo.de(desde, hasta)));
    }

    /** Administrador: todo. Resto: solo lo de su propio usuario. */
    private PayrollReport payroll(Authentication authentication, Periodo periodo) {
        boolean admin = CurrentUser.esAdmin(authentication);
        if (periodo.esTodo()) {
            return admin ? generatePayrollReport.generate() : generatePayrollReport.generateFor(CurrentUser.id(authentication));
        }
        return generatePayrollReport.generateEn(CurrentUser.id(authentication), admin, periodo);
    }

    private CropReport crops(Authentication authentication, Periodo periodo) {
        CropReport reporte = CurrentUser.esAdmin(authentication)
                ? generateCropReport.generate()
                : generateCropReport.generateFor(CurrentUser.id(authentication));
        return reporte == null ? null : reporte.en(periodo);
    }

    private ReporteFinanciero finanzas(Authentication authentication, Periodo periodo) {
        ReporteFinanciero reporte = CurrentUser.esAdmin(authentication)
                ? generarReporteFinanciero.generate()
                : generarReporteFinanciero.generateFor(CurrentUser.id(authentication));
        return reporte == null ? null : reporte.en(periodo);
    }
}
