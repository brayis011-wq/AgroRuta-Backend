package com.agroruta.report.infrastructure.web;

import com.agroruta.report.application.ports.in.GenerateCropReportUseCase;
import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.application.ports.in.GenerarReporteFinancieroUseCase;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.ReporteFinanciero;
import com.agroruta.report.infrastructure.pdf.PdfReportService;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GeneratePayrollReportUseCase generatePayrollReport;
    private final GenerateCropReportUseCase generateCropReport;
    private final PdfReportService pdfReportService;
    private final GenerarReporteFinancieroUseCase generarReporteFinanciero;

    @GetMapping(value = "/payroll", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payrollReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generatePayrollPdf(payroll(authentication), CurrentUser.nombre(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-nomina.pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/crops", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> cropReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generateCropPdf(crops(authentication), CurrentUser.nombre(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-cultivos.pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/costs", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> costsReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generateCostsPdf(finanzas(authentication), CurrentUser.nombre(authentication));
        return adjunto(pdf, "reporte-costos.pdf");
    }

    @GetMapping(value = "/profitability", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> profitabilityReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generateProfitabilityPdf(finanzas(authentication), CurrentUser.nombre(authentication));
        return adjunto(pdf, "reporte-rentabilidad.pdf");
    }

    /**
     * Varias partes en un solo PDF: {@code ?sections=payroll,crops,costs,profitability} (sin el parámetro, todas).
     * Solo se consultan los datos de las partes pedidas.
     */
    @GetMapping(value = "/full", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> fullReport(@RequestParam(required = false) List<String> sections,
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
        boolean conCostos = partes.contains("costs");
        boolean conRentabilidad = partes.contains("profitability");
        byte[] pdf = pdfReportService.generateCompletePdf(
                partes.contains("payroll") ? payroll(authentication) : null,
                partes.contains("crops") ? crops(authentication) : null,
                conCostos || conRentabilidad ? finanzas(authentication) : null,
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
    public ResponseEntity<PayrollReport> payrollPreview(Authentication authentication) {
        return ResponseEntity.ok(payroll(authentication));
    }

    @GetMapping(value = "/crops/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CropReport> cropPreview(Authentication authentication) {
        return ResponseEntity.ok(crops(authentication));
    }

    /** Administrador: todo. Resto: solo lo de su propio usuario. */
    private PayrollReport payroll(Authentication authentication) {
        return CurrentUser.esAdmin(authentication)
                ? generatePayrollReport.generate()
                : generatePayrollReport.generateFor(CurrentUser.id(authentication));
    }

    private CropReport crops(Authentication authentication) {
        return CurrentUser.esAdmin(authentication)
                ? generateCropReport.generate()
                : generateCropReport.generateFor(CurrentUser.id(authentication));
    }

    private ReporteFinanciero finanzas(Authentication authentication) {
        return CurrentUser.esAdmin(authentication)
                ? generarReporteFinanciero.generate()
                : generarReporteFinanciero.generateFor(CurrentUser.id(authentication));
    }
}