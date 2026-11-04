package com.agroruta.admin.infrastructure.web;

import com.agroruta.admin.application.ports.in.AdministrarSistemaUseCase;
import com.agroruta.admin.domain.AgricultorResumen;
import com.agroruta.admin.domain.ConteoAgricultor;
import com.agroruta.admin.infrastructure.web.dto.AgricultorAdminResponse;
import com.agroruta.admin.infrastructure.web.dto.CambiarEstadoUsuarioRequest;
import com.agroruta.admin.infrastructure.web.dto.CambiarRolUsuarioRequest;
import com.agroruta.admin.infrastructure.web.dto.UsuarioAdminResponse;
import com.agroruta.user.domain.Rol;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Mock private AdministrarSistemaUseCase useCase;
    @InjectMocks private AdminController controller;

    private Authentication auth;
    private Usuario ana;

    @BeforeEach
    void setUp() {
        Usuario admin = new Usuario(1L, "Admin", "admin@agroruta.com", "hash", Rol.Administrador);
        CustomUserDetails principal = new CustomUserDetails(admin);
        auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        ana = new Usuario(3L, "Ana", "ana@agroruta.com", "hash", Rol.Agricultor);
    }

    @Test
    @DisplayName("usuarios no expone la contraseña")
    void usuariosSinPassword() {
        when(useCase.listarUsuarios()).thenReturn(List.of(ana));

        List<UsuarioAdminResponse> body = controller.usuarios().getBody();

        assertThat(body).hasSize(1);
        assertThat(body.get(0).email()).isEqualTo("ana@agroruta.com");
        assertThat(body.get(0).rol()).isEqualTo("Agricultor");
        assertThat(UsuarioAdminResponse.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("password");
    }

    @Test
    @DisplayName("cambiarEstado pasa el id del administrador autenticado")
    void cambiarEstadoUsaIdDelAdmin() {
        ana.desactivar();
        when(useCase.cambiarEstado(1L, 3L, false)).thenReturn(ana);

        ResponseEntity<UsuarioAdminResponse> r =
                controller.cambiarEstado(3L, new CambiarEstadoUsuarioRequest(false), auth);

        assertThat(r.getBody().activo()).isFalse();
    }

    @Test
    @DisplayName("cambiarRol pasa el id del administrador autenticado")
    void cambiarRolUsaIdDelAdmin() {
        Usuario cambiado = new Usuario(3L, "Ana", "ana@agroruta.com", "hash", Rol.Trabajador);
        when(useCase.cambiarRol(1L, 3L, "Trabajador")).thenReturn(cambiado);

        ResponseEntity<UsuarioAdminResponse> r =
                controller.cambiarRol(3L, new CambiarRolUsuarioRequest("Trabajador"), auth);

        assertThat(r.getBody().rol()).isEqualTo("Trabajador");
    }

    @Test
    @DisplayName("agricultores incluye sus totales")
    void agricultoresConTotales() {
        ConteoAgricultor conteo = new ConteoAgricultor(2, 10.0, 3, 4, new BigDecimal("900000"));
        when(useCase.listarAgricultores()).thenReturn(List.of(new AgricultorResumen(ana, conteo)));

        List<AgricultorAdminResponse> body = controller.agricultores().getBody();

        assertThat(body.get(0).fincas()).isEqualTo(2);
        assertThat(body.get(0).totalPagado()).isEqualByComparingTo("900000");
    }
}
