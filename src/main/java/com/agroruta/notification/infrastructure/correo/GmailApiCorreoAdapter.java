package com.agroruta.notification.infrastructure.correo;

import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.notification.domain.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Envía con la API de Gmail por HTTPS (Render gratis bloquea los puertos SMTP).
 * Usa un refresh token de OAuth de la cuenta remitente: con él pide un access token
 * (dura una hora y se guarda en memoria) y manda el correo en formato MIME.
 */
public class GmailApiCorreoAdapter implements CorreoPort {

    private static final Logger log = LoggerFactory.getLogger(GmailApiCorreoAdapter.class);

    static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
    static final String URL_ENVIO = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String remitente;
    private final RestTemplate http;
    private final Clock reloj;

    private String accessToken;
    private Instant venceEn = Instant.EPOCH;

    public GmailApiCorreoAdapter(String clientId, String clientSecret, String refreshToken, String remitente) {
        this(clientId, clientSecret, refreshToken, remitente, conTiempos(), Clock.systemUTC());
    }

    GmailApiCorreoAdapter(String clientId, String clientSecret, String refreshToken, String remitente,
                          RestTemplate http, Clock reloj) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.remitente = remitente;
        this.http = http;
        this.reloj = reloj;
    }

    private static RestTemplate conTiempos() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(20_000);
        return new RestTemplate(f);
    }

    @Override
    public boolean enviar(Correo correo) {
        if (correo == null || correo.para() == null || correo.para().isBlank()) return false;
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token());
            h.setContentType(MediaType.APPLICATION_JSON);
            String raw = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mime(correo).getBytes(StandardCharsets.UTF_8));
            http.postForEntity(URL_ENVIO, new HttpEntity<>(Map.of("raw", raw), h), Map.class);
            log.info("Correo enviado: \"{}\"", correo.asunto());
            return true;
        } catch (RuntimeException e) {
            synchronized (this) { venceEn = Instant.EPOCH; } // por si el token dejó de servir
            log.warn("No se pudo enviar el correo \"{}\": {}", correo.asunto(), e.getMessage());
            return false;
        }
    }

    /** Access token vigente; lo renueva un minuto antes de que venza */
    @SuppressWarnings("unchecked")
    synchronized String token() {
        Instant ahora = reloj.instant();
        if (accessToken != null && ahora.isBefore(venceEn.minusSeconds(60))) return accessToken;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        Map<String, Object> r = http.postForObject(URL_TOKEN, new HttpEntity<>(form, h), Map.class);
        Object t = r == null ? null : r.get("access_token");
        if (!(t instanceof String s) || s.isBlank()) {
            throw new IllegalStateException("Google no devolvió access_token");
        }
        long segundos = r.get("expires_in") instanceof Number n ? n.longValue() : 3600;
        accessToken = s;
        venceEn = ahora.plusSeconds(segundos);
        return accessToken;
    }

    /** Mensaje MIME con la versión de texto y la HTML, en UTF-8 */
    String mime(Correo c) {
        String limite = "agroruta-" + UUID.randomUUID();
        Base64.Encoder b64 = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII));
        String de = remitente == null || remitente.isBlank() ? "" : "From: " + encabezado("AgroRuta") + " <" + limpio(remitente) + ">\r\n";
        return de
                + "To: " + limpio(c.para()) + "\r\n"
                + "Subject: " + encabezado(c.asunto()) + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: multipart/alternative; boundary=\"" + limite + "\"\r\n"
                + "\r\n"
                + "--" + limite + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + b64.encodeToString(nulo(c.texto()).getBytes(StandardCharsets.UTF_8)) + "\r\n"
                + "--" + limite + "\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: base64\r\n\r\n"
                + b64.encodeToString(nulo(c.html()).getBytes(StandardCharsets.UTF_8)) + "\r\n"
                + "--" + limite + "--\r\n";
    }

    /** Encabezado con tildes según RFC 2047 */
    static String encabezado(String s) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(limpio(s).getBytes(StandardCharsets.UTF_8)) + "?=";
    }

    /** Sin saltos de línea: nadie puede colar encabezados nuevos con un nombre o un correo */
    static String limpio(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }
}
