package com.agroruta.user.domain;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.OptionalLong;

/**
 * Códigos de 6 dígitos que cambian cada 30 segundos (TOTP, RFC 6238 con HMAC-SHA1): los mismos que
 * dan Google Authenticator, Microsoft Authenticator, Authy y similares.
 * Sin base de datos ni reloj propio, para poder probarlo con los vectores del estándar.
 */
public final class Totp {

    public static final int PERIODO_SEGUNDOS = 30;
    public static final int DIGITOS = 6;
    /** Se acepta el código anterior y el siguiente: relojes un poco corridos o código tecleado tarde */
    static final int VENTANA = 1;

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Totp() {}

    /** Secreto nuevo de 160 bits, en Base32 (lo que se escribe a mano si no se puede escanear el QR) */
    public static String nuevoSecreto(SecureRandom azar) {
        byte[] b = new byte[20];
        azar.nextBytes(b);
        return base32(b);
    }

    /** El intervalo de 30 s al que pertenece ese instante */
    public static long paso(long epochSegundos) {
        return Math.floorDiv(epochSegundos, PERIODO_SEGUNDOS);
    }

    /** El código del intervalo dado */
    public static String codigo(String secretoBase32, long paso) {
        byte[] hash;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(deBase32(secretoBase32), "HmacSHA1"));
            hash = mac.doFinal(ByteBuffer.allocate(8).putLong(paso).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        int desde = hash[hash.length - 1] & 0x0f;
        int binario = ((hash[desde] & 0x7f) << 24) | ((hash[desde + 1] & 0xff) << 16)
                | ((hash[desde + 2] & 0xff) << 8) | (hash[desde + 3] & 0xff);
        return String.format("%0" + DIGITOS + "d", binario % 1_000_000);
    }

    /**
     * ¿El código es el de ahora (o el del intervalo de al lado)? Devuelve el intervalo que coincidió.
     * Un intervalo ya usado ({@code <= ultimoPaso}) no vuelve a servir: quien vea el código por encima
     * del hombro no puede reutilizarlo.
     */
    public static OptionalLong verificar(String secretoBase32, String codigo, long epochSegundos, Long ultimoPaso) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) return OptionalLong.empty();
        long actual = paso(epochSegundos);
        for (long p = actual - VENTANA; p <= actual + VENTANA; p++) {
            if (ultimoPaso != null && p <= ultimoPaso) continue;
            if (MessageDigest.isEqual(codigo(secretoBase32, p).getBytes(StandardCharsets.UTF_8),
                    codigo.getBytes(StandardCharsets.UTF_8))) {
                return OptionalLong.of(p);
            }
        }
        return OptionalLong.empty();
    }

    /** Lo que lleva el QR: la app de autenticación lo entiende y crea la cuenta sola */
    public static String uri(String emisor, String cuenta, String secretoBase32) {
        String etiqueta = URLEncoder.encode(emisor + ":" + cuenta, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + etiqueta + "?secret=" + secretoBase32
                + "&issuer=" + URLEncoder.encode(emisor, StandardCharsets.UTF_8).replace("+", "%20")
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PERIODO_SEGUNDOS;
    }

    static String base32(byte[] datos) {
        StringBuilder s = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                s.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) s.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return s.toString();
    }

    static byte[] deBase32(String texto) {
        String limpio = texto.replace("=", "").replace(" ", "").toUpperCase();
        byte[] salida = new byte[limpio.length() * 5 / 8];
        int buffer = 0, bits = 0, i = 0;
        for (char c : limpio.toCharArray()) {
            int v = BASE32.indexOf(c);
            if (v < 0) throw new IllegalArgumentException("Secreto Base32 no válido");
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                salida[i++] = (byte) ((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return salida;
    }
}
