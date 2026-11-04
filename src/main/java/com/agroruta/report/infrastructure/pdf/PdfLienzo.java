package com.agroruta.report.infrastructure.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Lienzo sencillo sobre PDFBox para los reportes: maneja páginas, encabezado,
 * pie con "Página X de Y", texto que no se sale de su ancho, rectángulos redondeados y tablas.
 * Las coordenadas de PDF crecen hacia arriba; {@link #y} es la posición actual de escritura.
 */
final class PdfLienzo implements AutoCloseable {

    static final PDType1Font NEGRITA = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    static final PDType1Font NORMAL = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    static final Color VERDE = new Color(0x2E, 0x7D, 0x32);
    static final Color VERDE_OSCURO = new Color(0x1B, 0x5E, 0x20);
    static final Color VERDE_SUAVE = new Color(0xE6, 0xF2, 0xE7);
    static final Color VERDE_FONDO = new Color(0xF3, 0xF8, 0xF3);
    static final Color TINTA = new Color(0x17, 0x23, 0x1C);
    static final Color TINTA_2 = new Color(0x4B, 0x5A, 0x51);
    static final Color TINTA_3 = new Color(0x7D, 0x8A, 0x82);
    static final Color BORDE = new Color(0xE3, 0xE9, 0xE4);
    static final Color ZEBRA = new Color(0xF8, 0xFA, 0xF8);
    static final Color AMBAR = new Color(0xB2, 0x6A, 0x00);
    static final Color BLANCO = Color.WHITE;

    static final float ANCHO = PDRectangle.A4.getWidth();
    static final float ALTO = PDRectangle.A4.getHeight();
    static final float MARGEN = 40;
    static final float CONTENIDO = ANCHO - 2 * MARGEN;
    private static final float PIE = 46;

    private final PDDocument doc = new PDDocument();
    private final String titulo;
    private final String subtitulo;
    private PDPageContentStream cs;
    float y;

    PdfLienzo(String titulo, String subtitulo) throws IOException {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        doc.getDocumentInformation().setTitle(titulo);
        doc.getDocumentInformation().setAuthor("AgroRuta");
        doc.getDocumentInformation().setCreator("AgroRuta");
        nuevaPagina();
    }

    // ── Páginas ────────────────────────────────────────────────────────────

    private void nuevaPagina() throws IOException {
        if (cs != null) cs.close();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        boolean primera = doc.getNumberOfPages() == 1;
        float alto = primera ? 96 : 44;
        rect(0, ALTO - alto, ANCHO, alto, VERDE);
        // franja decorativa más oscura al borde inferior del encabezado
        rect(0, ALTO - alto, ANCHO, 3, VERDE_OSCURO);
        if (primera) {
            texto("AGRORUTA", NEGRITA, 9, MARGEN, ALTO - 30, new Color(0xC8, 0xE6, 0xC9));
            texto(titulo, NEGRITA, 22, MARGEN, ALTO - 58, BLANCO);
            texto(subtitulo, NORMAL, 9.5f, MARGEN, ALTO - 78, new Color(0xDF, 0xEF, 0xE0));
            y = ALTO - alto - 26;
        } else {
            texto("AgroRuta · " + titulo, NEGRITA, 11, MARGEN, ALTO - 27, BLANCO);
            y = ALTO - alto - 22;
        }
    }

    /** Pasa a otra página si no caben {@code alto} puntos más. Devuelve true si cambió de página. */
    boolean asegurar(float alto) throws IOException {
        if (y - alto < PIE + 14) {
            nuevaPagina();
            return true;
        }
        return false;
    }

    /** Texto a la derecha del encabezado de la primera página (p. ej. fecha de generación). */
    void encabezadoDerecha(String linea1, String linea2) throws IOException {
        float x = ANCHO - MARGEN;
        if (linea1 != null) textoDerecha(linea1, NORMAL, 8.5f, x, ALTO - 58, new Color(0xDF, 0xEF, 0xE0));
        if (linea2 != null) textoDerecha(linea2, NEGRITA, 9.5f, x, ALTO - 74, BLANCO);
    }

    // ── Dibujo básico ──────────────────────────────────────────────────────

    void rect(float x, float yy, float w, float h, Color relleno) throws IOException {
        cs.setNonStrokingColor(relleno);
        cs.addRect(x, yy, w, h);
        cs.fill();
    }

    void linea(float x1, float y1, float x2, float y2, Color color, float grosor) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(grosor);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /** Rectángulo con esquinas redondeadas (relleno y, si se da, borde) */
    void rectRedondo(float x, float yy, float w, float h, float r, Color relleno, Color borde) throws IOException {
        r = Math.min(r, Math.min(w, h) / 2);
        float k = 0.5523f * r; // aproximación de un cuarto de círculo con Bézier
        cs.moveTo(x + r, yy);
        cs.lineTo(x + w - r, yy);
        cs.curveTo(x + w - r + k, yy, x + w, yy + r - k, x + w, yy + r);
        cs.lineTo(x + w, yy + h - r);
        cs.curveTo(x + w, yy + h - r + k, x + w - r + k, yy + h, x + w - r, yy + h);
        cs.lineTo(x + r, yy + h);
        cs.curveTo(x + r - k, yy + h, x, yy + h - r + k, x, yy + h - r);
        cs.lineTo(x, yy + r);
        cs.curveTo(x, yy + r - k, x + r - k, yy, x + r, yy);
        cs.closePath();
        cs.setNonStrokingColor(relleno);
        if (borde != null) {
            cs.setStrokingColor(borde);
            cs.setLineWidth(0.8f);
            cs.fillAndStroke();
        } else {
            cs.fill();
        }
    }

    // ── Texto ──────────────────────────────────────────────────────────────

    /** Reemplaza caracteres que la fuente estándar no tiene (emojis, flechas...) */
    static String seguro(String t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); ) {
            int cp = t.codePointAt(i);
            // espacios "raros" (no separable, angosto) → espacio normal
            if (cp == 0x00A0 || cp == 0x202F || cp == 0x2009) cp = ' ';
            String c = new String(Character.toChars(cp));
            try {
                NORMAL.encode(c);
                sb.append(c);
            } catch (IOException | IllegalArgumentException e) {
                sb.append(cp == '→' ? "-" : "?");
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    static float ancho(String t, PDType1Font f, float size) {
        try {
            return f.getStringWidth(seguro(t)) / 1000f * size;
        } catch (IOException e) {
            return t.length() * size * 0.5f;
        }
    }

    /** Recorta con "…" para que el texto quepa en {@code max} puntos */
    static String ajustar(String t, PDType1Font f, float size, float max) {
        String s = seguro(t);
        if (ancho(s, f, size) <= max) return s;
        String elipsis = "…";
        while (!s.isEmpty() && ancho(s + elipsis, f, size) > max) s = s.substring(0, s.length() - 1);
        return s.stripTrailing() + elipsis;
    }

    void texto(String t, PDType1Font f, float size, float x, float yy, Color color) throws IOException {
        cs.beginText();
        cs.setFont(f, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, yy);
        cs.showText(seguro(t));
        cs.endText();
    }

    void textoDerecha(String t, PDType1Font f, float size, float xDerecha, float yy, Color color) throws IOException {
        texto(t, f, size, xDerecha - ancho(t, f, size), yy, color);
    }

    void textoCentro(String t, PDType1Font f, float size, float xCentro, float yy, Color color) throws IOException {
        texto(t, f, size, xCentro - ancho(t, f, size) / 2, yy, color);
    }

    /** Título de sección con una barrita verde a la izquierda */
    void seccion(String t) throws IOException {
        asegurar(40);
        y -= 8;
        rectRedondo(MARGEN, y - 3, 3.5f, 15, 1.5f, VERDE, null);
        texto(t, NEGRITA, 12.5f, MARGEN + 10, y, TINTA);
        y -= 20;
    }

    /** Párrafo gris de una línea (p. ej. "No hay datos todavía") */
    void nota(String t) throws IOException {
        asegurar(20);
        texto(t, NORMAL, 9.5f, MARGEN, y, TINTA_3);
        y -= 18;
    }

    // ── Tarjetas de resumen ────────────────────────────────────────────────

    record Tarjeta(String etiqueta, String valor, String detalle, Color acento) {}

    void tarjetas(List<Tarjeta> ts) throws IOException {
        float alto = 62;
        asegurar(alto + 10);
        float gap = 10;
        float w = (CONTENIDO - gap * (ts.size() - 1)) / ts.size();
        float base = y - alto;
        for (int i = 0; i < ts.size(); i++) {
            Tarjeta t = ts.get(i);
            float x = MARGEN + i * (w + gap);
            rectRedondo(x, base, w, alto, 8, VERDE_FONDO, BORDE);
            rectRedondo(x + 10, base + alto - 14, 18, 3, 1.5f, t.acento(), null);
            texto(ajustar(t.etiqueta(), NORMAL, 8, w - 20), NORMAL, 8, x + 10, base + alto - 26, TINTA_3);
            texto(ajustar(t.valor(), NEGRITA, 15, w - 20), NEGRITA, 15, x + 10, base + 22, TINTA);
            if (t.detalle() != null) texto(ajustar(t.detalle(), NORMAL, 7.5f, w - 20), NORMAL, 7.5f, x + 10, base + 9, TINTA_3);
        }
        y = base - 16;
    }

    // ── Gráficas ───────────────────────────────────────────────────────────

    record Barra(String etiqueta, double valor, String valorTexto, String detalle) {}

    /** Barras verticales (p. ej. pagos por mes), con el valor encima de cada barra */
    void barrasVerticales(List<Barra> barras, float alto) throws IOException {
        float total = alto + 34;
        asegurar(total + 6);
        double max = barras.stream().mapToDouble(Barra::valor).max().orElse(0);
        float base = y - alto - 16;
        // líneas guía
        for (int i = 0; i <= 4; i++) {
            float gy = base + alto * i / 4f;
            linea(MARGEN, gy, MARGEN + CONTENIDO, gy, i == 0 ? BORDE : new Color(0xF0, 0xF3, 0xF0), i == 0 ? 0.9f : 0.6f);
        }
        float slot = CONTENIDO / Math.max(1, barras.size());
        float w = Math.min(46, slot * 0.55f);
        for (int i = 0; i < barras.size(); i++) {
            Barra b = barras.get(i);
            float cx = MARGEN + slot * i + slot / 2;
            float h = max > 0 ? (float) (b.valor() / max) * (alto - 14) : 0;
            boolean ultimo = i == barras.size() - 1;
            if (h > 0) rectRedondo(cx - w / 2, base, w, Math.max(h, 2), Math.min(4, h / 2), ultimo ? VERDE : new Color(0x81, 0xC7, 0x84), null);
            if (b.valor() > 0) textoCentro(b.valorTexto(), NEGRITA, 7.8f, cx, base + h + 5, TINTA_2);
            textoCentro(b.etiqueta(), ultimo ? NEGRITA : NORMAL, 8.2f, cx, base - 12, ultimo ? TINTA : TINTA_3);
        }
        y = base - 28;
    }

    /** Barras horizontales (p. ej. pagado por trabajador), con el valor al final */
    void barrasHorizontales(List<Barra> barras) throws IOException {
        double max = barras.stream().mapToDouble(Barra::valor).max().orElse(0);
        float colNombre = 150, colValor = 105;
        float pista = CONTENIDO - colNombre - colValor - 10;
        for (Barra b : barras) {
            asegurar(22);
            float base = y - 14;
            texto(ajustar(b.etiqueta(), NORMAL, 9, colNombre - 8), NORMAL, 9, MARGEN, base + 3, TINTA);
            float x = MARGEN + colNombre;
            rectRedondo(x, base + 1, pista, 9, 4.5f, VERDE_SUAVE, null);
            float w = max > 0 ? (float) (b.valor() / max) * pista : 0;
            if (w > 0) rectRedondo(x, base + 1, Math.max(w, 9), 9, 4.5f, VERDE, null);
            float xv = MARGEN + CONTENIDO;
            if (b.detalle() != null) {
                textoDerecha(b.detalle(), NORMAL, 7.8f, xv, base + 3, TINTA_3);
                xv -= ancho(b.detalle(), NORMAL, 7.8f) + 6;
            }
            textoDerecha(b.valorTexto(), NEGRITA, 9, xv, base + 3, TINTA);
            y -= 21;
        }
        y -= 8;
    }

    record Segmento(String etiqueta, int cantidad, Color color) {}

    /** Una barra repartida en partes (p. ej. siembras por etapa) con su leyenda */
    void barraApilada(List<Segmento> segs) throws IOException {
        asegurar(50);
        int total = segs.stream().mapToInt(Segmento::cantidad).sum();
        float base = y - 16;
        float x = MARGEN;
        rectRedondo(MARGEN, base, CONTENIDO, 14, 7, VERDE_SUAVE, null);
        for (Segmento s : segs) {
            if (s.cantidad() == 0 || total == 0) continue;
            float w = CONTENIDO * s.cantidad() / total;
            rect(x, base, w, 14, s.color());
            if (w > 22) textoCentro(String.valueOf(s.cantidad()), NEGRITA, 8, x + w / 2, base + 4, BLANCO);
            x += w;
        }
        // leyenda
        float ly = base - 16, lx = MARGEN;
        for (Segmento s : segs) {
            String t = s.etiqueta() + " (" + s.cantidad() + ")";
            float tw = ancho(t, NORMAL, 8.5f) + 22;
            if (lx + tw > MARGEN + CONTENIDO) { lx = MARGEN; ly -= 14; }
            rectRedondo(lx, ly - 1, 9, 9, 2, s.color(), null);
            texto(t, NORMAL, 8.5f, lx + 13, ly, s.cantidad() > 0 ? TINTA_2 : TINTA_3);
            lx += tw;
        }
        y = ly - 20;
    }

    // ── Tablas ─────────────────────────────────────────────────────────────

    record Columna(String titulo, float ancho, boolean derecha) {}

    /** Celda: texto y, opcionalmente, una "píldora" de color detrás (para estados) */
    record Celda(String texto, Color pildora, Color colorTexto, boolean negrita) {
        static Celda de(String t) { return new Celda(t, null, null, false); }
        static Celda negrita(String t) { return new Celda(t, null, null, true); }
    }

    void tabla(List<Columna> cols, List<List<Celda>> filas) throws IOException {
        float altoFila = 21;
        cabeceraTabla(cols);
        for (int i = 0; i < filas.size(); i++) {
            if (asegurar(altoFila)) cabeceraTabla(cols);
            float base = y - altoFila;
            if (i % 2 == 1) rect(MARGEN, base, CONTENIDO, altoFila, ZEBRA);
            float x = MARGEN;
            List<Celda> fila = filas.get(i);
            for (int c = 0; c < cols.size(); c++) {
                Columna col = cols.get(c);
                Celda celda = c < fila.size() ? fila.get(c) : Celda.de("");
                PDType1Font f = celda.negrita() ? NEGRITA : NORMAL;
                float size = 8.8f;
                String t = ajustar(celda.texto(), f, size, col.ancho() - 14);
                float tw = ancho(t, f, size);
                float tx = col.derecha() ? x + col.ancho() - 7 - tw : x + 7;
                if (celda.pildora() != null) {
                    rectRedondo(tx - 5, base + 4.5f, tw + 10, altoFila - 9, 6, celda.pildora(), null);
                }
                texto(t, f, size, tx, base + 7.5f, celda.colorTexto() != null ? celda.colorTexto() : TINTA);
                x += col.ancho();
            }
            y = base;
        }
        linea(MARGEN, y, MARGEN + CONTENIDO, y, BORDE, 0.8f);
        y -= 14;
    }

    private void cabeceraTabla(List<Columna> cols) throws IOException {
        asegurar(48);
        float alto = 22;
        float base = y - alto;
        rectRedondo(MARGEN, base, CONTENIDO, alto, 5, VERDE, null);
        float x = MARGEN;
        for (Columna c : cols) {
            String t = ajustar(c.titulo().toUpperCase(), NEGRITA, 7.6f, c.ancho() - 14);
            if (c.derecha()) textoDerecha(t, NEGRITA, 7.6f, x + c.ancho() - 7, base + 8, BLANCO);
            else texto(t, NEGRITA, 7.6f, x + 7, base + 8, BLANCO);
            x += c.ancho();
        }
        y = base;
    }

    // ── Cierre ─────────────────────────────────────────────────────────────

    byte[] terminar() throws IOException {
        cs.close();
        cs = null;
        int total = doc.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = doc.getPage(i);
            try (PDPageContentStream pie = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                pie.setStrokingColor(BORDE);
                pie.setLineWidth(0.8f);
                pie.moveTo(MARGEN, PIE);
                pie.lineTo(ANCHO - MARGEN, PIE);
                pie.stroke();
                escribir(pie, "AgroRuta · " + titulo, NORMAL, 8, MARGEN, PIE - 14, TINTA_3);
                String pag = "Página " + (i + 1) + " de " + total;
                escribir(pie, pag, NORMAL, 8, ANCHO - MARGEN - ancho(pag, NORMAL, 8), PIE - 14, TINTA_3);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static void escribir(PDPageContentStream s, String t, PDType1Font f, float size, float x, float yy, Color c) throws IOException {
        s.beginText();
        s.setFont(f, size);
        s.setNonStrokingColor(c);
        s.newLineAtOffset(x, yy);
        s.showText(seguro(t));
        s.endText();
    }

    @Override
    public void close() throws IOException {
        if (cs != null) cs.close();
        doc.close();
    }
}
