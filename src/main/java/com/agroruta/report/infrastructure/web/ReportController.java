package com.agroruta.report.infrastructure.web;

import com.agroruta.report.application.ports.in.GenerateCropReportUseCase;
import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.infrastructure.pdf.PdfReportService;
import com.agroruta.shared.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final GeneratePayrollReportUseCase generatePayrollReport;
    private final GenerateCropReportUseCase generateCropReport;
    private final PdfReportService pdfReportService;

    @GetMapping(value = "/payroll", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payrollReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generatePayrollPdf(payroll(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payroll-report.pdf")
                .body(pdf);
    }

    @GetMapping(value = "/crops", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> cropReport(Authentication authentication) {
        byte[] pdf = pdfReportService.generateCropPdf(crops(authentication));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=crop-report.pdf")
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
}