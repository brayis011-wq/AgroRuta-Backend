package com.agroruta.notification.domain;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CifradoWebPushTest {

    private static final Base64.Decoder B64 = Base64.getUrlDecoder();
    private static final Base64.Encoder A_B64 = Base64.getUrlEncoder().withoutPadding();

    // Ejemplo del apéndice A de RFC 8291
    private static final String TEXTO = "When I grow up, I want to be a watermelon";
    private static final String SERVIDOR_PRIVADA = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String SERVIDOR_PUBLICA = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String NAVEGADOR_PRIVADA = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
    private static final String NAVEGADOR_PUBLICA = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String AUTH = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String SAL = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String ESPERADO = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void coincideConElEjemploDelRfc8291() throws Exception {
        KeyPair efimero = new KeyPair(ClavesVapid.clavePublica(B64.decode(SERVIDOR_PUBLICA)),
                ClavesVapid.clavePrivada(B64.decode(SERVIDOR_PRIVADA)));

        byte[] cuerpo = CifradoWebPush.cifrar(TEXTO.getBytes(StandardCharsets.UTF_8), NAVEGADOR_PUBLICA, AUTH,
                B64.decode(SAL), efimero);

        assertThat(A_B64.encodeToString(cuerpo)).isEqualTo(ESPERADO);
    }

    @Test
    void elNavegadorPuedeDescifrarLoQueSeCifraConClavesNuevas() throws Exception {
        KeyPair navegador = ClavesVapid.parNuevo();
        String p256dh = A_B64.encodeToString(ClavesVapid.puntoSinComprimir((ECPublicKey) navegador.getPublic()));
        String mensaje = "{\"title\":\"AgroRuta\",\"body\":\"No fumigues hoy: lluvia desde las 3 p. m.\"}";

        byte[] cuerpo = CifradoWebPush.cifrar(mensaje.getBytes(StandardCharsets.UTF_8), p256dh, AUTH);
        byte[] otraVez = CifradoWebPush.cifrar(mensaje.getBytes(StandardCharsets.UTF_8), p256dh, AUTH);

        assertThat(descifrar(cuerpo, (ECPrivateKey) navegador.getPrivate(), B64.decode(p256dh), B64.decode(AUTH)))
                .isEqualTo(mensaje);
        // Sal y clave efímera nuevas en cada envío: dos avisos iguales no se parecen
        assertThat(cuerpo).isNotEqualTo(otraVez);
    }

    @Test
    void rechazaUnAvisoQueNoCabe() {
        assertThatThrownBy(() -> CifradoWebPush.cifrar(new byte[CifradoWebPush.MAX_BYTES + 1], NAVEGADOR_PUBLICA, AUTH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaLaFormaDeLasClavesDelNavegador() {
        assertThat(CifradoWebPush.clavesValidas(NAVEGADOR_PUBLICA, AUTH)).isTrue();
        assertThat(CifradoWebPush.clavesValidas("AAAA", AUTH)).isFalse();
        assertThat(CifradoWebPush.clavesValidas(NAVEGADOR_PUBLICA, "AAAA")).isFalse();
        assertThat(CifradoWebPush.clavesValidas("no es base64 !!", AUTH)).isFalse();
        assertThat(CifradoWebPush.clavesValidas(null, null)).isFalse();
        // 65 bytes que empiezan por 4 pero no son un punto de la curva
        byte[] falso = new byte[65];
        falso[0] = 4;
        falso[10] = 7;
        assertThat(CifradoWebPush.clavesValidas(A_B64.encodeToString(falso), AUTH)).isFalse();
    }

    @Test
    void lasClavesDelServidorFirmanUnaAutorizacionVerificable() throws Exception {
        ClavesVapid claves = ClavesVapid.generar();
        assertThat(B64.decode(claves.publica())).hasSize(65);
        assertThat(B64.decode(claves.privada())).hasSize(32);

        String cabecera = claves.autorizacion("https://fcm.googleapis.com", "mailto:soporte@agroruta.test", 1_800_000_000L);

        assertThat(cabecera).startsWith("vapid t=").endsWith(", k=" + claves.publica());
        String jwt = cabecera.substring("vapid t=".length(), cabecera.indexOf(", k="));
        String[] partes = jwt.split("\\.");
        assertThat(partes).hasSize(3);
        assertThat(new String(B64.decode(partes[1]), StandardCharsets.UTF_8))
                .isEqualTo("{\"aud\":\"https://fcm.googleapis.com\",\"exp\":1800000000,\"sub\":\"mailto:soporte@agroruta.test\"}");

        Signature firma = Signature.getInstance("SHA256withECDSAinP1363Format");
        firma.initVerify(ClavesVapid.clavePublica(B64.decode(claves.publica())));
        firma.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(firma.verify(B64.decode(partes[2]))).isTrue();
    }

    /** Lo que hace el navegador al recibir el aviso (RFC 8291, lado del receptor) */
    private static String descifrar(byte[] cuerpo, ECPrivateKey privada, byte[] publicaNavegador, byte[] auth) throws Exception {
        byte[] sal = Arrays.copyOfRange(cuerpo, 0, 16);
        int largoClave = cuerpo[20] & 0xff;
        byte[] publicaServidor = Arrays.copyOfRange(cuerpo, 21, 21 + largoClave);
        byte[] cifrado = Arrays.copyOfRange(cuerpo, 21 + largoClave, cuerpo.length);

        KeyAgreement acuerdo = KeyAgreement.getInstance("ECDH");
        acuerdo.init(privada);
        acuerdo.doPhase(ClavesVapid.clavePublica(publicaServidor), true);
        byte[] compartido = acuerdo.generateSecret();

        byte[] ikm = hkdf(auth, compartido, unir("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), publicaNavegador, publicaServidor), 32);
        byte[] clave = hkdf(sal, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(sal, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
        byte[] claro = aes.doFinal(cifrado);
        assertThat(claro[claro.length - 1]).isEqualTo((byte) 2);
        return new String(claro, 0, claro.length - 1, StandardCharsets.UTF_8);
    }

    private static byte[] hkdf(byte[] sal, byte[] ikm, byte[] info, int largo) throws Exception {
        return Arrays.copyOf(hmac(hmac(sal, ikm), unir(info, new byte[]{1})), largo);
    }

    private static byte[] hmac(byte[] clave, byte[] datos) throws Exception {
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
