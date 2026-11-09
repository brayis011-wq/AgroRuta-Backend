package com.agroruta.notification.application;

import com.agroruta.notification.domain.Correo;
import com.agroruta.notification.domain.RestablecimientoContrasena;
import com.agroruta.notification.domain.RestablecimientoRepository;
import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.user.application.IntentosLogin;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RecuperarContrasenaService")
class RecuperarContrasenaServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RestablecimientoRepository restablecimientos = mock(RestablecimientoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EnvioCorreo envio = mock(EnvioCorreo.class);
    private final IntentosLogin intentos = mock(IntentosLogin.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), RecuperarContrasenaService.ZONA);
    private final LocalDateTime ahora = LocalDateTime.now(reloj);
    private RecuperarContrasenaService service;
    private Usuario ana;

    @BeforeEach
    void setUp() {
        service = new RecuperarContrasenaService(usuarios, restablecimientos, encoder,
                new MensajesCorreo("https://agroruta.app"), envio, intentos, eventos, reloj);
        ana = new Usuario(7L, "Ana Pérez", "ana@x.com", "hash-viejo", Rol.Agricultor);
        when(restablecimientos.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("correo sin cuenta: no guarda ni envía nada (y no lo revela)")
    void sinCuenta() {
        when(usuarios.findByEmail("nadie@x.com")).thenReturn(Optional.empty());
        service.solicitar("nadie@x.com");
        verify(restablecimientos, never()).guardar(any());
        verify(envio, never()).enSegundoPlano(any());
    }

    @Test
    @DisplayName("cuenta desactivada: no envía enlace")
    void desactivada() {
        ana.desactivar();
        when(usuarios.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        service.solicitar("ana@x.com");
        verify(envio, never()).enSegundoPlano(any());
    }

    @Test
    @DisplayName("guarda solo el hash del token, vence en 30 minutos y el enlace lleva el token")
    void solicitarGuardaHashYEnvia() {
        when(usuarios.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        service.solicitar("ana@x.com");

        ArgumentCaptor<RestablecimientoContrasena> r = ArgumentCaptor.forClass(RestablecimientoContrasena.class);
        verify(restablecimientos).guardar(r.capture());
        verify(restablecimientos).anularPendientes(7L, ahora);
        ArgumentCaptor<Correo> c = ArgumentCaptor.forClass(Correo.class);
        verify(envio).enSegundoPlano(c.capture());

        String token = c.getValue().texto().replaceAll("(?s).*token=([A-Za-z0-9_-]+).*", "$1");
        assertThat(token).hasSizeGreaterThan(40);
        assertThat(r.getValue().getTokenHash()).isEqualTo(RecuperarContrasenaService.hash(token)).isNotEqualTo(token);
        assertThat(r.getValue().getExpiraEn()).isEqualTo(ahora.plusMinutes(30));
        assertThat(c.getValue().para()).isEqualTo("ana@x.com");
        assertThat(c.getValue().html()).contains("https://agroruta.app/auth/reset-password?token=" + token);
    }

    @Test
    @DisplayName("máximo 3 enlaces por hora para la misma cuenta")
    void limitePorHora() {
        when(usuarios.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        when(restablecimientos.contarDesde(7L, ahora.minusHours(1))).thenReturn(3L);
        service.solicitar("ana@x.com");
        verify(envio, never()).enSegundoPlano(any());
    }

    @Test
    @DisplayName("token válido: cambia la contraseña, gasta el enlace, desbloquea el login y avisa")
    void restablecer() {
        RestablecimientoContrasena r = new RestablecimientoContrasena(1L, 7L, RecuperarContrasenaService.hash("tok"),
                ahora.minusMinutes(5), ahora.plusMinutes(25), null);
        when(restablecimientos.buscarPorTokenHash(RecuperarContrasenaService.hash("tok"))).thenReturn(Optional.of(r));
        when(usuarios.findById(7L)).thenReturn(Optional.of(ana));
        when(encoder.encode("nueva-clave-1")).thenReturn("hash-nuevo");

        service.restablecer("tok", "nueva-clave-1");

        assertThat(ana.getPassword()).isEqualTo("hash-nuevo");
        verify(usuarios).save(ana);
        assertThat(r.getUsadoEn()).isEqualTo(ahora);
        verify(intentos).limpiar("ana@x.com");
        verify(eventos).publishEvent(new ContrasenaCambiada(7L));
    }

    @Test
    @DisplayName("token vencido, usado o inventado: error y no cambia nada")
    void tokenInvalido() {
        RestablecimientoContrasena vencido = new RestablecimientoContrasena(1L, 7L, "h", ahora.minusHours(1),
                ahora.minusMinutes(1), null);
        RestablecimientoContrasena usado = new RestablecimientoContrasena(2L, 7L, "h2", ahora.minusMinutes(5),
                ahora.plusMinutes(25), ahora.minusMinutes(1));
        when(restablecimientos.buscarPorTokenHash(RecuperarContrasenaService.hash("vencido"))).thenReturn(Optional.of(vencido));
        when(restablecimientos.buscarPorTokenHash(RecuperarContrasenaService.hash("usado"))).thenReturn(Optional.of(usado));
        when(restablecimientos.buscarPorTokenHash(eq(RecuperarContrasenaService.hash("otro")))).thenReturn(Optional.empty());

        for (String t : new String[]{"vencido", "usado", "otro", ""}) {
            assertThatThrownBy(() -> service.restablecer(t, "nueva-clave-1"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("no es válido");
        }
        verify(encoder, never()).encode(anyString());
        verify(usuarios, never()).save(any());
    }
}
