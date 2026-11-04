package com.agroruta.report.infrastructure.web;

import com.agroruta.report.application.ports.in.GenerateCropReportUseCase;
import com.agroruta.report.application.ports.in.GeneratePayrollReportUseCase;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.report.infrastructure.pdf.PdfReportService;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportController - Pruebas Unitarias")
class ReportControllerTest {


    @Mock
    private GeneratePayrollReportUseCase generatePayrollReport;

    @Mock
    private GenerateCropReportUseCase generateCropReport;

    @Mock
    private PdfReportService pdfReportService;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;

    private PayrollReport payrollReportEjemplo;
    private CropReport cropReportEjemplo;
    private byte[] pdfBytes;
    private Authentication admin;

    private Authentication auth(Long id, Rol rol) {
        CustomUserDetails d = new CustomUserDetails(new Usuario(id, "Nombre", "a@a.com", "pw", rol));
        return new UsernamePasswordAuthenticationToken(d, null, d.getAuthorities());
    }

    @BeforeEach
    void configurar() {

        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();

        admin = auth(99L, Rol.Administrador);

        pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};

        payrollReportEjemplo = new PayrollReport(
                LocalDateTime.now(),
                new BigDecimal("3500000"),
                List.of(
                        new WorkerPayrollDetail(
                                1L,
                                "Carlos López",
                                4,
                                new BigDecimal("3500000"),
                                LocalDate.of(2025, 1, 1),
                                LocalDate.of(2025, 1, 31),
                                20
                        )
                )
        );

        cropReportEjemplo = new CropReport(
                LocalDateTime.now(),
                List.of(
                        new CropDetail(
                                10L,
                                "AURORA",
                                "EN_PRODUCCION",
                                LocalDate.of(2025, 3, 1),
                                90L,
                                400.0,
                                2
                        )
                )
        );
    }

    @Nested
    @DisplayName("Endpoints PDF Payroll")
    class PayrollPdfTests {

        @Test
        @DisplayName("Debe retornar PDF payroll correctamente")
        void deberiaRetornarPdfPayroll() throws Exception {

            when(generatePayrollReport.generate())
                    .thenReturn(payrollReportEjemplo);

            when(pdfReportService.generatePayrollPdf(eq(payrollReportEjemplo), any()))
                    .thenReturn(pdfBytes);

            mockMvc.perform(get("/api/reports/payroll")
                            .principal(admin)
                            .accept(MediaType.APPLICATION_PDF))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(pdfBytes))
                    .andExpect(header().string(
                            "Content-Disposition",
                            "attachment; filename=\"reporte-nomina.pdf\""
                    ));

            verify(generatePayrollReport, times(1)).generate();

            verify(pdfReportService, times(1))
                    .generatePayrollPdf(payrollReportEjemplo, "Nombre");

            verifyNoInteractions(generateCropReport);
        }

        @Test
        @DisplayName("Debe propagar excepción cuando falla payroll")
        void deberiaPropagarErrorPayroll() {

            when(generatePayrollReport.generate())
                    .thenThrow(new RuntimeException("Error generando reporte"));

            assertThatThrownBy(() ->
                    mockMvc.perform(get("/api/reports/payroll").principal(admin)))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error generando reporte");
        }
    }

    @Nested
    @DisplayName("Endpoints PDF Crop")
    class CropPdfTests {

        @Test
        @DisplayName("Debe retornar PDF crop correctamente")
        void deberiaRetornarPdfCrop() throws Exception {

            when(generateCropReport.generate())
                    .thenReturn(cropReportEjemplo);

            when(pdfReportService.generateCropPdf(eq(cropReportEjemplo), any()))
                    .thenReturn(pdfBytes);

            mockMvc.perform(get("/api/reports/crops")
                            .principal(admin)
                            .accept(MediaType.APPLICATION_PDF))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(pdfBytes))
                    .andExpect(header().string(
                            "Content-Disposition",
                            "attachment; filename=\"reporte-cultivos.pdf\""
                    ));

            verify(generateCropReport, times(1)).generate();

            verify(pdfReportService, times(1))
                    .generateCropPdf(cropReportEjemplo, "Nombre");

            verifyNoInteractions(generatePayrollReport);
        }

        @Test
        @DisplayName("Debe propagar excepción cuando falla crop")
        void deberiaPropagarErrorCrop() {

            when(generateCropReport.generate())
                    .thenThrow(new RuntimeException("Error generando reporte"));

            assertThatThrownBy(() ->
                    mockMvc.perform(get("/api/reports/crops").principal(admin)))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Error generando reporte");
        }
    }

    @Nested
    @DisplayName("Preview Payroll JSON")
    class PayrollPreviewTests {

        @Test
        @DisplayName("Debe retornar preview payroll")
        void deberiaRetornarPreviewPayroll() throws Exception {

            when(generatePayrollReport.generate())
                    .thenReturn(payrollReportEjemplo);

            mockMvc.perform(get("/api/reports/payroll/preview")
                            .principal(admin)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(generatePayrollReport, times(1)).generate();

            verifyNoInteractions(pdfReportService);
        }

        @Test
        @DisplayName("Debe propagar excepción preview payroll")
        void deberiaPropagarPreviewPayroll() {

            when(generatePayrollReport.generate())
                    .thenThrow(new RuntimeException("Error preview payroll"));

            assertThatThrownBy(() ->
                    mockMvc.perform(get("/api/reports/payroll/preview").principal(admin)))
                    .hasRootCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Preview Crop JSON")
    class CropPreviewTests {

        @Test
        @DisplayName("Debe retornar preview crop")
        void deberiaRetornarPreviewCrop() throws Exception {

            when(generateCropReport.generate())
                    .thenReturn(cropReportEjemplo);

            mockMvc.perform(get("/api/reports/crops/preview")
                            .principal(admin)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(generateCropReport, times(1)).generate();

            verifyNoInteractions(pdfReportService);
        }

        @Test
        @DisplayName("Debe propagar excepción preview crop")
        void deberiaPropagarPreviewCrop() {

            when(generateCropReport.generate())
                    .thenThrow(new RuntimeException("Error preview crop"));

            assertThatThrownBy(() ->
                    mockMvc.perform(get("/api/reports/crops/preview").principal(admin)))
                    .hasRootCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Alcance por agricultor")
    class AlcancePorAgricultor {

        @Test
        @DisplayName("Un agricultor recibe solo su reporte de nómina")
        void agricultorUsaGenerateForEnPayroll() throws Exception {

            when(generatePayrollReport.generateFor(5L)).thenReturn(payrollReportEjemplo);

            mockMvc.perform(get("/api/reports/payroll/preview")
                            .principal(auth(5L, Rol.Agricultor))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(generatePayrollReport).generateFor(5L);
            verify(generatePayrollReport, never()).generate();
        }

        @Test
        @DisplayName("Un agricultor recibe solo su reporte de cultivos")
        void agricultorUsaGenerateForEnCrops() throws Exception {

            when(generateCropReport.generateFor(5L)).thenReturn(cropReportEjemplo);

            mockMvc.perform(get("/api/reports/crops/preview")
                            .principal(auth(5L, Rol.Agricultor))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(generateCropReport).generateFor(5L);
            verify(generateCropReport, never()).generate();
        }
    }

}