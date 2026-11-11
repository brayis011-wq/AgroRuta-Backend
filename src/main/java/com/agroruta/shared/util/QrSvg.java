package com.agroruta.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Código QR como imagen SVG, lista para poner en el src de un &lt;img&gt;. */
public final class QrSvg {

    private QrSvg() {}

    /** "data:image/svg+xml;base64,..." con el QR del texto */
    public static String dataUri(String texto) {
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg(texto).getBytes(StandardCharsets.UTF_8));
    }

    static String svg(String texto) {
        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo crear el código QR", e);
        }
        int n = m.getWidth();
        StringBuilder d = new StringBuilder();
        for (int y = 0; y < n; y++) {
            int x = 0;
            while (x < n) {
                if (!m.get(x, y)) { x++; continue; }
                int inicio = x;
                while (x < n && m.get(x, y)) x++;
                d.append('M').append(inicio).append(' ').append(y).append('h').append(x - inicio).append("v1h-")
                        .append(x - inicio).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + n + " " + n
                + "\" shape-rendering=\"crispEdges\"><rect width=\"" + n + "\" height=\"" + n
                + "\" fill=\"#fff\"/><path fill=\"#000\" d=\"" + d + "\"/></svg>";
    }
}
