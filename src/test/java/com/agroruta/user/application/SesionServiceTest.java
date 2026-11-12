package com.agroruta.user.application;

import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.exception.SesionInvalidaException;
import com.agroruta.user.application.ports.in.SesionUseCase.Renovacion;
import com.agroruta.user.application.ports.out.SesionRepository;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Sesion;
import com.agroruta.user.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SesionServiceTest {

    /** Reloj que se puede adelantar. */
    static class RelojMovil extends Clock {
        Instant ahora = Instant.parse("2026-11-08T12:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
        void avanzar(Duration d) { ahora = ahora.plus(d); }
    }

    /** Repositorio en memoria. */
    static class SesionesEnMemoria implements SesionRepository {
        final List<Sesion> filas = new ArrayList<>();
        long sec = 1;

        @Override public Sesion guardar(Sesion s) {
            Sesion g = s.getId() != null ? s : new Sesion(sec++, s.getUsuarioId(), s.getTokenHash(), s.getCreadoEn(),
                    s.getExpiraEn(), s.getRotadoEn(), s.getRevocadoEn(), s.getFamilia(), s.getDispositivo(), s.getIniciadaEn());
            filas.removeIf(f -> f.getId().equals(g.getId()));
            filas.add(g);
            return g;
        }
        @Override public Optional<Sesion> buscarPorTokenHash(String h) {
            return filas.stream().filter(f -> f.getTokenHash().equals(h)).findFirst();
        }
        @Override public void revocarTodas(Long usuarioId, LocalDateTime ahora) {
            filas.stream().filter(f -> f.getUsuarioId().equals(usuarioId)).forEach(f -> f.revocar(ahora));
        }
        @Override public void borrarVencidas(Long usuarioId, LocalDateTime ahora) {
            filas.removeIf(f -> f.getUsuarioId().equals(usuarioId) && f.vencida(ahora));
        }
        @Override public List<Sesion> buscarAbiertas(Long usuarioId, LocalDateTime ahora) {
            return filas.stream().filter(f -> f.getUsuarioId().equals(usuarioId) && f.abierta(ahora)).toList();
        }
        @Override public void revocarFamilia(Long usuarioId, String familia, LocalDateTime ahora) {
            filas.stream().filter(f -> f.getUsuarioId().equals(usuarioId) && familia.equals(f.getFamilia())).forEach(f -> f.revocar(ahora));
        }
        @Override public int revocarOtras(Long usuarioId, String familia, LocalDateTime ahora) {
            List<Sesion> otras = filas.stream()
                    .filter(f -> f.getUsuarioId().equals(usuarioId) && !familia.equals(f.getFamilia()) && !f.revocada()).toList();
            otras.forEach(f -> f.revocar(ahora));
            return otras.size();
        }
        long abiertas() { return filas.stream().filter(f -> !f.revocada() && !f.rotada()).count(); }
    }

    private final RelojMovil reloj = new RelojMovil();
    private final SesionesEnMemoria repo = new SesionesEnMemoria();
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Usuario ana = new Usuario(7L, "Ana", "ana@x.com", "hash", Rol.Agricultor);
    private SesionService service;

    @BeforeEach
    void setUp() {
        when(usuarios.findById(7L)).thenReturn(Optional.of(ana));
        service = new SesionService(repo, usuarios, Duration.ofDays(30), reloj);
    }

    @Test
    void abrirGuardaSoloElHash() {
        String token = service.abrir(7L);

        assertThat(token).hasSizeGreaterThan(40);
        assertThat(repo.filas).singleElement()
                .satisfies(s -> assertThat(s.getTokenHash()).isEqualTo(SesionService.hash(token)).isNotEqualTo(token));
    }

    @Test
    void renovarRotaElToken() {
        String viejo = service.abrir(7L);

        Renovacion r = service.renovar(viejo);

        assertThat(r.usuario()).isSameAs(ana);
        assertThat(r.token()).isNotEqualTo(viejo);
        assertThat(repo.abiertas()).isEqualTo(1);
        // El token nuevo también sirve
        assertThat(service.renovar(r.token()).usuario()).isSameAs(ana);
    }

    @Test
    void dosPestanasRenovandoAlTiempoNoCierranLaSesion() {
        String viejo = service.abrir(7L);
        service.renovar(viejo);
        reloj.avanzar(Duration.ofSeconds(5));

        assertThat(service.renovar(viejo).usuario()).isSameAs(ana);
    }

    @Test
    void reusarUnTokenRotadoHaceRatoCierraTodasLasSesiones() {
        String viejo = service.abrir(7L);
        String nuevo = service.renovar(viejo).token();
        reloj.avanzar(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.renovar(viejo)).isInstanceOf(SesionInvalidaException.class);
        // Quien tenía el token bueno también queda por fuera
        assertThatThrownBy(() -> service.renovar(nuevo)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    void tokenVencidoDesconocidoOVacioNoSirve() {
        String token = service.abrir(7L);
        reloj.avanzar(Duration.ofDays(31));

        assertThatThrownBy(() -> service.renovar(token)).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> service.renovar("inventado")).isInstanceOf(SesionInvalidaException.class);
        assertThatThrownBy(() -> service.renovar(null)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    void usuarioDesactivadoNoRenueva() {
        String token = service.abrir(7L);
        ana.setActivo(false);

        assertThatThrownBy(() -> service.renovar(token)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    void cerrarYCambiarLaContrasenaRevocan() {
        String a = service.abrir(7L);
        String b = service.abrir(7L);

        service.cerrar(a);
        assertThatThrownBy(() -> service.renovar(a)).isInstanceOf(SesionInvalidaException.class);

        service.contrasenaCambiada(new ContrasenaCambiada(7L));
        assertThatThrownBy(() -> service.renovar(b)).isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    void abrirBorraLasSesionesVencidas() {
        service.abrir(7L);
        reloj.avanzar(Duration.ofDays(31));

        service.abrir(7L);

        assertThat(repo.filas).hasSize(1);
    }
}
