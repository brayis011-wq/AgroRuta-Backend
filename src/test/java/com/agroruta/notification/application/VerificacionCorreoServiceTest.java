package com.agroruta.notification.application;

import com.agroruta.notification.domain.Correo;
import com.agroruta.notification.domain.VerificacionCorreo;
import com.agroruta.notification.application.ports.out.VerificacionCorreoRepository;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VerificacionCorreoService")
class VerificacionCorreoServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final VerificacionCorreoRepository verificaciones = mock(VerificacionCorreoRepository.class);
    private final EnvioCorreo envio = mock(EnvioCorreo.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), RecuperarContrasenaService.ZONA);
    private final LocalDateTime ahora = LocalDateTime.now(reloj);
    private VerificacionCorreoService service;
    private Usuario ana;

    @BeforeEach
    void setUp() {
        service = new VerificacionCorreoService(true, usuarios, verificaciones,
                new MensajesCorreo("https://agroruta.app"), envio, reloj);
        ana = new Usuario(7L, "Ana Pérez", "ana@x.com", "h", Rol.Agricultor);
        ana.setEmailVerificado(false);
        when(verificaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("el enlace lleva el token, se guarda solo su hash y vence en 24 horas")
    void enviaEnlace() {
        service.enviarEnlace(ana);

        ArgumentCaptor<VerificacionCorreo> v = ArgumentCaptor.forClass(VerificacionCorreo.class);
        verify(verificaciones).guardar(v.capture());
        ArgumentCaptor<Correo> c = ArgumentCaptor.forClass(Correo.class);
        verify(envio).enSegundoPlano(c.capture());
        String token = c.getValue().texto().replaceAll("(?s).*token=([A-Za-z0-9_-]+).*", "$1");
        assertThat(v.getValue().getTokenHash()).isEqualTo(RecuperarContrasenaService.hash(token));
        assertThat(v.getValue().getExpiraEn()).isEqualTo(ahora.plusHours(24));
        assertThat(c.getValue().html()).contains("https://agroruta.app/auth/verify?token=" + token);
        assertThat(c.getValue().asunto()).isEqualTo("Confirma tu correo en AgroRuta");
    }

    @Test
    @DisplayName("confirmar: marca el correo como verificado y gasta el enlace")
    void confirma() {
        VerificacionCorreo v = new VerificacionCorreo(1L, 7L, RecuperarContrasenaService.hash("tok"),
                ahora.minusHours(1), ahora.plusHours(23), null);
        when(verificaciones.buscarPorTokenHash(RecuperarContrasenaService.hash("tok"))).thenReturn(Optional.of(v));
        when(usuarios.findById(7L)).thenReturn(Optional.of(ana));

        Usuario u = service.confirmar("tok");

        assertThat(u.isEmailVerificado()).isTrue();
        verify(usuarios).save(ana);
        assertThat(v.getUsadoEn()).isEqualTo(ahora);
    }

    @Test
    @DisplayName("confirmar con un enlace vencido, usado o inventado: error")
    void enlaceMalo() {
        VerificacionCorreo vencido = new VerificacionCorreo(1L, 7L, "h", ahora.minusDays(2), ahora.minusDays(1), null);
        when(verificaciones.buscarPorTokenHash(RecuperarContrasenaService.hash("vencido"))).thenReturn(Optional.of(vencido));
        for (String t : new String[]{"vencido", "otro", " "}) {
            assertThatThrownBy(() -> service.confirmar(t)).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no es válido");
        }
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("reenviar: solo a cuentas sin confirmar y máximo 3 por hora")
    void reenviar() {
        when(usuarios.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        service.reenviar("ana@x.com");
        verify(envio).enSegundoPlano(any());

        org.mockito.Mockito.reset(envio);
        when(verificaciones.contarDesde(7L, ahora.minusHours(1))).thenReturn(3L);
        service.reenviar("ana@x.com");
        verify(envio, never()).enSegundoPlano(any());

        ana.setEmailVerificado(true);
        when(verificaciones.contarDesde(7L, ahora.minusHours(1))).thenReturn(0L);
        service.reenviar("ana@x.com");
        when(usuarios.findByEmail("nadie@x.com")).thenReturn(Optional.empty());
        service.reenviar("nadie@x.com");
        verify(envio, never()).enSegundoPlano(any());
    }

    @Test
    @DisplayName("se puede apagar con VERIFICAR_CORREO=false")
    void apagada() {
        assertThat(new VerificacionCorreoService(false, usuarios, verificaciones,
                new MensajesCorreo("u"), envio, reloj).activa()).isFalse();
        assertThat(service.activa()).isTrue();
    }
}
