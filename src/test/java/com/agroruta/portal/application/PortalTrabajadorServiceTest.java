package com.agroruta.portal.application;

import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.portal.domain.EstadoPortal;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.application.ports.out.UsuarioRepository;
import com.agroruta.worker.application.ports.in.JornalUseCase;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.application.ports.in.PagoUseCase;
import com.agroruta.worker.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortalTrabajadorService")
class PortalTrabajadorServiceTest {

    @Mock private TrabajadorRepository trabajadorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JornalUseCase jornalUseCase;
    @Mock private NominaUseCase nominaUseCase;
    @Mock private PagoUseCase pagoUseCase;

    @Mock private org.springframework.context.ApplicationEventPublisher eventos;

    @InjectMocks private PortalTrabajadorService service;

    private Trabajador luis;

    @BeforeEach
    void setUp() {
        Cargo cargo = new Cargo(1L, "Recolector", "", new BigDecimal("60000"), true);
        luis = new Trabajador(5L, "Luis", "Herrera", "1.052.334.120", null, null,
                LocalDate.of(2025, 1, 10), TipoContrato.JORNAL, cargo);
        luis.setAgricultorId(3L);
    }

    @Test
    @DisplayName("normalizar quita puntos, espacios y guiones")
    void normalizar() {
        assertThat(PortalTrabajadorService.normalizar(" 1.052.334-120 ")).isEqualTo("1052334120");
    }

    @Nested
    @DisplayName("solicitarAcceso")
    class Solicitar {

        @Test
        @DisplayName("busca la cédula normalizada y deja la solicitud pendiente")
        void creaSolicitud() {
            when(trabajadorRepository.buscarPorCedulaNormalizada("1052334120")).thenReturn(Optional.of(luis));
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.empty());

            service.solicitarAcceso(9L, "Luis", "luis@gmail.com", "1.052.334.120");

            assertThat(luis.getAccesoEstado()).isEqualTo(EstadoAccesoPortal.PENDIENTE);
            verify(trabajadorRepository).guardar(luis);
        }

        @Test
        @DisplayName("rechaza una cédula que ningún agricultor registró")
        void cedulaDesconocida() {
            when(trabajadorRepository.buscarPorCedulaNormalizada("123456")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validarCedulaDisponible("123456"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ningún agricultor");
        }

        @Test
        @DisplayName("rechaza una cédula que ya usa otra cuenta")
        void cedulaOcupada() {
            luis.solicitarAcceso(7L, "Otra", "otra@gmail.com");
            when(trabajadorRepository.buscarPorCedulaNormalizada("1052334120")).thenReturn(Optional.of(luis));

            assertThatThrownBy(() -> service.validarCedulaDisponible("1052334120"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya tiene una cuenta");
        }

        @Test
        @DisplayName("libera la solicitud rechazada anterior de la misma cuenta")
        void liberaAnterior() {
            Trabajador anterior = new Trabajador(8L, "Otro", "Señor", "777777", null, null,
                    LocalDate.of(2025, 1, 1), TipoContrato.JORNAL, luis.getCargo());
            anterior.solicitarAcceso(9L, "Luis", "luis@gmail.com");
            anterior.rechazarAcceso();
            when(trabajadorRepository.buscarPorCedulaNormalizada("1052334120")).thenReturn(Optional.of(luis));
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.of(anterior));

            service.solicitarAcceso(9L, "Luis", "luis@gmail.com", "1052334120");

            assertThat(anterior.getUsuarioId()).isNull();
            assertThat(luis.getUsuarioId()).isEqualTo(9L);
            verify(trabajadorRepository).guardar(anterior);
            verify(trabajadorRepository).guardar(luis);
        }
    }

    @Nested
    @DisplayName("estado y datos")
    class EstadoYDatos {

        @Test
        @DisplayName("sin vínculo devuelve SIN_SOLICITUD")
        void sinVinculo() {
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.empty());

            assertThat(service.estado(9L).estado()).isEqualTo(EstadoPortal.SIN_SOLICITUD);
        }

        @Test
        @DisplayName("pendiente no expone los datos del trabajador")
        void pendienteSinDatos() {
            luis.solicitarAcceso(9L, "Luis", "luis@gmail.com");
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.of(luis));

            EstadoPortal e = service.estado(9L);

            assertThat(e.estado()).isEqualTo("PENDIENTE");
            assertThat(e.trabajador()).isNull();
        }

        @Test
        @DisplayName("aprobado incluye los datos y el nombre del empleador")
        void aprobado() {
            luis.solicitarAcceso(9L, "Luis", "luis@gmail.com");
            luis.aprobarAcceso();
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.of(luis));
            when(usuarioRepository.findById(3L))
                    .thenReturn(Optional.of(new Usuario(3L, "Ana Rodríguez", "ana@gmail.com", "x", Rol.Agricultor)));

            EstadoPortal e = service.estado(9L);

            assertThat(e.estado()).isEqualTo("APROBADO");
            assertThat(e.trabajador()).isSameAs(luis);
            assertThat(e.empleador()).isEqualTo("Ana Rodríguez");
        }

        @Test
        @DisplayName("sin aprobación no puede ver sus pagos")
        void pagosSinAprobacion() {
            luis.solicitarAcceso(9L, "Luis", "luis@gmail.com");
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.of(luis));

            assertThatThrownBy(() -> service.misPagos(9L)).isInstanceOf(BusinessException.class);
            verifyNoInteractions(pagoUseCase);
        }

        @Test
        @DisplayName("aprobado ve sus nóminas sin las anuladas")
        void nominasSinAnuladas() {
            luis.solicitarAcceso(9L, "Luis", "luis@gmail.com");
            luis.aprobarAcceso();
            when(trabajadorRepository.buscarPorUsuarioId(9L)).thenReturn(Optional.of(luis));
            Nomina vigente = new Nomina(1L, luis, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15), List.of());
            Nomina anulada = new Nomina(2L, luis, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15), List.of());
            anulada.anular();
            when(nominaUseCase.listarPorTrabajador(5L)).thenReturn(List.of(vigente, anulada));

            assertThat(service.misNominas(9L)).containsExactly(vigente);
        }
    }
}
