package com.agroruta.report.infrastructure.pdf;

import com.agroruta.worker.application.ports.out.ComprobantePdfPort;
import com.agroruta.worker.domain.ComprobantePago;
import com.agroruta.worker.domain.MetodoPago;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static com.agroruta.report.infrastructure.pdf.FormatoReporte.fecha;
import static com.agroruta.report.infrastructure.pdf.FormatoReporte.fechaHora;
import static com.agroruta.report.infrastructure.pdf.FormatoReporte.pesos;
import static com.agroruta.report.infrastructure.pdf.PdfLienzo.*;

/** Comprobante de pago de una página, con un QR que lleva a la verificación pública. */
@Component
public class ComprobantePagoPdf implements ComprobantePdfPort {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final float LADO_QR = 118;

    @Override
    public byte[] generar(ComprobantePago c) {
        try (PdfLienzo pdf = new PdfLienzo("Comprobante de pago", "N.º " + c.numero() + " · Pagado el " + fecha(c.fechaPago()))) {
            pdf.encabezadoDerecha("Generado", fechaHora(LocalDateTime.now(ZONA)));

            // Valor pagado, grande
            float alto = 78;
            pdf.rectRedondo(MARGEN, pdf.y - alto + 12, CONTENIDO, alto, 10, VERDE_FONDO, BORDE);
            pdf.texto("VALOR PAGADO", NEGRITA, 8.5f, MARGEN + 18, pdf.y - 8, TINTA_3);
            pdf.texto(pesos(c.monto()), NEGRITA, 28, MARGEN + 18, pdf.y - 42, VERDE_OSCURO);
            pdf.textoDerecha(metodo(c.metodoPago()), NEGRITA, 11, MARGEN + CONTENIDO - 18, pdf.y - 24, TINTA);
            pdf.textoDerecha(fecha(c.fechaPago()), NORMAL, 10, MARGEN + CONTENIDO - 18, pdf.y - 40, TINTA_2);
            pdf.y -= alto + 12;

            pdf.seccion("Quién recibe");
            fila(pdf, "Trabajador", c.trabajador());
            fila(pdf, "Cédula", c.cedula());

            pdf.seccion("Quién paga");
            fila(pdf, "Empleador", c.empleador());

            pdf.seccion("Detalle del pago");
            if (c.periodoInicio() != null) {
                fila(pdf, "Período trabajado", fecha(c.periodoInicio()) + " a " + fecha(c.periodoFin()));
            }
            fila(pdf, "Jornales", c.jornales() > 0 ? String.valueOf(c.jornales()) : null);
            fila(pdf, "Forma de pago", metodo(c.metodoPago()));
            fila(pdf, "Referencia", c.referencia());
            fila(pdf, "Observaciones", c.observaciones());

            // Firmas
            pdf.y -= 46;
            float mitad = CONTENIDO / 2;
            firma(pdf, MARGEN, mitad - 20, "Entrega (empleador)");
            firma(pdf, MARGEN + mitad + 20, mitad - 20, "Recibe (trabajador)");
            pdf.y -= 34;

            // Verificación
            float altoCaja = LADO_QR + 24;
            pdf.asegurar(altoCaja + 10);
            float base = pdf.y - altoCaja;
            pdf.rectRedondo(MARGEN, base, CONTENIDO, altoCaja, 10, BLANCO, BORDE);
            qr(pdf, c.urlVerificacion(), MARGEN + 12, base + 12, LADO_QR);
            float x = MARGEN + LADO_QR + 30;
            float anchoTexto = CONTENIDO - LADO_QR - 46;
            pdf.texto("Verifica este comprobante", NEGRITA, 12.5f, x, pdf.y - 30, TINTA);
            pdf.texto("Escanea el código con la cámara del celular. La página de AgroRuta", NORMAL, 9.2f, x, pdf.y - 48, TINTA_2);
            pdf.texto("mostrará el valor y la fecha reales de este pago.", NORMAL, 9.2f, x, pdf.y - 60, TINTA_2);
            pdf.texto("Si lo que ves allí es distinto a este papel, el comprobante fue alterado.", NORMAL, 9.2f, x, pdf.y - 72, TINTA_2);
            pdf.texto("CÓDIGO", NEGRITA, 7.5f, x, pdf.y - 94, TINTA_3);
            pdf.texto(c.codigo(), NEGRITA, 11, x, pdf.y - 108, VERDE_OSCURO);
            pdf.texto(ajustar(c.urlVerificacion(), NORMAL, 7.5f, anchoTexto), NORMAL, 7.5f, x, pdf.y - 124, TINTA_3);
            pdf.y = base - 10;

            return pdf.terminar();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el comprobante de pago", e);
        }
    }

    private static void fila(PdfLienzo pdf, String etiqueta, String valor) throws IOException {
        if (valor == null || valor.isBlank()) return;
        pdf.asegurar(20);
        pdf.texto(etiqueta, NORMAL, 9.5f, MARGEN + 10, pdf.y, TINTA_3);
        pdf.texto(ajustar(valor, NEGRITA, 10.5f, CONTENIDO - 150), NEGRITA, 10.5f, MARGEN + 140, pdf.y, TINTA);
        pdf.linea(MARGEN + 10, pdf.y - 6, MARGEN + CONTENIDO, pdf.y - 6, BORDE, 0.6f);
        pdf.y -= 19;
    }

    private static void firma(PdfLienzo pdf, float x, float ancho, String quien) throws IOException {
        pdf.linea(x, pdf.y, x + ancho, pdf.y, TINTA_3, 0.8f);
        pdf.texto(quien, NORMAL, 8.5f, x, pdf.y - 12, TINTA_3);
    }

    private static String metodo(MetodoPago m) {
        if (m == null) return "—";
        return switch (m) {
            case EFECTIVO -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case CHEQUE -> "Cheque";
        };
    }

    /** Dibuja el QR con cuadritos negros (sin imágenes: el PDF pesa menos y no depende de AWT). */
    private static void qr(PdfLienzo pdf, String texto, float x, float yy, float lado) throws IOException {
        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
        } catch (WriterException e) {
            throw new IOException("No se pudo crear el código QR", e);
        }
        int n = m.getWidth();
        float celda = lado / n;
        pdf.rect(x, yy, lado, lado, BLANCO);
        for (int fila = 0; fila < n; fila++) {
            int col = 0;
            while (col < n) {
                if (!m.get(col, fila)) { col++; continue; }
                int inicio = col;
                while (col < n && m.get(col, fila)) col++;
                // Un rectángulo por cada tramo seguido; +0.2 para que no queden rayitas blancas entre celdas
                pdf.rect(x + inicio * celda, yy + lado - (fila + 1) * celda, (col - inicio) * celda + 0.2f, celda + 0.2f, TINTA);
            }
        }
    }
}
