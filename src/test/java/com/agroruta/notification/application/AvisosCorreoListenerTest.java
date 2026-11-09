package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.application.ports.out.CorreoPort;
import com.agroruta.notification.domain.Correo;
import com.agroruta.notification.domain.PreferenciasCorreo;
import com.agroruta.shared.evento.AccesoPortalResuelto;
import com.agroruta.shared.evento.AccesoPortalSolicitado;
import com.agroruta.shared.evento.ContrasenaCambiada;
import com.agroruta.shared.evento.CuentaBloqueada;
import com.agroruta.shared.evento.PagoRegistrado;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.PagoRepository;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AvisosCorreoListener")
class AvisosCorreoListenerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
    private final PagoRepository pagos = mock(PagoRepository.class);
    private final PreferenciasCorreoUseCase preferencias = mock(PreferenciasCorreoUseCase.class);
    private final CorreoPort correo = mock(CorreoPort.class);
    private AvisosCorreoListener listener;

    private final Usuario agricultor = new Usuario(1L, "Carlos Ruiz", "carlos@x.com", "h", Rol.Agricultor);
    private final Usuario cuenta = new Usuario(5L, "Pedro Gómez", "pedro@x.com", "h", Rol.Trabajador);
    private Trabajador pedro;

    @BeforeEach
    void setUp() {
        listener = new AvisosCorreoListener(usuarios, trabajadores, pagos, preferencias,
                new MensajesCorreo("https://agroruta.app"), correo);
        pedro = new Trabajador(20L, "Pedro", "Gómez", "1052", null, null, LocalDate.of(2026, 1, 1), TipoContrato.JORNAL, null);
        pedro.setAgricultorId(1L);
        pedro.solicitarAcceso(5L, "Pedro Gómez", "pedro@x.com");
        when(trabajadores.buscarPorId(20L)).thenReturn(Optional.of(pedro));
        when(usuarios.findById(1L)).thenReturn(Optional.of(agricultor));
        when(usuarios.findById(5L)).thenReturn(Optional.of(cuenta));
        when(preferencias.obtener(anyLong())).thenAnswer(i -> PreferenciasCorreo.porDefecto(i.getArgument(0)));
    }

    private Correo enviado() {
        ArgumentCaptor<Correo> c = ArgumentCaptor.forClass(Correo.class);
        verify(correo).enviar(c.capture());
        return c.getValue();
    }

    @Test
    @DisplayName("solicitud de acceso: avisa al agricultor dueño del trabajador")
    void solicitud() {
        listener.accesoSolicitado(new AccesoPortalSolicitado(20L));
        Correo c = enviado();
        assertThat(c.para()).isEqualTo("carlos@x.com");
        assertThat(c.texto()).contains("Pedro Gómez (pedro@x.com)", "https://agroruta.app/worker");
    }

    @Test
    @DisplayName("solicitud de acceso: nada si el agricultor apagó esos avisos")
    void solicitudSinAvisos() {
        when(preferencias.obtener(1L)).thenReturn(new PreferenciasCorreo(1L, true, false));
        listener.accesoSolicitado(new AccesoPortalSolicitado(20L));
        verify(correo, never()).enviar(any());
    }

    @Test
    @DisplayName("acceso aprobado, rechazado o retirado: avisa a la cuenta del trabajador")
    void resuelto() {
        listener.accesoResuelto(new AccesoPortalResuelto(20L, true, false));
        Correo c = enviado();
        assertThat(c.para()).isEqualTo("pedro@x.com");
        assertThat(c.asunto()).contains("aprobado");
        assertThat(c.texto()).contains("Carlos Ruiz aprobó");

        assertThat(new MensajesCorreo("u").accesoResuelto("Pedro", "p@x", false, true, "Carlos").asunto()).contains("retirado");
        assertThat(new MensajesCorreo("u").accesoResuelto("Pedro", "p@x", false, false, null).texto()).contains("Tu empleador no aprobó");
    }

    @Test
    @DisplayName("pago: avisa al trabajador solo si tiene el portal aprobado")
    void pago() {
        Nomina n = new Nomina(30L, pedro, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), List.of());
        Pago p = new Pago(40L, n, LocalDate.of(2026, 11, 5), new BigDecimal("650000"), MetodoPago.TRANSFERENCIA, null);
        when(pagos.buscarPorId(40L)).thenReturn(Optional.of(p));

        listener.pagoRegistrado(new PagoRegistrado(40L));
        verify(correo, never()).enviar(any()); // todavía pendiente

        pedro.aprobarAcceso();
        listener.pagoRegistrado(new PagoRegistrado(40L));
        Correo c = enviado();
        assertThat(c.para()).isEqualTo("pedro@x.com");
        assertThat(c.texto()).contains("650.000", "Transferencia", "1 de octubre al 15 de octubre");
    }

    @Test
    @DisplayName("seguridad: contraseña cambiada y cuenta bloqueada")
    void seguridad() {
        when(usuarios.findByEmail("carlos@x.com")).thenReturn(Optional.of(agricultor));
        listener.cuentaBloqueada(new CuentaBloqueada("carlos@x.com", 15));
        assertThat(enviado().texto()).contains("15 minutos", "https://agroruta.app/auth/forgot-password");

        org.mockito.Mockito.reset(correo);
        listener.contrasenaCambiada(new ContrasenaCambiada(1L));
        assertThat(enviado().asunto()).contains("contraseña");
    }

    @Test
    @DisplayName("un error al armar el correo nunca se propaga")
    void errorNoSePropaga() {
        when(trabajadores.buscarPorId(99L)).thenThrow(new RuntimeException("base caída"));
        assertThatCode(() -> listener.accesoSolicitado(new AccesoPortalSolicitado(99L))).doesNotThrowAnyException();
    }
}
