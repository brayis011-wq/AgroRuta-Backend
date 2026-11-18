package com.agroruta.notification.application;

import com.agroruta.notification.application.ports.in.BandejaAvisosUseCase;
import com.agroruta.notification.domain.AvisoBandeja;
import com.agroruta.shared.evento.AccesoPortalResuelto;
import com.agroruta.shared.evento.AccesoPortalSolicitado;
import com.agroruta.shared.evento.PagoRegistrado;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.worker.application.ports.out.PagoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Nomina;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BandejaAvisosListener")
class BandejaAvisosListenerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TrabajadorRepository trabajadores = mock(TrabajadorRepository.class);
    private final PagoRepository pagos = mock(PagoRepository.class);
    private final BandejaAvisosUseCase bandeja = mock(BandejaAvisosUseCase.class);
    private BandejaAvisosListener listener;

    private final Usuario agricultor = new Usuario(1L, "Carlos Ruiz", "carlos@x.com", "h", Rol.Agricultor);
    private final Usuario cuenta = new Usuario(5L, "Pedro Gómez", "pedro@x.com", "h", Rol.Trabajador);
    private Trabajador pedro;

    @BeforeEach
    void setUp() {
        listener = new BandejaAvisosListener(usuarios, trabajadores, pagos, bandeja);
        pedro = new Trabajador(20L, "Pedro", "Gómez", "1052", null, null, LocalDate.of(2026, 1, 1), TipoContrato.JORNAL, null);
        pedro.setAgricultorId(1L);
        pedro.solicitarAcceso(5L, "Pedro Gómez", "pedro@x.com");
        when(trabajadores.buscarPorId(20L)).thenReturn(Optional.of(pedro));
        when(usuarios.findById(1L)).thenReturn(Optional.of(agricultor));
        when(usuarios.findById(5L)).thenReturn(Optional.of(cuenta));
    }

    private AvisoBandeja.Nuevo guardado() {
        ArgumentCaptor<AvisoBandeja.Nuevo> c = ArgumentCaptor.forClass(AvisoBandeja.Nuevo.class);
        verify(bandeja).avisar(c.capture());
        return c.getValue();
    }

    @Test
    @DisplayName("solicitud de acceso: queda en la campana del agricultor dueño del trabajador")
    void solicitud() {
        listener.accesoSolicitado(new AccesoPortalSolicitado(20L));
        AvisoBandeja.Nuevo a = guardado();
        assertThat(a.usuarioId()).isEqualTo(1L);
        assertThat(a.tipo()).isEqualTo(AvisoBandeja.Tipo.ACCESO_SOLICITADO);
        assertThat(a.datos()).containsEntry("trabajador", "Pedro Gómez");
        assertThat(a.ruta()).isEqualTo("/worker");
        assertThat(a.etiqueta()).isEqualTo("acceso-20");
    }

    @Test
    @DisplayName("pago: solo al trabajador con el acceso aprobado, con el monto y el periodo")
    void pago() {
        Nomina n = new Nomina(30L, pedro, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), List.of());
        Pago p = new Pago(40L, n, LocalDate.of(2026, 11, 5), new BigDecimal("650000"), MetodoPago.TRANSFERENCIA, null);
        when(pagos.buscarPorId(40L)).thenReturn(Optional.of(p));

        listener.pagoRegistrado(new PagoRegistrado(40L));
        verify(bandeja, never()).avisar(any()); // la solicitud sigue pendiente

        pedro.aprobarAcceso();
        listener.pagoRegistrado(new PagoRegistrado(40L));
        AvisoBandeja.Nuevo a = guardado();
        assertThat(a.usuarioId()).isEqualTo(5L);
        assertThat(a.tipo()).isEqualTo(AvisoBandeja.Tipo.PAGO_REGISTRADO);
        assertThat(a.datos()).containsEntry("monto", "650000").containsEntry("inicio", "2026-10-01").containsEntry("fin", "2026-10-15");
        assertThat(a.cuerpo()).contains("650.000");
        assertThat(a.etiqueta()).isEqualTo("pago-40");
    }

    @Test
    @DisplayName("acceso resuelto: al trabajador, con el nombre del empleador")
    void resuelto() {
        listener.accesoResuelto(new AccesoPortalResuelto(20L, true, false));
        AvisoBandeja.Nuevo a = guardado();
        assertThat(a.usuarioId()).isEqualTo(5L);
        assertThat(a.tipo()).isEqualTo(AvisoBandeja.Tipo.ACCESO_RESUELTO);
        assertThat(a.titulo()).isEqualTo("Ya puedes entrar a tu portal");
        assertThat(a.datos()).containsEntry("aprobado", true).containsEntry("empleador", "Carlos Ruiz");
    }

    @Test
    @DisplayName("si algo falla al preparar el aviso, no se propaga")
    void tolerante() {
        when(trabajadores.buscarPorId(20L)).thenThrow(new RuntimeException("base caída"));
        assertThatCode(() -> listener.accesoSolicitado(new AccesoPortalSolicitado(20L))).doesNotThrowAnyException();
    }
}
