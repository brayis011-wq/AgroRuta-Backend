package com.agroruta.report.infrastructure.pdf;

import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.WorkerPayrollDetail;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfReportService {

    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float MARGIN = 50;
    private static final float ROW_HEIGHT = 20;
    private static final float FONT_SIZE = 10;

    public byte[] generatePayrollPdf(PayrollReport report) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                // título
                y = writeLine(cs, "Reporte de Nómina", BOLD, 18, MARGIN, y);
                y = writeLine(cs, "Generado: " + report.generatedAt(), NORMAL, 9, MARGIN, y - 5);
                y = writeLine(cs, "Total acumulado: $" + report.totalAccumulated(), BOLD, 12, MARGIN, y - 10);

                y -= 20;

                // encabezados tabla
                y = writeLine(cs, "Trabajador", BOLD, FONT_SIZE, MARGIN, y);
                writeAt(cs, "Pagos", BOLD, FONT_SIZE, 300, y + ROW_HEIGHT);
                writeAt(cs, "Total pagado", BOLD, FONT_SIZE, 420, y + ROW_HEIGHT);

                y -= 5;

                // filas
                for (WorkerPayrollDetail d : report.workerDetails()) {
                    y = writeLine(cs, d.workerName(), NORMAL, FONT_SIZE, MARGIN, y);
                    writeAt(cs, String.valueOf(d.totalPagos()), NORMAL, FONT_SIZE, 300, y + ROW_HEIGHT);
                    writeAt(cs, "$" + d.totalPaid(), NORMAL, FONT_SIZE, 420, y + ROW_HEIGHT);
                }
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF de nómina", e);
        }
    }

    public byte[] generateCropPdf(CropReport report) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PDRectangle.A4.getHeight() - MARGIN;

                // título
                y = writeLine(cs, "Reporte de Cultivos", BOLD, 18, MARGIN, y);
                y = writeLine(cs, "Generado: " + report.generatedAt(), NORMAL, 9, MARGIN, y - 5);

                y -= 20;

                // encabezados tabla
                y = writeLine(cs, "Variedad", BOLD, FONT_SIZE, MARGIN, y);
                writeAt(cs, "Estado",          BOLD, FONT_SIZE, 160, y + ROW_HEIGHT);
                writeAt(cs, "Fecha siembra",   BOLD, FONT_SIZE, 260, y + ROW_HEIGHT);
                writeAt(cs, "Días",            BOLD, FONT_SIZE, 360, y + ROW_HEIGHT);
                writeAt(cs, "Kg",              BOLD, FONT_SIZE, 410, y + ROW_HEIGHT);
                writeAt(cs, "Cosechas",        BOLD, FONT_SIZE, 460, y + ROW_HEIGHT);

                y -= 5;

                // filas
                for (CropDetail d : report.cropDetails()) {
                    y = writeLine(cs, d.variedad(), NORMAL, FONT_SIZE, MARGIN, y);
                    writeAt(cs, d.estado(),                                          NORMAL, FONT_SIZE, 160, y + ROW_HEIGHT);
                    writeAt(cs, d.fechaSiembra().toString(),                         NORMAL, FONT_SIZE, 260, y + ROW_HEIGHT);
                    writeAt(cs, String.valueOf(d.diasDesdeSiembra()),                NORMAL, FONT_SIZE, 360, y + ROW_HEIGHT);
                    writeAt(cs, d.totalKgCosechado() != null ? d.totalKgCosechado().toString() : "0", NORMAL, FONT_SIZE, 410, y + ROW_HEIGHT);
                    writeAt(cs, String.valueOf(d.totalCosechas()),                   NORMAL, FONT_SIZE, 460, y + ROW_HEIGHT);
                }
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF de cultivos", e);
        }
    }

    private float writeLine(PDPageContentStream cs, String text, PDType1Font font, float size, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y - ROW_HEIGHT;
    }

    private void writeAt(PDPageContentStream cs, String text, PDType1Font font, float size, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }
}