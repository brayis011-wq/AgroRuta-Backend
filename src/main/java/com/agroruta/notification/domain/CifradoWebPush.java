package com.agroruta.notification.domain;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifrado del contenido de un aviso push (RFC 8291, codificación aes128gcm de RFC 8188).
 * <p>
 * Solo el navegador que se suscribió puede leer el aviso: el servicio que lo entrega
 * (Google, Mozilla, Apple) ve bytes cifrados. Se usa un par de claves nuevo en cada envío.
 */
public final class CifradoWebPush {

    private static final SecureRandom AZAR = new SecureRandom();
    /** Tamaño de registro que se declara en la cabecera; el aviso completo cabe en uno */
    private static final int TAMANO_REGISTRO = 4096;
    /** Lo máximo que cabe en un registro: 4096 - 16 del sello de autenticación - 1 del delimitador */
    public static final int MAX_BYTES = TAMANO_REGISTRO - 17;

    private CifradoWebPush() {}

    /**
     * @param mensaje texto del aviso (hasta {@link #MAX_BYTES} bytes)
     * @param p256dh  clave pública del navegador (base64url)
     * @param auth    secreto de autenticación del navegador (base64url)
     * @return el cuerpo de la petición, listo para enviarse con Content-Encoding: aes128gcm
     */
    public static byte[] cifrar(byte[] mensaje, String p256dh, String auth) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        return cifrar(mensaje, p256dh, auth, sal, ClavesVapid.parNuevo());
    }

    /** Con la sal y el par efímero dados: solo para comprobar el resultado contra el ejemplo del RFC. */
    static byte[] cifrar(byte[] mensaje, String p256dh, String auth, byte[] sal, KeyPair efimero) {
        if (mensaje.length > MAX_BYTES) throw new IllegalArgumentException("El aviso es demasiado largo");
        try {
            Base64.Decoder b64 = Base64.getUrlDecoder();
            byte[] publicaNavegador = b64.decode(p256dh);
            byte[] secretoAuth = b64.decode(auth);
            byte[] publicaServidor = ClavesVapid.puntoSinComprimir((ECPublicKey) efimero.getPublic());

            KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
            acuerdo.init(efimero.getPrivate());
            acuerdo.doPhase(ClavesVapid.clavePublica(publicaNavegador), true);
            byte[] compartido = acuerdo.generateSecret();

            // Clave de entrada: mezcla el secreto compartido con el secreto de autenticación y las dos claves públicas
            byte[] info = unir("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), publicaNavegador, publicaServidor);
            byte[] ikm = hkdf(secretoAuth, compartido, info, 32);

            byte[] clave = hkdf(sal, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
            byte[] nonce = hkdf(sal, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

            Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
            // 0x02 marca el final del último (y único) registro
            byte[] cifrado = aes.doFinal(unir(mensaje, new byte[]{2}));

            // Cabecera: sal (16) | tamaño de registro (4) | largo de la clave (1) | clave pública efímera (65)
            ByteArrayOutputStream cuerpo = new ByteArrayOutputStream(86 + cifrado.length);
            cuerpo.write(sal);
            cuerpo.write(new byte[]{(byte) (TAMANO_REGISTRO >>> 24), (byte) (TAMANO_REGISTRO >>> 16),
                    (byte) (TAMANO_REGISTRO >>> 8), (byte) TAMANO_REGISTRO});
            cuerpo.write(publicaServidor.length);
            cuerpo.write(publicaServidor);
            cuerpo.write(cifrado);
            return cuerpo.toByteArray();
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("No se pudo cifrar el aviso push: " + e.getMessage(), e);
        }
    }

    /** Comprueba que las claves que mandó el navegador tienen la forma esperada (65 y 16 bytes). */
    public static boolean clavesValidas(String p256dh, String auth) {
        try {
            Base64.Decoder b64 = Base64.getUrlDecoder();
            ClavesVapid.clavePublica(b64.decode(p256dh));
            return b64.decode(auth).length == 16;
        } catch (GeneralSecurityException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    /** HKDF-SHA256 (RFC 5869) para salidas de hasta 32 bytes: un solo bloque */
    private static byte[] hkdf(byte[] sal, byte[] ikm, byte[] info, int largo) throws GeneralSecurityException {
        byte[] prk = hmac(sal, ikm);
        return Arrays.copyOf(hmac(prk, unir(info, new byte[]{1})), largo);
    }

    private static byte[] hmac(byte[] clave, byte[] datos) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(clave, "HmacSHA256"));
        return mac.doFinal(datos);
    }

    private static byte[] unir(byte[]... partes) {
        int total = 0;
        for (byte[] p : partes) total += p.length;
        byte[] salida = new byte[total];
        int pos = 0;
        for (byte[] p : partes) {
            System.arraycopy(p, 0, salida, pos, p.length);
            pos += p.length;
        }
        return salida;
    }
}
