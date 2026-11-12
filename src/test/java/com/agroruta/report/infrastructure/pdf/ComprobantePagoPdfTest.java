package com.agroruta.report.infrastructure.pdf;

import com.agroruta.worker.domain.ComprobantePago;
import com.agroruta.worker.domain.MetodoPago;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComprobantePagoPdf")
class ComprobantePagoPdfTest {

    private final ComprobantePagoPdf pdf = new ComprobantePagoPdf();

    private static ComprobantePago comprobante(String referencia, String observaciones) {
        return new ComprobantePago(12L, "CP-000012", LocalDate.of(2026, 10, 16), new BigDecimal("450000"),
                MetodoPago.TRANSFERENCIA, referencia, observaciones, "Juan Pérez", "1052334120", "María Gómez",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), 6, "abcDEF123456_-xy",
                "https://app.ejemplo.test/receipt/12/abcDEF123456_-xy");
    }

    @Test
    @DisplayName("genera un PDF de una página con los datos del pago")
    void generaElPdf() throws Exception {
        byte[] bytes = pdf.generar(comprobante("TRX-889", "Pago de la quincena"));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(texto.contains("Comprobante de pago"));
            assertTrue(texto.contains("CP-000012"));
            assertTrue(texto.contains("$450.000"));
            assertTrue(texto.contains("Juan Pérez"));
            assertTrue(texto.contains("María Gómez"));
            assertTrue(texto.contains("Transferencia"));
            assertTrue(texto.contains("TRX-889"));
            assertTrue(texto.contains("abcDEF123456_-xy"));
        }
    }

    @Test
    @DisplayName("no falla si faltan los datos opcionales")
    void sinDatosOpcionales() {
        ComprobantePago c = new ComprobantePago(1L, "CP-000001", LocalDate.of(2026, 1, 2), new BigDecimal("1000"),
                null, null, null, null, null, null, null, null, 0, "AAAAAAAAAAAAAAAA", "http://localhost/receipt/1/AAAAAAAAAAAAAAAA");
        assertTrue(pdf.generar(c).length > 500);
    }
}
