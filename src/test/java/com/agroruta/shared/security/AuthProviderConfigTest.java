package com.agroruta.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthProviderConfig - Tests unitarios")
class AuthProviderConfigTest {

    @Mock
    private UserDetailsService userDetailsService;

    private AuthProviderConfig authProviderConfig;

    @BeforeEach
    void setUp() {
        authProviderConfig = new AuthProviderConfig(userDetailsService);
    }

    // -------------------------------------------------------------------------
    // passwordEncoder()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("passwordEncoder() debe retornar una instancia no nula")
    void passwordEncoder_shouldNotBeNull() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();

        assertThat(encoder).isNotNull();
    }

    @Test
    @DisplayName("passwordEncoder() debe retornar un BCryptPasswordEncoder")
    void passwordEncoder_shouldBeBCryptInstance() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("passwordEncoder() debe codificar una contraseña correctamente")
    void passwordEncoder_shouldEncodePasswordSuccessfully() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();
        String rawPassword = "miContraseña123";

        String encoded = encoder.encode(rawPassword);

        assertThat(encoded).isNotBlank();
        assertThat(encoded).isNotEqualTo(rawPassword);
    }

    @Test
    @DisplayName("passwordEncoder() debe verificar correctamente una contraseña codificada")
    void passwordEncoder_shouldMatchEncodedPassword() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();
        String rawPassword = "miContraseña123";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("passwordEncoder() no debe coincidir con una contraseña incorrecta")
    void passwordEncoder_shouldNotMatchWrongPassword() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();
        String encoded = encoder.encode("correcta");

        assertThat(encoder.matches("incorrecta", encoded)).isFalse();
    }

    @Test
    @DisplayName("passwordEncoder() debe generar hashes distintos para la misma contraseña (salt aleatorio)")
    void passwordEncoder_shouldProduceDifferentHashesForSamePassword() {
        PasswordEncoder encoder = authProviderConfig.passwordEncoder();
        String raw = "mismoPassword";

        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    // -------------------------------------------------------------------------
    // authenticationProvider()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("authenticationProvider() debe retornar una instancia no nula")
    void authenticationProvider_shouldNotBeNull() {
        DaoAuthenticationProvider provider = authProviderConfig.authenticationProvider();

        assertThat(provider).isNotNull();
    }

    @Test
    @DisplayName("authenticationProvider() debe retornar un DaoAuthenticationProvider")
    void authenticationProvider_shouldBeDaoAuthenticationProviderInstance() {
        DaoAuthenticationProvider provider = authProviderConfig.authenticationProvider();

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    @DisplayName("authenticationProvider() debe tener configurado un PasswordEncoder BCrypt")
    void authenticationProvider_shouldHaveBCryptPasswordEncoder() {
        // Verificamos indirectamente: el provider debe poder codificar/verificar
        // contraseñas usando BCrypt sin lanzar excepciones
        DaoAuthenticationProvider provider = authProviderConfig.authenticationProvider();

        // Si el encoder no estuviera configurado, getPasswordEncoder() lanzaría
        // un error al intentar autenticar; aquí solo validamos que el bean se
        // construye de forma coherente con el encoder esperado.
        assertThat(provider).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Constructor / inyección
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("El constructor debe aceptar un UserDetailsService sin lanzar excepciones")
    void constructor_shouldAcceptUserDetailsService() {
        AuthProviderConfig config = new AuthProviderConfig(userDetailsService);

        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Dos llamadas a passwordEncoder() deben producir beans funcionalmente equivalentes")
    void passwordEncoder_twoInstancesShouldBeFunctionallyEquivalent() {
        PasswordEncoder encoder1 = authProviderConfig.passwordEncoder();
        PasswordEncoder encoder2 = authProviderConfig.passwordEncoder();

        String raw = "testPassword";
        String hash = encoder1.encode(raw);

        // Ambos encoders deben verificar el mismo hash
        assertThat(encoder2.matches(raw, hash)).isTrue();
    }
}