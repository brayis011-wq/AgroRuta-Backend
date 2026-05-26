package com.agroruta.configuration.infrastructure.web;

import com.agroruta.configuration.application.ports.in.ChangePasswordUseCase;
import com.agroruta.configuration.application.ports.in.GetProfileUseCase;
import com.agroruta.configuration.application.ports.in.UpdateProfileUseCase;
import com.agroruta.configuration.domain.Perfil;
import com.agroruta.user.domain.Rol;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias de la capa web para {@link ProfileController}.
 *
 * Patrón: standaloneSetup + MockitoExtension (sin contexto Spring completo).
 * El @AuthenticationPrincipal se resuelve configurando el SecurityContextHolder
 * y registrando AuthenticationPrincipalArgumentResolver en MockMvc.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController - Pruebas Unitarias")
class ProfileControllerTest {

    private static final String BASE_URL     = "/api/configuration/perfil";
    private static final String PASSWORD_URL = BASE_URL + "/password";
    private static final String USER_EMAIL   = "ana@agroruta.com";

    @Mock private GetProfileUseCase     obtenerPerfilUseCase;
    @Mock private UpdateProfileUseCase  actualizarPerfilUseCase;
    @Mock private ChangePasswordUseCase cambiarPasswordUseCase;

    @InjectMocks
    private ProfileController profileController;

    private MockMvc     mockMvc;
    private ObjectMapper objectMapper;
    private Perfil       perfilEjemplo;
    private UserDetails  userDetails;

    @BeforeEach
    void configurar() {
        objectMapper = new ObjectMapper();

        // MockMvc con el resolver de @AuthenticationPrincipal
        mockMvc = MockMvcBuilders
                .standaloneSetup(profileController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        // Usuario simulado en el SecurityContext
        userDetails = User.withUsername(USER_EMAIL)
                .password("irrelevante")
                .authorities(new SimpleGrantedAuthority("ROLE_AGRICULTOR"))
                .build();

        autenticar(userDetails);

        // Perfil de dominio de ejemplo
        perfilEjemplo = new Perfil(
                1L, "Ana Gómez", USER_EMAIL,
                "3001234567", "https://cdn.agroruta.com/foto.jpg",
                Rol.Agricultor
        );
    }

    /** Registra el usuario en el SecurityContextHolder para el test actual. */
    private void autenticar(UserDetails ud) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities())
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /api/configuration/perfil
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("obtener()")
    class Obtener {

        @Test
        @DisplayName("debería retornar 200 con el perfil del usuario autenticado")
        void deberiaRetornar200_conPerfil() throws Exception {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL))
                    .thenReturn(perfilEjemplo);

            mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(obtenerPerfilUseCase, times(1)).obtenerPorEmail(USER_EMAIL);
            verifyNoInteractions(actualizarPerfilUseCase, cambiarPasswordUseCase);
        }

        @Test
        @DisplayName("debería propagar RuntimeException cuando el perfil no existe")
        void deberiaPropagar_cuandoPerfilNoExiste() {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL))
                    .thenThrow(new RuntimeException("Perfil no encontrado"));

            assertThatThrownBy(() ->
                    mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON)))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Perfil no encontrado");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUT /api/configuration/perfil
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        private String body(String nombre, String telefono, String foto) throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "nombre",     nombre,
                    "telefono",   telefono,
                    "fotoPerfil", foto
            ));
        }

        @Test
        @DisplayName("debería retornar 200 con el perfil actualizado")
        void deberiaRetornar200_cuandoDatosValidos() throws Exception {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL)).thenReturn(perfilEjemplo);
            when(actualizarPerfilUseCase.actualizar(eq(1L), anyString(), anyString(), anyString()))
                    .thenReturn(perfilEjemplo);

            mockMvc.perform(put(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Ana Actualizada", "3109876543",
                                    "https://cdn.agroruta.com/nueva.jpg")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(obtenerPerfilUseCase).obtenerPorEmail(USER_EMAIL);
            verify(actualizarPerfilUseCase).actualizar(
                    eq(1L),
                    eq("Ana Actualizada"),
                    eq("3109876543"),
                    eq("https://cdn.agroruta.com/nueva.jpg"));
        }

        @Test
        @DisplayName("debería pasar el id del perfil autenticado al use-case")
        void deberiaPasarIdDelPerfilAutenticado() throws Exception {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL)).thenReturn(perfilEjemplo);
            when(actualizarPerfilUseCase.actualizar(anyLong(), anyString(), anyString(), anyString()))
                    .thenReturn(perfilEjemplo);

            mockMvc.perform(put(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("X", "Y", "Z")));

            verify(actualizarPerfilUseCase).actualizar(eq(1L), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("debería propagar RuntimeException cuando el perfil no existe")
        void deberiaPropagar_cuandoPerfilNoExiste() {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL))
                    .thenThrow(new RuntimeException("Perfil no encontrado"));

            assertThatThrownBy(() ->
                    mockMvc.perform(put(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("X", "Y", "Z"))))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Perfil no encontrado");

            verifyNoInteractions(actualizarPerfilUseCase);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PATCH /api/configuration/perfil/password
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cambiarPassword()")
    class CambiarPassword {

        private String body(String actual, String nueva) throws Exception {
            return objectMapper.writeValueAsString(Map.of(
                    "passwordActual", actual,
                    "nuevaPassword",  nueva
            ));
        }

        @Test
        @DisplayName("debería retornar 204 cuando la contraseña se cambia exitosamente")
        void deberiaRetornar204_cuandoCambioExitoso() throws Exception {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL)).thenReturn(perfilEjemplo);
            doNothing().when(cambiarPasswordUseCase)
                    .cambiarPassword(anyLong(), anyString(), anyString());

            mockMvc.perform(patch(PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("secret123", "nuevoSecret456")))
                    .andExpect(status().isNoContent());

            verify(cambiarPasswordUseCase).cambiarPassword(1L, "secret123", "nuevoSecret456");
        }

        @Test
        @DisplayName("debería usar el id del perfil autenticado al cambiar la contraseña")
        void deberiaUsarIdDelPerfilAutenticado() throws Exception {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL)).thenReturn(perfilEjemplo);

            mockMvc.perform(patch(PASSWORD_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("secret123", "nuevoSecret456")));

            verify(cambiarPasswordUseCase).cambiarPassword(eq(1L), anyString(), anyString());
        }

        @Test
        @DisplayName("debería propagar RuntimeException cuando la contraseña actual es incorrecta")
        void deberiaPropagar_cuandoPasswordIncorrecta() {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL)).thenReturn(perfilEjemplo);
            doThrow(new RuntimeException("La contraseña actual es incorrecta"))
                    .when(cambiarPasswordUseCase)
                    .cambiarPassword(anyLong(), anyString(), anyString());

            assertThatThrownBy(() ->
                    mockMvc.perform(patch(PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("wrongPass", "nuevoSecret456"))))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("La contraseña actual es incorrecta");
        }

        @Test
        @DisplayName("debería propagar RuntimeException cuando el perfil no existe")
        void deberiaPropagar_cuandoPerfilNoExiste() {
            when(obtenerPerfilUseCase.obtenerPorEmail(USER_EMAIL))
                    .thenThrow(new RuntimeException("Perfil no encontrado"));

            assertThatThrownBy(() ->
                    mockMvc.perform(patch(PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("secret123", "nuevoSecret456"))))
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Perfil no encontrado");

            verifyNoInteractions(cambiarPasswordUseCase);
        }
    }
}