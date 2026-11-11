package com.agroruta.notification.domain;

import java.nio.charset.StandardCharsets;

/**
 * Lo que muestra la notificación del teléfono.
 *
 * @param url      pantalla de la app que se abre al tocarla (ruta, p. ej. /dashboard)
 * @param etiqueta avisos con la misma etiqueta se reemplazan en vez de apilarse
 */
public record AvisoPush(String titulo, String cuerpo, String url, String etiqueta) {

    private static final int MAX_TITULO = 80;
    private static final int MAX_CUERPO = 320;

    /** El JSON que recibe el service worker (public/sw.js del frontend) */
    public byte[] json() {
        return ("{\"title\":" + cadena(recortar(titulo, MAX_TITULO))
                + ",\"body\":" + cadena(recortar(cuerpo, MAX_CUERPO))
                + ",\"url\":" + cadena(url == null ? "/" : url)
                + ",\"tag\":" + cadena(etiqueta == null ? "agroruta" : etiqueta) + "}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    private static String cadena(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 2).append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
