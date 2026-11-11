package com.agroruta.notification.application;

import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.notification.application.ports.out.ClavesPushRepository;
import com.agroruta.notification.application.ports.out.PushPort;
import com.agroruta.notification.application.ports.out.SuscripcionPushRepository;
import com.agroruta.notification.domain.AvisoPush;
import com.agroruta.notification.domain.SuscripcionPush;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.weather.domain.ConsejoClima;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvisosPushServiceTest {

    // Claves de navegador del ejemplo de RFC 8291 (válidas)
    private static final String P256DH = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String AUTH = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String ENDPOINT = "https://fcm.googleapis.com/fcm/send/abc123";
    private static final AvisoPush AVISO = new AvisoPush("Tu finca hoy", "No fumigues hoy", "/dashboard", "resumen");

    private static class Suscripciones implements SuscripcionPushRepository {
        final List<SuscripcionPush> filas = new ArrayList<>();
        long siguiente = 1;

        @Override
        public void guardar(Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo) {
            filas.removeIf(s -> s.endpoint().equals(endpoint));
            filas.add(new SuscripcionPush(siguiente++, usuarioId, endpoint, p256dh, auth, dispositivo));
        }

        @Override
        public List<SuscripcionPush> deUsuario(Long usuarioId) {
            return filas.stream().filter(s -> s.usuarioId().equals(usuarioId))
                    .sorted(Comparator.comparing(SuscripcionPush::id).reversed()).toList();
        }

        @Override
        public boolean existe(Long usuarioId) {
            return !deUsuario(usuarioId).isEmpty();
        }

        @Override
        public void eliminar(Long id) {
            filas.removeIf(s -> s.id().equals(id));
        }

        @Override
        public void eliminarPorEndpoint(Long usuarioId, String endpoint) {
            filas.removeIf(s -> s.usuarioId().equals(usuarioId) && s.endpoint().equals(endpoint));
        }

        @Override
        public void eliminarTodas() {
            filas.clear();
        }
    }

    private static class Claves implements ClavesPushRepository {
        Guardadas guardadas;
        int vecesGuardado;

        @Override
        public Optional<Guardadas> buscar() {
            return Optional.ofNullable(guardadas);
        }

        @Override
        public void guardar(String publica, String privadaCifrada) {
            guardadas = new Guardadas(publica, privadaCifrada);
            vecesGuardado++;
        }
    }

    private static class Envios implements PushPort {
        record Envio(String endpoint, String autorizacion, byte[] cuerpo) {}

        final List<Envio> hechos = new ArrayList<>();
        Resultado resultado = Resultado.ENTREGADO;

        @Override
        public Resultado enviar(String endpoint, String autorizacion, byte[] cuerpo) {
            hechos.add(new Envio(endpoint, autorizacion, cuerpo));
            return resultado;
        }
    }

    private final Suscripciones suscripciones = new Suscripciones();
    private final Claves claves = new Claves();
    private final Envios envios = new Envios();

    private AvisosPushService servicio(String secreto) {
        return new AvisosPushService(suscripciones, claves, envios, secreto, "mailto:soporte@agroruta.test");
    }

    private final AvisosPushService service = servicio("secreto-de-prueba-suficientemente-largo");

    @Test
    void laClavePublicaSeGeneraUnaVezYSeGuardaCifrada() {
        String primera = service.clavePublica();
        String segunda = service.clavePublica();

        assertThat(primera).isEqualTo(segunda).hasSize(87);
        assertThat(claves.vecesGuardado).isEqualTo(1);
        assertThat(claves.guardadas.publica()).isEqualTo(primera);
        // La privada no queda en claro: son 32 bytes (43 caracteres en base64url) y lo guardado es otra cosa
        assertThat(claves.guardadas.privadaCifrada()).hasSizeGreaterThan(60);
    }

    @Test
    void otroArranqueDelServidorReutilizaLasMismasClaves() {
        String antes = service.clavePublica();

        assertThat(servicio("secreto-de-prueba-suficientemente-largo").clavePublica()).isEqualTo(antes);
        assertThat(claves.vecesGuardado).isEqualTo(1);
    }

    @Test
    void siCambiaElSecretoDelServidor_generaClavesNuevasYBorraLasSuscripciones() {
        String antes = service.clavePublica();
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, "Chrome · Android");

        String despues = servicio("otro-secreto-distinto-del-anterior").clavePublica();

        assertThat(despues).isNotEqualTo(antes);
        assertThat(suscripciones.filas).isEmpty();
    }

    @Test
    void suscribirGuardaElDispositivo() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, "Chrome · Android");

        assertThat(service.tieneSuscripcion(7L)).isTrue();
        assertThat(service.tieneSuscripcion(8L)).isFalse();
        assertThat(suscripciones.filas).singleElement()
                .satisfies(s -> assertThat(s.dispositivo()).isEqualTo("Chrome · Android"));
    }

    @Test
    void elMismoNavegadorConOtraCuentaPasaASerDeEsaCuenta() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, null);
        service.suscribir(8L, ENDPOINT, P256DH, AUTH, null);

        assertThat(service.tieneSuscripcion(7L)).isFalse();
        assertThat(service.tieneSuscripcion(8L)).isTrue();
    }

    @Test
    void soloAceptaServiciosDeAvisosConocidos() {
        for (String malo : List.of(
                "https://example.com/push/abc",
                "http://fcm.googleapis.com/fcm/send/abc",
                "https://fcm.googleapis.com.evil.test/abc",
                "https://evilfcm.googleapis.com.test/abc",
                "https://localhost:8080/admin",
                "https://169.254.169.254/latest/meta-data",
                "https://user@fcm.googleapis.com/abc",
                "https://fcm.googleapis.com:8443/abc",
                "no es una url")) {
            assertThatThrownBy(() -> service.suscribir(7L, malo, P256DH, AUTH, null))
                    .as(malo).isInstanceOf(BusinessException.class);
        }
        assertThat(AvisosPushService.endpointPermitido("https://updates.push.services.mozilla.com/wpush/v2/abc")).isTrue();
        assertThat(AvisosPushService.endpointPermitido("https://web.push.apple.com/abc")).isTrue();
        assertThat(AvisosPushService.endpointPermitido("https://wns2-by3p.notify.windows.com/w/?token=abc")).isTrue();
        assertThat(AvisosPushService.endpointPermitido(null)).isFalse();
        assertThat(suscripciones.filas).isEmpty();
    }

    @Test
    void rechazaClavesDeNavegadorMalFormadas() {
        assertThatThrownBy(() -> service.suscribir(7L, ENDPOINT, "AAAA", AUTH, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.suscribir(7L, ENDPOINT, P256DH, "AAAA", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void guardaComoMaximoDiezDispositivosPorPersona() {
        for (int i = 0; i < 12; i++) {
            service.suscribir(7L, ENDPOINT + i, P256DH, AUTH, "d" + i);
        }
        assertThat(suscripciones.deUsuario(7L)).hasSize(10)
                .extracting(SuscripcionPush::dispositivo).contains("d11").doesNotContain("d0", "d1");
    }

    @Test
    void enviarCifraYFirmaParaCadaDispositivo() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, null);
        service.suscribir(7L, "https://updates.push.services.mozilla.com/wpush/v2/xyz", P256DH, AUTH, null);
        service.suscribir(8L, ENDPOINT + "-otro", P256DH, AUTH, null);

        int entregados = service.enviar(7L, AVISO);

        assertThat(entregados).isEqualTo(2);
        assertThat(envios.hechos).hasSize(2);
        for (Envios.Envio e : envios.hechos) {
            assertThat(e.autorizacion()).startsWith("vapid t=").endsWith(", k=" + service.clavePublica());
            // 86 bytes de cabecera + el aviso cifrado; el texto no viaja en claro
            assertThat(e.cuerpo().length).isGreaterThan(86 + AVISO.json().length);
            assertThat(new String(e.cuerpo(), StandardCharsets.ISO_8859_1)).doesNotContain("No fumigues");
        }
        // La firma va dirigida al servicio de cada navegador
        assertThat(audiencia(envios.hechos.get(0).autorizacion())).contains("https://updates.push.services.mozilla.com");
        assertThat(audiencia(envios.hechos.get(1).autorizacion())).contains("https://fcm.googleapis.com");
    }

    @Test
    void unaSuscripcionCaducadaSeBorra() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, null);
        envios.resultado = PushPort.Resultado.CADUCADA;

        assertThat(service.enviar(7L, AVISO)).isZero();
        assertThat(service.tieneSuscripcion(7L)).isFalse();
    }

    @Test
    void unFalloPasajeroNoBorraLaSuscripcion() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, null);
        envios.resultado = PushPort.Resultado.FALLO;

        assertThat(service.enviar(7L, AVISO)).isZero();
        assertThat(service.tieneSuscripcion(7L)).isTrue();
    }

    @Test
    void sinDispositivosNoHaceNada() {
        assertThat(service.enviar(7L, AVISO)).isZero();
        assertThat(envios.hechos).isEmpty();
        assertThat(claves.vecesGuardado).isZero();
    }

    @Test
    void desuscribirSoloQuitaElDispositivoDeEsaPersona() {
        service.suscribir(7L, ENDPOINT, P256DH, AUTH, null);
        service.desuscribir(8L, ENDPOINT);
        assertThat(service.tieneSuscripcion(7L)).isTrue();
        service.desuscribir(7L, ENDPOINT);
        assertThat(service.tieneSuscripcion(7L)).isFalse();
    }

    @Test
    void elAvisoViajaComoJsonEscapadoYRecortado() {
        String json = new String(new AvisoPush("Pago \"listo\"", "Línea 1\nLínea 2 \\ fin", null, null).json(), StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("{\"title\":\"Pago \\\"listo\\\"\",\"body\":\"Línea 1\\nLínea 2 \\\\ fin\",\"url\":\"/\",\"tag\":\"agroruta\"}");

        String largo = new String(new AvisoPush("t", "x".repeat(1000), "/", "e").json(), StandardCharsets.UTF_8);
        assertThat(largo.length()).isLessThan(400);
        assertThat(largo).contains("…");
    }

    @Test
    void elResumenDelDiaPoneElClimaPrimero() {
        LocalDate hoy = LocalDate.of(2026, 11, 10);
        ConsejoClima noFumigar = new ConsejoClima(ConsejoClima.Tipo.NO_FUMIGAR, ConsejoClima.Nivel.ALERTA, 1L, "La Esperanza",
                "Lote 3", "Papa", hoy, "LLUVIA", LocalDateTime.of(2026, 11, 10, 15, 0), null, null, null);
        AlertaCultivo alerta = new AlertaCultivo(AlertaCultivo.Tipo.FUMIGACION_HOY, "La Esperanza", "Lote 3", "Papa",
                "Mancozeb", null, hoy);

        assertThat(AlertasDiariasService.resumenCorto(List.of(alerta, alerta), List.of(), List.of(), List.of(noFumigar), hoy))
                .isEqualTo("Lote 3: no fumigues hoy: lluvia desde las 3 p. m. · 2 avisos del cultivo");
        assertThat(AlertasDiariasService.resumenCorto(List.of(alerta), List.of(), List.of(), List.of(), hoy))
                .isEqualTo("1 aviso del cultivo");
    }

    private static String audiencia(String autorizacion) {
        String datos = autorizacion.substring("vapid t=".length()).split("\\.")[1];
        return new String(java.util.Base64.getUrlDecoder().decode(datos), StandardCharsets.UTF_8);
    }
}
