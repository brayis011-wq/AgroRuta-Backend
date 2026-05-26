package com.agroruta.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig - Tests unitarios")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthFilter;

    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthFilter, authenticationProvider);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("El constructor debe crear la instancia correctamente")
    void constructor_shouldCreateInstance() {
        SecurityConfig config = new SecurityConfig(jwtAuthFilter, authenticationProvider);

        assertThat(config).isNotNull();
    }

    // -------------------------------------------------------------------------
    // authenticationManager()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("authenticationManager() debe retornar el manager de AuthenticationConfiguration")
    void authenticationManager_shouldReturnManagerFromConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager())
                .thenReturn(authenticationManager);

        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);

        assertThat(result).isNotNull();
        assertThat(result).isSameAs(authenticationManager);
    }

    @Test
    @DisplayName("authenticationManager() debe delegar exactamente una vez en AuthenticationConfiguration")
    void authenticationManager_shouldDelegateOnceToConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager())
                .thenReturn(authenticationManager);

        securityConfig.authenticationManager(authenticationConfiguration);

        verify(authenticationConfiguration, times(1)).getAuthenticationManager();
    }

    @Test
    @DisplayName("authenticationManager() debe propagar la excepción si AuthenticationConfiguration falla")
    void authenticationManager_shouldPropagateException() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager())
                .thenThrow(new RuntimeException("Error al obtener AuthenticationManager"));

        assertThatThrownBy(() -> securityConfig.authenticationManager(authenticationConfiguration))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al obtener AuthenticationManager");
    }
}