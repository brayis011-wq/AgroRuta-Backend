package com.agroruta.user.domain;

/** Convierte el User-Agent del navegador en algo que una persona reconoce: "Chrome · Windows". */
public final class Dispositivo {

    static final int MAX = 120;

    private Dispositivo() {}

    /** null si no hay User-Agent */
    public static String describir(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        String ua = userAgent;
        String navegador = navegador(ua);
        String sistema = sistema(ua);
        String texto = navegador == null && sistema == null ? "Otro dispositivo"
                : navegador == null ? sistema
                : sistema == null ? navegador
                : navegador + " · " + sistema;
        return texto.length() > MAX ? texto.substring(0, MAX) : texto;
    }

    // El orden importa: Edge y Opera también dicen "Chrome", y Chrome también dice "Safari"
    private static String navegador(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) return "Edge";
        if (ua.contains("OPR/") || ua.contains("Opera")) return "Opera";
        if (ua.contains("SamsungBrowser/")) return "Samsung Internet";
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return null;
    }

    private static String sistema(String ua) {
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "Mac";
        if (ua.contains("CrOS")) return "ChromeOS";
        if (ua.contains("Linux")) return "Linux";
        return null;
    }
}
