package com.agroruta.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dispositivo")
class DispositivoTest {

    @Test
    @DisplayName("reconoce los navegadores comunes aunque todos digan Chrome o Safari")
    void navegadores() {
        assertEquals("Chrome · Windows", Dispositivo.describir(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"));
        assertEquals("Edge · Windows", Dispositivo.describir(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0"));
        assertEquals("Safari · iPhone", Dispositivo.describir(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"));
        assertEquals("Chrome · Android", Dispositivo.describir(
                "Mozilla/5.0 (Linux; Android 14; SM-A155M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"));
        assertEquals("Firefox · Linux", Dispositivo.describir("Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0"));
        assertEquals("Safari · Mac", Dispositivo.describir(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15"));
    }

    @Test
    @DisplayName("sin User-Agent no inventa nada")
    void desconocido() {
        assertNull(Dispositivo.describir(null));
        assertNull(Dispositivo.describir("  "));
        assertEquals("Otro dispositivo", Dispositivo.describir("curl/8.5.0"));
    }
}
