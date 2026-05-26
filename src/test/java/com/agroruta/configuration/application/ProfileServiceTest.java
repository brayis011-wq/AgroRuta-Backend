package com.agroruta.configuration.application;

import com.agroruta.configuration.domain.Perfil;
import com.agroruta.configuration.domain.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ProfileService}.
 *
 * Convenciones:
 *  - @Nested agrupa por caso de uso (port in).
 *  - Nombre de método: deberiaX_cuandoY
 *  - Sin dependencias de Spring Context → ejecución rápida.
 *  - Cobertura de líneas y ramas diseñada para superar el umbral de SonarQube.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    // ── puertos secundarios (driven) ─────────────────────────────────────────
    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    // ── servicio bajo prueba ──────────────────────────────────────────────────
    @InjectMocks
    private ProfileService profileService;

    // ── fixture compartido ────────────────────────────────────────────────────
    private Perfil perfilEjemplo;

    @BeforeEach
    void configurarFixture() {
        perfilEjemplo = new Perfil();
        perfilEjemplo.setId(1L);
        perfilEjemplo.setNombre("Ana Gómez");
        perfilEjemplo.setEmail("ana@agroruta.com");
        perfilEjemplo.setTelefono("3001234567");
        perfilEjemplo.setFotoPerfil("https://cdn.agroruta.com/foto.jpg");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GetProfileUseCase → obtenerPorEmail
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("obtenerPorEmail")
    class ObtenerPorEmail {

        @Test
        @DisplayName("debería retornar el perfil cuando el email existe")
        void deberiaRetornarPerfil_cuandoEmailExiste() {
            // given
            String email = "ana@agroruta.com";
            when(profileRepository.buscarPorEmail(email))
                    .thenReturn(Optional.of(perfilEjemplo));

            // when
            Perfil resultado = profileService.obtenerPorEmail(email);

            // then
            assertThat(resultado).isNotNull();
            assertThat(resultado.getEmail()).isEqualTo(email);
            assertThat(resultado.getNombre()).isEqualTo("Ana Gómez");
            verify(profileRepository, times(1)).buscarPorEmail(email);
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando el email no existe")
        void deberiaLanzarExcepcion_cuandoEmailNoExiste() {
            // given
            String emailInexistente = "fantasma@agroruta.com";
            when(profileRepository.buscarPorEmail(emailInexistente))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> profileService.obtenerPorEmail(emailInexistente))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Perfil no encontrado");

            verify(profileRepository, times(1)).buscarPorEmail(emailInexistente);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UpdateProfileUseCase → actualizar
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        @DisplayName("debería actualizar y retornar el perfil cuando el id existe")
        void deberiaActualizarPerfil_cuandoIdExiste() {
            // given
            Long id          = 1L;
            String nombre    = "Ana Actualizada";
            String telefono  = "3109876543";
            String foto      = "https://cdn.agroruta.com/nueva-foto.jpg";

            when(profileRepository.buscarPorId(id))
                    .thenReturn(Optional.of(perfilEjemplo));
            when(profileRepository.guardar(any(Perfil.class)))
                    .thenReturn(perfilEjemplo);

            // when
            Perfil resultado = profileService.actualizar(id, nombre, telefono, foto);

            // then
            assertThat(resultado).isNotNull();
            verify(profileRepository, times(1)).buscarPorId(id);
            verify(profileRepository, times(1)).guardar(perfilEjemplo);
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando el id no existe")
        void deberiaLanzarExcepcion_cuandoIdNoExiste() {
            // given
            Long idInexistente = 99L;
            when(profileRepository.buscarPorId(idInexistente))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(
                    () -> profileService.actualizar(idInexistente, "X", "Y", "Z"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Perfil no encontrado");

            verify(profileRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("debería persistir el perfil modificado exactamente una vez")
        void deberiaPersistirUnaVez_cuandoActualizaExitosamente() {
            // given
            when(profileRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(perfilEjemplo));
            when(profileRepository.guardar(perfilEjemplo))
                    .thenReturn(perfilEjemplo);

            // when
            profileService.actualizar(1L, "Nuevo Nombre", "3000000000", "url");

            // then
            verify(profileRepository, times(1)).guardar(perfilEjemplo);
            verifyNoMoreInteractions(profileRepository);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ChangePasswordUseCase → cambiarPassword
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cambiarPassword")
    class CambiarPassword {

        private static final Long   ID              = 1L;
        private static final String PASSWORD_ACTUAL = "secret123";
        private static final String HASH_ACTUAL     = "$2a$10$hash_simulado";
        private static final String NUEVA_PASSWORD  = "nuevo_secret456";
        private static final String NUEVO_HASH      = "$2a$10$nuevo_hash_simulado";

        @Test
        @DisplayName("debería cambiar la contraseña cuando la actual es correcta")
        void deberiaCambiarPassword_cuandoPasswordActualEsCorrecta() {
            // given
            when(profileRepository.obtenerPasswordHash(ID)).thenReturn(HASH_ACTUAL);
            when(passwordEncoder.matches(PASSWORD_ACTUAL, HASH_ACTUAL)).thenReturn(true);
            when(passwordEncoder.encode(NUEVA_PASSWORD)).thenReturn(NUEVO_HASH);

            // when
            profileService.cambiarPassword(ID, PASSWORD_ACTUAL, NUEVA_PASSWORD);

            // then
            verify(profileRepository, times(1)).obtenerPasswordHash(ID);
            verify(passwordEncoder, times(1)).matches(PASSWORD_ACTUAL, HASH_ACTUAL);
            verify(passwordEncoder, times(1)).encode(NUEVA_PASSWORD);
            verify(profileRepository, times(1)).cambiarPassword(ID, NUEVO_HASH);
        }

        @Test
        @DisplayName("debería lanzar RuntimeException cuando la contraseña actual es incorrecta")
        void deberiaLanzarExcepcion_cuandoPasswordActualEsIncorrecta() {
            // given
            when(profileRepository.obtenerPasswordHash(ID)).thenReturn(HASH_ACTUAL);
            when(passwordEncoder.matches(PASSWORD_ACTUAL, HASH_ACTUAL)).thenReturn(false);

            // when / then
            assertThatThrownBy(
                    () -> profileService.cambiarPassword(ID, PASSWORD_ACTUAL, NUEVA_PASSWORD))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("La contraseña actual es incorrecta");

            verify(passwordEncoder, never()).encode(anyString());
            verify(profileRepository, never()).cambiarPassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("no debería llamar encode ni cambiarPassword si las contraseñas no coinciden")
        void noDeberiaEncodeNiGuardar_cuandoPasswordNoCoincide() {
            // given
            when(profileRepository.obtenerPasswordHash(ID)).thenReturn(HASH_ACTUAL);
            when(passwordEncoder.matches(anyString(), eq(HASH_ACTUAL))).thenReturn(false);

            // when / then
            assertThatThrownBy(
                    () -> profileService.cambiarPassword(ID, "wrongPass", NUEVA_PASSWORD))
                    .isInstanceOf(RuntimeException.class);

            verifyNoMoreInteractions(passwordEncoder);   // .encode nunca se llama
        }

        @Test
        @DisplayName("debería consultar el hash exactamente una vez por llamada")
        void deberiaConsultarHashUnaVez() {
            // given
            when(profileRepository.obtenerPasswordHash(ID)).thenReturn(HASH_ACTUAL);
            when(passwordEncoder.matches(PASSWORD_ACTUAL, HASH_ACTUAL)).thenReturn(true);
            when(passwordEncoder.encode(NUEVA_PASSWORD)).thenReturn(NUEVO_HASH);

            // when
            profileService.cambiarPassword(ID, PASSWORD_ACTUAL, NUEVA_PASSWORD);

            // then
            verify(profileRepository, times(1)).obtenerPasswordHash(ID);
        }
    }
}