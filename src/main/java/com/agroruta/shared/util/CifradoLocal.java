package com.agroruta.shared.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra datos que se guardan en la base (AES-256-GCM) con una clave derivada del secreto del
 * servidor. El contexto separa los usos: lo cifrado para una cosa no se puede leer con la clave de otra.
 */
public final class CifradoLocal {

    private static final SecureRandom AZAR = new SecureRandom();

    private final SecretKeySpec clave;

    public CifradoLocal(String secreto, String contexto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            this.clave = new SecretKeySpec(mac.doFinal(contexto.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo preparar el cifrado", e);
        }
    }

    /** base64(iv de 12 bytes + texto cifrado con su sello) */
    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[12];
            AZAR.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(128, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] todo = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, iv.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar", e);
        }
    }

    /** @throws IllegalStateException si el dato se alteró o se cifró con otro secreto */
    public String descifrar(String guardado) {
        try {
            byte[] todo = Base64.getDecoder().decode(guardado);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(128, todo, 0, 12));
            return new String(c.doFinal(todo, 12, todo.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar", e);
        }
    }
}
