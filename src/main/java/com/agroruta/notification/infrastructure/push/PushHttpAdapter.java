package com.agroruta.notification.infrastructure.push;

import com.agroruta.notification.application.ports.out.PushPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Entrega el aviso cifrado al servicio de avisos del navegador (protocolo Web Push, RFC 8030). */
@Component
public class PushHttpAdapter implements PushPort {

    private static final Logger log = LoggerFactory.getLogger(PushHttpAdapter.class);
    /** Si el teléfono está apagado, el servicio guarda el aviso hasta un día */
    private static final String UN_DIA = "86400";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            // Sin redirecciones: el destino ya se validó y no debe cambiar por el camino
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Override
    public Resultado enviar(String endpoint, String autorizacion, byte[] cuerpo) {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", autorizacion)
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", UN_DIA)
                    .header("Urgency", "normal")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                    .build();
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            int estado = respuesta.statusCode();
            if (estado >= 200 && estado < 300) return Resultado.ENTREGADO;
            if (estado == 404 || estado == 410) return Resultado.CADUCADA;
            log.warn("El servicio de avisos push respondió {}: {}", estado, recortar(respuesta.body()));
            return Resultado.FALLO;
        } catch (IOException e) {
            log.warn("No se pudo conectar con el servicio de avisos push: {}", e.getMessage());
            return Resultado.FALLO;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Resultado.FALLO;
        }
    }

    private static String recortar(String texto) {
        if (texto == null) return "";
        return texto.length() <= 200 ? texto : texto.substring(0, 200);
    }
}
