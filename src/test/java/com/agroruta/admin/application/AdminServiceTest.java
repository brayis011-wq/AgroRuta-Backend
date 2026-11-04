package com.agroruta.admin.application;

import com.agroruta.admin.application.ports.out.AdminEstadisticasPort;
import com.agroruta.admin.domain.AgricultorDetalle;
import com.agroruta.admin.domain.AgricultorResumen;
import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.domain.EstadisticasSistema;
import com.agroruta.admin.domain.ResumenSistema;
import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.domain.Finca;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.domain.UsuarioRepository;
import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AdminEstadisticasPort estadisticasPort;
    @Mock private FincaUseCase fincaUseCase;
    @Mock private TrabajadorUseCase trabajadorUseCase;
    @Mock private NominaUseCase nominaUseCase;

    @InjectMocks private AdminService service;

    private Usuario admin;
    private Usuario otroAdmin;
    private Usuario agricultor;
    private Usuario trabajador;

    @BeforeEach
    void setUp() {
        admin = usuario(1L, "Admin", Rol.Administrador);
        otroAdmin = usuario(2L, "Otro admin", Rol.Administrador);
        agricultor = usuario(3L, "Ana", Rol.Agricultor);
        trabajador = usuario(4L, "Luis", Rol.Trabajador);
    }

    private static Usuario usuario(Long id, String nombre, Rol rol) {
        return new Usuario(id, nombre, nombre.toLowerCase().replace(' ', '.') + "@agroruta.com", "hash", rol);
    }

    // ── resumen ──────────────────────────────────────────────────────────────
    @Test
    @DisplayName("resumen cuenta usuarios activos por rol y separa los inactivos")
    void resumenCuentaUsuarios() {
        trabajador.desactivar();
        when(usuarioRepository.findAll()).thenReturn(List.of(admin, agricultor, trabajador));
        EstadisticasSistema stats = new EstadisticasSistema(
                2, 15.5, 4, 3, 120.0, 6, 1, new BigDecimal("500000"));
        when(estadisticasPort.estadisticasSistema()).thenReturn(stats);

        ResumenSistema r = service.resumen();

        assertThat(r.usuariosActivos()).isEqualTo(2);
        assertThat(r.usuariosInactivos()).isEqualTo(1);
        assertThat(r.administradores()).isEqualTo(1);
        assertThat(r.agricultores()).isEqualTo(1);
        assertThat(r.trabajadoresUsuarios()).isZero();
        assertThat(r.estadisticas()).isEqualTo(stats);
    }

    // ── cambiarEstado ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cambiarEstado")
    class CambiarEstado {

        @Test
        @DisplayName("desactiva a un agricultor")
        void desactivaAgricultor() {
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(agricultor));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Usuario r = service.cambiarEstado(1L, 3L, false);

            assertThat(r.isActivo()).isFalse();
            verify(usuarioRepository).save(agricultor);
        }

        @Test
        @DisplayName("reactiva una cuenta desactivada")
        void reactiva() {
            agricultor.desactivar();
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(agricultor));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.cambiarEstado(1L, 3L, true).isActivo()).isTrue();
        }

        @Test
        @DisplayName("no permite que el administrador se desactive a sí mismo")
        void noSeDesactivaASiMismo() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.cambiarEstado(1L, 1L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("propia cuenta");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("no permite desactivar al último administrador activo")
        void noDesactivaUltimoAdmin() {
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
            when(usuarioRepository.countActivosPorRol(Rol.Administrador)).thenReturn(1L);

            assertThatThrownBy(() -> service.cambiarEstado(1L, 2L, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("al menos un administrador");
        }

        @Test
        @DisplayName("permite desactivar a otro administrador si queda más de uno")
        void desactivaOtroAdmin() {
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
            when(usuarioRepository.countActivosPorRol(Rol.Administrador)).thenReturn(2L);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.cambiarEstado(1L, 2L, false).isActivo()).isFalse();
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si el usuario no existe")
        void usuarioInexistente() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cambiarEstado(1L, 99L, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── cambiarRol ───────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cambiarRol")
    class CambiarRol {

        @Test
        @DisplayName("cambia el rol sin importar mayúsculas")
        void cambiaRol() {
            when(usuarioRepository.findById(4L)).thenReturn(Optional.of(trabajador));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.cambiarRol(1L, 4L, "agricultor").getRol()).isEqualTo(Rol.Agricultor);
        }

        @Test
        @DisplayName("no permite cambiar el propio rol")
        void noCambiaPropioRol() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.cambiarRol(1L, 1L, "Agricultor"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("propio rol");
        }

        @Test
        @DisplayName("no permite quitar el rol al último administrador")
        void noQuitaUltimoAdmin() {
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroAdmin));
            when(usuarioRepository.countActivosPorRol(Rol.Administrador)).thenReturn(1L);

            assertThatThrownBy(() -> service.cambiarRol(1L, 2L, "Agricultor"))
                    .isInstanceOf(BusinessException.class);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda nada si el rol es el mismo")
        void mismoRol() {
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(agricultor));

            service.cambiarRol(1L, 3L, "Agricultor");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rechaza un rol que no existe")
        void rolInvalido() {
            assertThatThrownBy(() -> service.cambiarRol(1L, 3L, "Jefe"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rol no válido");
            verifyNoInteractions(usuarioRepository);
        }
    }

    // ── agricultores ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("listarAgricultores solo incluye agricultores y les asigna sus totales")
    void listarAgricultores() {
        Usuario sinDatos = usuario(5L, "Pedro", Rol.Agricultor);
        when(usuarioRepository.findAll()).thenReturn(List.of(admin, agricultor, trabajador, sinDatos));
        ConteoAgricultor conteo = new ConteoAgricultor(2, 10.0, 3, 4, new BigDecimal("900000"));
        when(estadisticasPort.conteosPorAgricultor()).thenReturn(Map.of(3L, conteo));

        List<AgricultorResumen> lista = service.listarAgricultores();

        assertThat(lista).extracting(a -> a.usuario().getId()).containsExactly(3L, 5L);
        assertThat(lista.get(0).conteo()).isEqualTo(conteo);
        assertThat(lista.get(1).conteo()).isEqualTo(ConteoAgricultor.VACIO);
    }

    @Test
    @DisplayName("detalleAgricultor junta fincas, trabajadores y nóminas del agricultor")
    void detalleAgricultor() {
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(agricultor));
        when(estadisticasPort.conteosPorAgricultor()).thenReturn(Map.of());
        Finca finca = new Finca(7L, "La Esperanza", "Vereda El Monte", 12.5, 3L);
        when(fincaUseCase.listarFincasPorAgricultor(3L)).thenReturn(List.of(finca));
        when(trabajadorUseCase.listarTodosDe(3L)).thenReturn(List.of());
        when(nominaUseCase.listarTodasDe(3L)).thenReturn(List.of());

        AgricultorDetalle d = service.detalleAgricultor(3L);

        assertThat(d.resumen().usuario()).isSameAs(agricultor);
        assertThat(d.fincas()).containsExactly(finca);
    }

    @Test
    @DisplayName("detalleAgricultor da 404 si el usuario no es agricultor")
    void detalleNoAgricultor() {
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(trabajador));

        assertThatThrownBy(() -> service.detalleAgricultor(4L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
