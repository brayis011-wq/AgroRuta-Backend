package com.agroruta.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Totp")
class TotpTest {

    /** El secreto de los ejemplos del RFC 6238: "12345678901234567890" */
    private static final String SECRETO = Totp.base32("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    @DisplayName("da los códigos de los ejemplos del RFC 6238 (SHA-1, últimos 6 dígitos)")
    void vectoresDelEstandar() {
        assertEquals("287082", Totp.codigo(SECRETO, Totp.paso(59)));
        assertEquals("081804", Totp.codigo(SECRETO, Totp.paso(1111111109L)));
        assertEquals("005924", Totp.codigo(SECRETO, Totp.paso(1234567890L)));
        assertEquals("279037", Totp.codigo(SECRETO, Totp.paso(2000000000L)));
    }

    @Test
    @DisplayName("acepta el código actual, el anterior y el siguiente, pero no uno más viejo")
    void ventana() {
        long ahora = 1_800_000_000L;
        long paso = Totp.paso(ahora);
        assertEquals(paso, Totp.verificar(SECRETO, Totp.codigo(SECRETO, paso), ahora, null).orElseThrow());
        assertEquals(paso - 1, Totp.verificar(SECRETO, Totp.codigo(SECRETO, paso - 1), ahora, null).orElseThrow());
        assertEquals(paso + 1, Totp.verificar(SECRETO, Totp.codigo(SECRETO, paso + 1), ahora, null).orElseThrow());
        assertTrue(Totp.verificar(SECRETO, Totp.codigo(SECRETO, paso - 3), ahora, null).isEmpty());
    }

    @Test
    @DisplayName("un código ya usado no sirve otra vez")
    void sinRepetir() {
        long ahora = 1_800_000_000L;
        long paso = Totp.paso(ahora);
        String codigo = Totp.codigo(SECRETO, paso);
        assertTrue(Totp.verificar(SECRETO, codigo, ahora, paso).isEmpty());
        assertTrue(Totp.verificar(SECRETO, codigo, ahora, paso - 1).isPresent());
    }

    @Test
    @DisplayName("rechaza lo que no son seis dígitos")
    void formato() {
        assertTrue(Totp.verificar(SECRETO, "12345", 59, null).isEmpty());
        assertTrue(Totp.verificar(SECRETO, "abcdef", 59, null).isEmpty());
        assertTrue(Totp.verificar(SECRETO, null, 59, null).isEmpty());
    }

    @Test
    @DisplayName("el secreto va y vuelve por Base32, y el enlace para la app lleva emisor y cuenta")
    void base32YUri() {
        byte[] datos = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Totp.base32(datos));
        assertArrayEquals(datos, Totp.deBase32("gezdgnbvgy3tqojq gezdgnbvgy3tqojq"));
        assertEquals(32, Totp.nuevoSecreto(new SecureRandom()).length());
        assertEquals("otpauth://totp/AgroRuta%3Aana%40x.com?secret=ABC&issuer=AgroRuta&algorithm=SHA1&digits=6&period=30",
                Totp.uri("AgroRuta", "ana@x.com", "ABC"));
    }
}
