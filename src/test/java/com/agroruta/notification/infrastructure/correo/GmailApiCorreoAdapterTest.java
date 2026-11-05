package com.agroruta.notification.infrastructure.correo;

import com.agroruta.notification.domain.Correo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GmailApiCorreoAdapter")
class GmailApiCorreoAdapterTest {

    private final RestTemplate http = mock(RestTemplate.class);
    private final GmailApiCorreoAdapter adapter = new GmailApiCorreoAdapter("id", "secreto", "refresh",
            "agroruta.avisos@gmail.com", http, Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC));

    private final Correo correo = new Correo("ana@x.com", "Tu contraseña cambió", "<p>Hola</p>", "Hola");

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tokenOk() {
        when(http.postForObject(eq(GmailApiCorreoAdapter.URL_TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenReturn((Map) Map.of("access_token", "ya29.abc", "expires_in", 3599));
        when(http.postForEntity(eq(GmailApiCorreoAdapter.URL_ENVIO), any(HttpEntity.class), eq(Map.class)))
                .thenReturn((ResponseEntity) ResponseEntity.ok(Map.of("id", "1")));
    }

    @Test
    @DisplayName("pide el access token una vez y lo reutiliza mientras está vigente")
    void reutilizaToken() {
        tokenOk();
        assertThat(adapter.enviar(correo)).isTrue();
        assertThat(adapter.enviar(correo)).isTrue();
        verify(http, times(1)).postForObject(eq(GmailApiCorreoAdapter.URL_TOKEN), any(HttpEntity.class), eq(Map.class));
        verify(http, times(2)).postForEntity(eq(GmailApiCorreoAdapter.URL_ENVIO), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    @DisplayName("si Google falla devuelve false y no lanza")
    void fallaSinLanzar() {
        when(http.postForObject(eq(GmailApiCorreoAdapter.URL_TOKEN), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("invalid_grant"));
        assertThat(adapter.enviar(correo)).isFalse();
    }

    @Test
    @DisplayName("sin destinatario no intenta enviar")
    void sinDestinatario() {
        assertThat(adapter.enviar(new Correo(" ", "a", "b", "c"))).isFalse();
    }

    @Test
    @DisplayName("MIME en UTF-8 con asunto codificado, texto y HTML")
    void mime() {
        String m = adapter.mime(correo);
        assertThat(m).contains("To: ana@x.com\r\n", "MIME-Version: 1.0", "multipart/alternative",
                "Content-Type: text/plain; charset=UTF-8", "Content-Type: text/html; charset=UTF-8");
        assertThat(m).contains("Subject: " + GmailApiCorreoAdapter.encabezado("Tu contraseña cambió"));
        assertThat(m).contains("<agroruta.avisos@gmail.com>");
        assertThat(m).contains(Base64.getMimeEncoder().encodeToString("<p>Hola</p>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("un salto de línea en el destinatario o el asunto no agrega encabezados")
    void sinInyeccion() {
        String m = adapter.mime(new Correo("ana@x.com\r\nBcc: otro@x.com", "Hola\r\nBcc: x@x.com", "", ""));
        assertThat(m).doesNotContain("\r\nBcc:");
    }
}
