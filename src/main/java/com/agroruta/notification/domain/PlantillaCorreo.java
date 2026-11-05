package com.agroruta.notification.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma los correos con el mismo diseño: encabezado verde de AgroRuta, una tarjeta blanca con el
 * contenido y un pie. Todo con estilos en línea y tablas, que es lo que respetan Gmail y Outlook.
 * Cada bloque se escribe también en la versión de texto plano.
 */
public final class PlantillaCorreo {

    static final String VERDE = "#2e7d32";
    static final String VERDE_OSCURO = "#1b5e20";
    static final String TINTA = "#1f2a1f";
    static final String GRIS = "#5f6b5f";

    private final String titulo;
    private final StringBuilder html = new StringBuilder();
    private final StringBuilder texto = new StringBuilder();
    private String resumen = "";
    private final List<String> pie = new ArrayList<>();

    private PlantillaCorreo(String titulo) {
        this.titulo = titulo;
        texto.append(titulo).append("\n\n");
    }

    public static PlantillaCorreo con(String titulo) {
        return new PlantillaCorreo(titulo);
    }

    /** Línea que muestran algunos clientes junto al asunto, antes de abrir el correo */
    public PlantillaCorreo resumen(String resumen) {
        this.resumen = resumen == null ? "" : resumen;
        return this;
    }

    public PlantillaCorreo parrafo(String t) {
        html.append("<p style=\"margin:0 0 14px;font-size:15px;line-height:1.55;color:").append(TINTA).append("\">")
                .append(escapar(t)).append("</p>");
        texto.append(t).append("\n\n");
        return this;
    }

    public PlantillaCorreo seccion(String t) {
        html.append("<h2 style=\"margin:22px 0 8px;font-size:15px;color:").append(VERDE_OSCURO).append("\">")
                .append(escapar(t)).append("</h2>");
        texto.append(t.toUpperCase()).append("\n");
        return this;
    }

    /** Lista con una parte en negrita (lote, producto...) y el resto normal */
    public PlantillaCorreo lista(List<String[]> items) {
        html.append("<ul style=\"margin:0 0 14px;padding-left:20px;font-size:14px;line-height:1.55;color:")
                .append(TINTA).append("\">");
        for (String[] it : items) {
            String fuerte = it.length > 0 ? it[0] : "";
            String resto = it.length > 1 ? it[1] : "";
            html.append("<li style=\"margin:0 0 6px\"><strong>").append(escapar(fuerte)).append("</strong>")
                    .append(resto.isEmpty() ? "" : " " + escapar(resto)).append("</li>");
            texto.append(" - ").append(fuerte).append(resto.isEmpty() ? "" : " " + resto).append("\n");
        }
        html.append("</ul>");
        texto.append("\n");
        return this;
    }

    /** Recuadro de color para lo importante */
    public PlantillaCorreo aviso(String t) {
        html.append("<div style=\"margin:0 0 14px;padding:12px 14px;border-radius:10px;background:#fff7e0;"
                        + "border:1px solid #f1d38a;font-size:14px;line-height:1.5;color:#6b4e00\">")
                .append(escapar(t)).append("</div>");
        texto.append("! ").append(t).append("\n\n");
        return this;
    }

    public PlantillaCorreo boton(String etiqueta, String url) {
        html.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin:8px 0 18px\"><tr>")
                .append("<td style=\"border-radius:10px;background:").append(VERDE).append("\">")
                .append("<a href=\"").append(escapar(url)).append("\" style=\"display:inline-block;padding:12px 22px;")
                .append("font-size:15px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:10px\">")
                .append(escapar(etiqueta)).append("</a></td></tr></table>");
        texto.append(etiqueta).append(": ").append(url).append("\n\n");
        return this;
    }

    public PlantillaCorreo pie(String t) {
        pie.add(t);
        return this;
    }

    public Correo para(String email, String asunto) {
        StringBuilder h = new StringBuilder(4096);
        h.append("<!doctype html><html lang=\"es\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escapar(asunto)).append("</title></head>")
                .append("<body style=\"margin:0;padding:0;background:#f3f6f1;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif\">")
                .append("<span style=\"display:none;max-height:0;overflow:hidden;opacity:0\">").append(escapar(resumen)).append("</span>")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background:#f3f6f1\">")
                .append("<tr><td align=\"center\" style=\"padding:24px 12px\">")
                .append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:560px\">")
                .append("<tr><td style=\"padding:0 4px 14px;font-size:20px;font-weight:bold;color:").append(VERDE)
                .append("\">&#127793; AgroRuta</td></tr>")
                .append("<tr><td style=\"background:#ffffff;border:1px solid #e2e8df;border-radius:16px;padding:26px 24px\">")
                .append("<h1 style=\"margin:0 0 16px;font-size:20px;line-height:1.3;color:").append(TINTA).append("\">")
                .append(escapar(titulo)).append("</h1>")
                .append(html)
                .append("</td></tr>");
        if (!pie.isEmpty()) {
            h.append("<tr><td style=\"padding:14px 8px;font-size:12px;line-height:1.5;color:").append(GRIS).append("\">");
            for (String p : pie) h.append(escapar(p)).append("<br>");
            h.append("</td></tr>");
        }
        h.append("</table></td></tr></table></body></html>");

        StringBuilder t = new StringBuilder(texto);
        if (!pie.isEmpty()) t.append("--\n").append(String.join("\n", pie)).append("\n");
        return new Correo(email, asunto, h.toString(), t.toString());
    }

    static String escapar(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '&' -> b.append("&amp;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
