package com.agroruta.crop.infrastructure.adapter;

import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.ConfianzaDiagnostico;
import com.agroruta.crop.domain.ContextoCultivo;
import com.agroruta.crop.domain.TipoProblema;
import com.agroruta.shared.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Habla con un servidor de mentira en esta misma máquina: no llama a Google. */
class GeminiDiagnosticoAdapterTest {

    private static final ContextoCultivo CAFE = new ContextoCultivo("Café", "Castillo", "crecimiento", 40L, "polvo naranja");
    private static final byte[] FOTO = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> cabeceras = new AtomicReference<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{}";
    /** Respuesta distinta para un modelo concreto; los demás usan "estado" y "respuesta" */
    private final Map<String, Respuesta> porModelo = new ConcurrentHashMap<>();
    private final List<String> modelosPedidos = Collections.synchronizedList(new ArrayList<>());

    private record Respuesta(int estado, String cuerpo, long demoraMs) {}

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1beta/interactions", intercambio -> {
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            cabeceras.set(Map.of(
                    "clave", String.valueOf(intercambio.getRequestHeaders().getFirst("x-goog-api-key")),
                    "revision", String.valueOf(intercambio.getRequestHeaders().getFirst("Api-Revision")),
                    "tipo", String.valueOf(intercambio.getRequestHeaders().getFirst("Content-Type"))));
            String modelo = JSON.readTree(cuerpoRecibido.get()).path("model").asText();
            modelosPedidos.add(modelo);
            Respuesta r = porModelo.getOrDefault(modelo, new Respuesta(estado, respuesta, 0));
            if (r.demoraMs() > 0) {
                try {
                    Thread.sleep(r.demoraMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = r.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(r.estado(), bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        // Varias peticiones a la vez: un modelo "colgado" no debe frenar la respuesta del siguiente
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.start();
    }

    @AfterEach
    void apagar() {
        servidor.stop(0);
    }

    private GeminiDiagnosticoAdapter adaptador() {
        return adaptador("", Duration.ofSeconds(10), Duration.ofSeconds(5));
    }

    private GeminiDiagnosticoAdapter adaptador(String respaldo, Duration total, Duration porIntento) {
        return new GeminiDiagnosticoAdapter("clave-de-prueba", "gemini-3.8-flash", respaldo,
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/v1beta/interactions", total, porIntento);
    }

    private static final String SATURADO = "{\"error\":{\"message\":\"gemini-3.8-flash is currently experiencing high demand\",\"code\":\"service_unavailable\"}}";
    private static final String SANA = """
            {"esPlanta": true, "problema": "Planta sana", "tipo": "SANA", "confianza": "ALTA", "sintomas": "Hojas verdes.",
             "recomendaciones": ["Sigue vigilando."], "otrasPosibilidades": [], "advertencia": ""}""";

    /** Una interacción terminada, con la forma que documenta la API */
    private static String interaccion(String estado, String texto) throws IOException {
        return JSON.writeValueAsString(Map.of(
                "id", "int_1", "status", estado, "model", "gemini-3.8-flash",
                "steps", List.of(
                        Map.of("type", "thought", "signature", "abc"),
                        Map.of("type", "model_output", "content", List.of(Map.of("type", "text", "text", texto)))),
                "usage", Map.of("total_input_tokens", 1200, "total_output_tokens", 300)));
    }

    @Test
    void sinClaveQuedaApagado() {
        GeminiDiagnosticoAdapter apagado = new GeminiDiagnosticoAdapter(" ", "gemini-3.8-flash", "gemini-3.6-flash");
        assertFalse(apagado.disponible());
        assertThrows(ExternalApiException.class, () -> apagado.analizar(FOTO, CAFE));
    }

    @Test
    void mandaLaFotoElContextoYLaFormaDeLaRespuesta() throws IOException {
        respuesta = interaccion("completed", """
                {"esPlanta": true, "problema": "Roya del café (Hemileia vastatrix)", "tipo": "ENFERMEDAD", "confianza": "MEDIA",
                 "sintomas": "Polvillo anaranjado en el envés.", "recomendaciones": ["Revisa el lote.", "Retira hojas afectadas."],
                 "otrasPosibilidades": ["Mancha de hierro"], "advertencia": ""}""");

        Optional<AnalisisFoto> analisis = adaptador().analizar(FOTO, CAFE);

        // Lo que se envió
        assertEquals("clave-de-prueba", cabeceras.get().get("clave"));
        assertEquals(GeminiDiagnosticoAdapter.REVISION, cabeceras.get().get("revision"));
        assertTrue(cabeceras.get().get("tipo").startsWith("application/json"));
        JsonNode enviado = JSON.readTree(cuerpoRecibido.get());
        assertEquals("gemini-3.8-flash", enviado.path("model").asText());
        assertTrue(enviado.path("system_instruction").asText().contains("asistente agronómico"));
        assertFalse(enviado.path("store").asBoolean(true), "la foto no debe quedar guardada en Google");
        JsonNode imagen = enviado.path("input").get(0);
        assertEquals("image", imagen.path("type").asText());
        assertEquals("image/jpeg", imagen.path("mime_type").asText());
        assertEquals(Base64.getEncoder().encodeToString(FOTO), imagen.path("data").asText());
        JsonNode texto = enviado.path("input").get(1);
        assertEquals("text", texto.path("type").asText());
        assertTrue(texto.path("text").asText().contains("Cultivo: Café"));
        assertTrue(texto.path("text").asText().contains("«polvo naranja»"));
        JsonNode formato = enviado.path("response_format");
        assertEquals("text", formato.path("type").asText());
        assertEquals("application/json", formato.path("mime_type").asText());
        assertEquals("object", formato.path("schema").path("type").asText());
        assertEquals(8, formato.path("schema").path("required").size());
        assertEquals("DANO_AMBIENTAL", formato.path("schema").path("properties").path("tipo").path("enum").get(3).asText());
        assertFalse(cuerpoRecibido.get().contains("clave-de-prueba"), "la clave va en la cabecera, no en el cuerpo");

        // Lo que se leyó
        AnalisisFoto a = analisis.orElseThrow();
        assertTrue(a.esPlanta());
        assertEquals("Roya del café (Hemileia vastatrix)", a.problema());
        assertEquals(TipoProblema.ENFERMEDAD, a.tipo());
        assertEquals(ConfianzaDiagnostico.MEDIA, a.confianza());
        assertEquals(List.of("Revisa el lote.", "Retira hojas afectadas."), a.recomendaciones());
        assertEquals(List.of("Mancha de hierro"), a.otrasPosibilidades());
        assertEquals("gemini-3.8-flash", a.modelo());
    }

    @Test
    void unValorQueNoEstaEnLaListaQuedaSinDefinir() throws IOException {
        respuesta = interaccion("completed", """
                {"esPlanta": true, "problema": "Algo raro", "tipo": "HONGO", "confianza": "media", "sintomas": "x",
                 "recomendaciones": [], "otrasPosibilidades": [], "advertencia": ""}""");
        AnalisisFoto a = adaptador().analizar(FOTO, CAFE).orElseThrow();
        assertEquals(null, a.tipo());
        assertEquals(ConfianzaDiagnostico.MEDIA, a.confianza());
    }

    @Test
    void siElModeloNoRespondeDevuelveVacio() throws IOException {
        respuesta = JSON.writeValueAsString(Map.of("id", "int_2", "status", "failed", "steps", List.of(),
                "errors", List.of(Map.of("code", "blocked", "message", "contenido descartado"))));
        assertTrue(adaptador().analizar(FOTO, CAFE).isEmpty());
    }

    @Test
    void unaRespuestaCortadaEsUnFalloDelServicio() throws IOException {
        respuesta = interaccion("incomplete", "{\"esPlanta\": true, \"problema\": \"Roya del ca");
        assertThrows(ExternalApiException.class, () -> adaptador().analizar(FOTO, CAFE));

        respuesta = interaccion("completed", "[1, 2, 3]");
        assertThrows(ExternalApiException.class, () -> adaptador().analizar(FOTO, CAFE));
    }

    @Test
    void losErroresDeGoogleNoMuestranDetallesInternos() {
        // Así responde Google a una clave inválida
        estado = 400;
        respuesta = "[{\"error\": {\"code\": 400, \"message\": \"API key not valid. Please pass a valid API key.\", \"status\": \"INVALID_ARGUMENT\","
                + " \"details\": [{\"reason\": \"API_KEY_INVALID\"}]}}]";
        ExternalApiException clave = assertThrows(ExternalApiException.class, () -> adaptador().analizar(FOTO, CAFE));
        assertEquals("El diagnóstico con foto no está disponible en este momento.", clave.getMessage());

        estado = 429;
        respuesta = "{\"error\": {\"code\": 429, \"status\": \"RESOURCE_EXHAUSTED\"}}";
        ExternalApiException cupo = assertThrows(ExternalApiException.class, () -> adaptador().analizar(FOTO, CAFE));
        assertTrue(cupo.getMessage().contains("cupo"));

        estado = 503;
        respuesta = "sobrecargado";
        ExternalApiException caido = assertThrows(ExternalApiException.class, () -> adaptador().analizar(FOTO, CAFE));
        assertFalse(caido.getMessage().contains("sobrecargado"));
    }

    @Test
    void siElModeloPrincipalEstaSaturadoUsaElDeRespaldo() throws IOException {
        porModelo.put("gemini-3.8-flash", new Respuesta(503, SATURADO, 0));
        porModelo.put("gemini-3.6-flash", new Respuesta(503, SATURADO, 0));
        respuesta = JSON.writeValueAsString(Map.of("status", "completed", "model", "gemini-3.5-flash-lite",
                "steps", List.of(Map.of("type", "model_output", "content", List.of(Map.of("type", "text", "text", SANA))))));

        AnalisisFoto a = adaptador("gemini-3.6-flash, gemini-3.5-flash-lite", Duration.ofSeconds(10), Duration.ofSeconds(5))
                .analizar(FOTO, CAFE).orElseThrow();

        assertEquals(List.of("gemini-3.8-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite"), modelosPedidos);
        assertEquals(TipoProblema.SANA, a.tipo());
        assertEquals("gemini-3.5-flash-lite", a.modelo(), "queda anotado qué modelo respondió");
    }

    @Test
    void siElModeloPrincipalSeQuedaColgadoUsaElDeRespaldo() throws IOException {
        porModelo.put("gemini-3.8-flash", new Respuesta(200, "{}", 3000));
        respuesta = interaccion("completed", SANA);

        long inicio = System.nanoTime();
        AnalisisFoto a = adaptador("gemini-3.5-flash-lite", Duration.ofSeconds(10), Duration.ofMillis(400))
                .analizar(FOTO, CAFE).orElseThrow();

        assertEquals(TipoProblema.SANA, a.tipo());
        assertEquals(List.of("gemini-3.8-flash", "gemini-3.5-flash-lite"), modelosPedidos);
        assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toMillis() < 2500, "no espera al modelo colgado");
    }

    @Test
    void siTodosLosModelosFallanAvisaQueEstaOcupado() {
        estado = 503;
        respuesta = SATURADO;
        ExternalApiException e = assertThrows(ExternalApiException.class,
                () -> adaptador("gemini-3.6-flash", Duration.ofSeconds(10), Duration.ofSeconds(5)).analizar(FOTO, CAFE));
        assertTrue(e.getMessage().contains("muy ocupado"));
        assertEquals(2, modelosPedidos.size());
    }

    @Test
    void noSePasaDelTiempoTotal() {
        porModelo.put("gemini-3.8-flash", new Respuesta(200, "{}", 3000));
        porModelo.put("gemini-3.6-flash", new Respuesta(200, "{}", 3000));
        long inicio = System.nanoTime();
        assertThrows(ExternalApiException.class,
                () -> adaptador("gemini-3.6-flash", Duration.ofMillis(1500), Duration.ofMillis(500)).analizar(FOTO, CAFE));
        assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toMillis() < 2500);
    }

    @Test
    void conLaClaveRechazadaNoPruebaOtrosModelos() {
        estado = 400;
        respuesta = "[{\"error\": {\"code\": 400, \"status\": \"INVALID_ARGUMENT\", \"details\": [{\"reason\": \"API_KEY_INVALID\"}]}}]";
        assertThrows(ExternalApiException.class,
                () -> adaptador("gemini-3.6-flash,gemini-3.5-flash-lite", Duration.ofSeconds(10), Duration.ofSeconds(5)).analizar(FOTO, CAFE));
        assertEquals(1, modelosPedidos.size());
    }

    @Test
    void siNoHayConexionEsUnFalloDelServicio() {
        GeminiDiagnosticoAdapter sinServidor = adaptador();
        servidor.stop(0);
        assertThrows(ExternalApiException.class, () -> sinServidor.analizar(FOTO, CAFE));
    }

    @Test
    void describeLaSiembraAunqueFaltenDatos() {
        String vacio = GeminiDiagnosticoAdapter.describir(new ContextoCultivo(null, " ", null, null, null));
        assertTrue(vacio.contains("Cultivo: sin dato"));
        assertTrue(vacio.contains("(no escribió nota)"));
    }
}
