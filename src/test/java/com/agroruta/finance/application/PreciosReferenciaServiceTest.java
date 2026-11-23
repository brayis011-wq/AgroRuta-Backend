package com.agroruta.finance.application;

import com.agroruta.finance.application.ports.out.PreciosReferenciaRepository;
import com.agroruta.finance.domain.EstadoPrecios;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import com.agroruta.shared.exception.ExternalApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** El estado de los precios de SIPSA: si hay, si se están trayendo y por qué falló la última vez. */
class PreciosReferenciaServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    /** Repositorio en memoria */
    static class Memoria implements PreciosReferenciaRepository {
        final List<PrecioReferencia> precios = new ArrayList<>();
        LocalDateTime cuando;
        @Override public void guardar(List<PrecioReferencia> p, LocalDateTime c) { precios.clear(); precios.addAll(p); cuando = c; }
        @Override public List<PrecioReferencia> ultimaSemana() { return List.copyOf(precios); }
        @Override public Optional<LocalDateTime> ultimaActualizacion() { return Optional.ofNullable(cuando); }
        @Override public Optional<PreferenciaPrecio> preferencia(Long a, String c) { return Optional.empty(); }
        @Override public List<PreferenciaPrecio> preferencias(Long a) { return List.of(); }
        @Override public void guardarPreferencia(Long a, PreferenciaPrecio p, LocalDateTime c) {}
        @Override public void borrarPreferencia(Long a, String c) {}
    }

    private static final PrecioReferencia PAPA = new PrecioReferencia("Papa criolla", LocalDate.of(2026, 11, 14),
            new BigDecimal("2500"), new BigDecimal("2000"), new BigDecimal("3000"), 12);

    @Test
    void siFallaGuardaElMotivoYLoBorraCuandoSaleBien() {
        Memoria repo = new Memoria();
        AtomicInteger llamadas = new AtomicInteger();
        boolean[] falla = {true};
        var servicio = new PreciosReferenciaService(repo, semanas -> {
            llamadas.incrementAndGet();
            if (falla[0]) throw new ExternalApiException("No se pudo consultar SIPSA: tiempo agotado");
            return List.of(PAPA);
        }, Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), BOGOTA), Runnable::run);

        // Sin precios: pedir el estado los trae (aquí, en el mismo hilo) y falla
        EstadoPrecios e = servicio.estado();
        assertThat(e.hayPrecios()).isFalse();
        assertThat(e.actualizando()).isFalse();
        assertThat(e.ultimoError()).contains("tiempo agotado");
        assertThat(llamadas.get()).isEqualTo(1);

        // Recién fallado no se vuelve a intentar enseguida
        servicio.estado();
        assertThat(llamadas.get()).isEqualTo(1);

        // Si se trae a mano y sale bien, ya hay precios y no queda el error
        falla[0] = false;
        assertThat(servicio.actualizar()).isEqualTo(1);
        EstadoPrecios bien = servicio.estado();
        assertThat(bien.hayPrecios()).isTrue();
        assertThat(bien.ultimaSemana()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(bien.ultimoError()).isNull();
        assertThat(bien.actualizadoEn()).isNotNull();
    }

    /** SIPSA que solo lee respuestas descargadas (el DANE directo siempre falla, como desde Render) */
    static class SoloArchivo implements com.agroruta.finance.application.ports.out.SipsaPort {
        @Override public List<PrecioReferencia> preciosRecientes(int semanas) {
            throw new ExternalApiException("No se pudo consultar SIPSA: Remote host terminated the handshake");
        }
        @Override public List<PrecioReferencia> desdeRespuesta(java.io.InputStream xml, int semanas) {
            return com.agroruta.finance.infrastructure.sipsa.SipsaSoapAdapter.leer(xml).ultimasSemanas(semanas);
        }
    }

    private static final String RESPUESTA = """
            <S:Envelope xmlns:S="http://www.w3.org/2003/05/soap-envelope"><S:Body>
            <ns2:promediosSipsaSemanaMadrResponse xmlns:ns2="http://servicios.sipsa.co.gov.dane/">
            <return><artiNombre>Papa criolla</artiNombre><fechaIni>2026-11-14T00:00:00-05:00</fechaIni>
            <promedioKg>2500</promedioKg><minimoKg>2000</minimoKg><maximoKg>3000</maximoKg></return>
            </ns2:promediosSipsaSemanaMadrResponse></S:Body></S:Envelope>""";

    @Test
    void unaRespuestaSubidaDesdeElPcSeGuardaYBorraElError() {
        Memoria repo = new Memoria();
        var servicio = new PreciosReferenciaService(repo, new SoloArchivo(),
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), BOGOTA), Runnable::run);
        assertThat(servicio.estado().ultimoError()).contains("handshake");

        EstadoPrecios e = servicio.cargarRespuesta(new java.io.ByteArrayInputStream(RESPUESTA.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(e.hayPrecios()).isTrue();
        assertThat(e.ultimaSemana()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(e.ultimoError()).isNull();
        assertThat(repo.precios).singleElement().satisfies(p -> assertThat(p.producto()).isEqualTo("Papa criolla"));
        assertThat(servicio.estado().ultimoError()).isNull();
    }

    @Test
    void unArchivoQueNoEsDeSipsaSeRechazaSinBorrarLoGuardado() {
        Memoria repo = new Memoria();
        repo.guardar(List.of(PAPA), LocalDateTime.of(2026, 11, 22, 8, 0));
        var servicio = new PreciosReferenciaService(repo, new SoloArchivo(),
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), BOGOTA), Runnable::run);
        var vacio = "<S:Envelope xmlns:S=\"http://www.w3.org/2003/05/soap-envelope\"><S:Body/></S:Envelope>";
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.cargarRespuesta(
                new java.io.ByteArrayInputStream(vacio.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isInstanceOf(com.agroruta.shared.exception.DatoInvalidoException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> servicio.cargarRespuesta(
                new java.io.ByteArrayInputStream("esto no es xml".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isInstanceOf(com.agroruta.shared.exception.DatoInvalidoException.class);
        assertThat(repo.precios).containsExactly(PAPA);
    }

    @Test
    void despuesDeMediaHoraSeVuelveAIntentar() {
        Memoria repo = new Memoria();
        AtomicInteger llamadas = new AtomicInteger();
        var mutable = new Object() { Instant ahora = Instant.parse("2026-11-23T12:00:00Z"); };
        Clock reloj = new Clock() {
            @Override public ZoneId getZone() { return BOGOTA; }
            @Override public Clock withZone(ZoneId z) { return this; }
            @Override public Instant instant() { return mutable.ahora; }
        };
        var servicio = new PreciosReferenciaService(repo, semanas -> {
            llamadas.incrementAndGet();
            throw new ExternalApiException("SIPSA respondió 503");
        }, reloj, Runnable::run);

        servicio.actualizarSiHaceFalta();
        mutable.ahora = mutable.ahora.plusSeconds(20 * 60);
        servicio.actualizarSiHaceFalta();
        assertThat(llamadas.get()).isEqualTo(1);
        mutable.ahora = mutable.ahora.plusSeconds(11 * 60);
        servicio.actualizarSiHaceFalta();
        assertThat(llamadas.get()).isEqualTo(2);
    }
}
