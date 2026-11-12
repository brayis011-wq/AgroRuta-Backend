package com.agroruta.notification.domain;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;

/**
 * Par de claves P-256 con el que el servidor se identifica ante los servicios de avisos push
 * (VAPID, RFC 8292). También reúne las conversiones de claves que usa el cifrado de los avisos.
 *
 * @param publica punto sin comprimir (65 bytes) en base64url; es la que recibe el navegador
 * @param privada escalar de 32 bytes en base64url
 */
public record ClavesVapid(String publica, String privada) {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DE_B64 = Base64.getUrlDecoder();

    public static ClavesVapid generar() {
        KeyPair par = parNuevo();
        return new ClavesVapid(B64.encodeToString(puntoSinComprimir((ECPublicKey) par.getPublic())),
                B64.encodeToString(a32Bytes(((ECPrivateKey) par.getPrivate()).getS())));
    }

    /**
     * Cabecera Authorization para un envío: un JWT firmado (ES256) que dice a qué servicio va,
     * hasta cuándo vale y a quién escribir si hay problemas.
     *
     * @param audiencia origen del servicio de avisos, p. ej. https://fcm.googleapis.com
     * @param contacto  mailto: o https: del responsable
     */
    public String autorizacion(String audiencia, String contacto, long expiraEnSegundos) {
        String cabecera = B64.encodeToString("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String datos = B64.encodeToString(("{\"aud\":\"" + audiencia + "\",\"exp\":" + expiraEnSegundos
                + ",\"sub\":\"" + contacto + "\"}").getBytes(StandardCharsets.UTF_8));
        String porFirmar = cabecera + "." + datos;
        try {
            // P1363: la firma sale como R||S (64 bytes), que es el formato de JWT
            Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format");
            firma.initSign(clavePrivada(DE_B64.decode(privada)));
            firma.update(porFirmar.getBytes(StandardCharsets.US_ASCII));
            return "vapid t=" + porFirmar + "." + B64.encodeToString(firma.sign()) + ", k=" + publica;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el aviso push", e);
        }
    }

    // ───────────── Conversión de claves P-256 ─────────────

    static KeyPair parNuevo() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(new ECGenParameterSpec("secp256r1"));
            return g.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Este servidor no soporta claves P-256", e);
        }
    }

    private static ECParameterSpec curva() throws GeneralSecurityException {
        AlgorithmParameters p = AlgorithmParameters.getInstance("EC");
        p.init(new ECGenParameterSpec("secp256r1"));
        return p.getParameterSpec(ECParameterSpec.class);
    }

    /** 0x04 || X (32) || Y (32) */
    static byte[] puntoSinComprimir(ECPublicKey clave) {
        byte[] punto = new byte[65];
        punto[0] = 4;
        System.arraycopy(a32Bytes(clave.getW().getAffineX()), 0, punto, 1, 32);
        System.arraycopy(a32Bytes(clave.getW().getAffineY()), 0, punto, 33, 32);
        return punto;
    }

    static ECPublicKey clavePublica(byte[] punto) throws GeneralSecurityException {
        if (punto.length != 65 || punto[0] != 4) throw new GeneralSecurityException("Clave pública con formato inesperado");
        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(punto, 1, 33));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(punto, 33, 65));
        ECParameterSpec curva = curva();
        // KeyFactory no comprueba que el punto esté en la curva (y² = x³ + ax + b mod p): una clave
        // inventada debilitaría el acuerdo de claves
        BigInteger p = ((java.security.spec.ECFieldFp) curva.getCurve().getField()).getP();
        BigInteger derecha = x.pow(3).add(curva.getCurve().getA().multiply(x)).add(curva.getCurve().getB()).mod(p);
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0 || !y.multiply(y).mod(p).equals(derecha)) {
            throw new GeneralSecurityException("La clave pública no es un punto de la curva P-256");
        }
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curva));
    }

    static ECPrivateKey clavePrivada(byte[] escalar) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, escalar), curva()));
    }

    private static byte[] a32Bytes(BigInteger n) {
        byte[] crudo = n.toByteArray();
        byte[] salida = new byte[32];
        int copiar = Math.min(crudo.length, 32);
        System.arraycopy(crudo, crudo.length - copiar, salida, 32 - copiar, copiar);
        return salida;
    }
}
