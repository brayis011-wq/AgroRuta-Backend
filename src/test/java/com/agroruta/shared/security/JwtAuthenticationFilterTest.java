package com.agroruta.shared.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @InjectMocks private JwtAuthenticationFilter filter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private static final String TOKEN  = "header.payload.signature";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String EMAIL  = "carlos@agroruta.com";

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = User.builder()
                .username(EMAIL)
                .password("hashed-pwd")
                .authorities(Collections.emptyList())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sin cabecera Authorization — pasa directo al siguiente filtro
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Sin cabecera Authorization")
    class SinCabecera {

        @Test
        @DisplayName("si no hay cabecera, pasa al siguiente filtro sin tocar el contexto")
        void sinCabecera_pasaAlSiguienteFiltro() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("si no hay cabecera, no se llama a JwtService")
        void sinCabecera_noLlamaAJwtService() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verifyNoInteractions(jwtService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cabecera sin prefijo "Bearer "
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cabecera sin prefijo Bearer")
    class SinPrefijosBearer {

        @Test
        @DisplayName("cabecera Basic auth pasa al siguiente filtro")
        void cabeceraBasic_pasaAlSiguiente() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("cabecera sin prefijo no llama a JwtService")
        void sinPrefijo_noLlamaAJwtService() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("token-sin-prefijo");

            filter.doFilterInternal(request, response, filterChain);

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("cadena vacía en Authorization pasa al siguiente filtro")
        void cadenaVacia_pasaAlSiguiente() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Token válido — autenticación exitosa
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token válido")
    class TokenValido {

        @BeforeEach
        void stubearTokenValido() {
            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);
        }

        @Test
        @DisplayName("establece la autenticación en el SecurityContext")
        void estableceAutenticacionEnContexto() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("el principal del contexto es el UserDetails cargado")
        void principalEsElUserDetailsCargado() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        }

        @Test
        @DisplayName("pasa al siguiente filtro después de autenticar")
        void pasaAlSiguienteFiltroTrasAutenticar() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("carga el usuario desde UserDetailsService exactamente una vez")
        void cargaUsuarioUnaVez() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(userDetailsService, times(1)).loadUserByUsername(EMAIL);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Token válido pero isTokenValid devuelve false
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token que no pasa isTokenValid")
    class TokenNoValido {

        @Test
        @DisplayName("no establece autenticación en el contexto si isTokenValid es false")
        void noAutenticaSiIsTokenValidEsFalse() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("pasa al siguiente filtro aunque isTokenValid sea false")
        void pasaAlSiguienteFiltroAunqueTokenNoValido() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Usuario ya autenticado en el contexto
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Usuario ya autenticado")
    class UsuarioYaAutenticado {

        @Test
        @DisplayName("si ya hay autenticación en el contexto, no llama a UserDetailsService")
        void noLlamaAUserDetailsServiceSiYaAutenticado() throws Exception {
            // Pre-cargar autenticación en el contexto
            var authExistente = new org.springframework.security.authentication
                    .UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authExistente);

            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);

            filter.doFilterInternal(request, response, filterChain);

            verifyNoInteractions(userDetailsService);
            verify(filterChain).doFilter(request, response);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Token expirado — ExpiredJwtException
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token expirado (ExpiredJwtException)")
    class TokenExpirado {

        private StringWriter responseBody;

        @BeforeEach
        void stubearRespuesta() throws Exception {
            responseBody = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN))
                    .thenThrow(new ExpiredJwtException(null, null, "Token expirado"));
        }

        @Test
        @DisplayName("responde con HTTP 401")
        void respondeConStatus401() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("el body contiene el mensaje de token expirado")
        void bodyContieneTokenExpirado() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(responseBody.toString()).contains("Token expirado");
        }

        @Test
        @DisplayName("el body tiene Content-Type application/json")
        void contentTypeEsJson() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setContentType("application/json");
        }

        @Test
        @DisplayName("NO pasa al siguiente filtro de la cadena")
        void noPasaAlSiguienteFiltro() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verifyNoInteractions(filterChain);
        }

        @Test
        @DisplayName("el body contiene el errorCode AGR-401")
        void bodyContieneErrorCode() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(responseBody.toString()).contains("AGR-401");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Token inválido — JwtException (firma corrupta, malformado, etc.)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token inválido (JwtException)")
    class TokenInvalido {

        private StringWriter responseBody;

        @BeforeEach
        void stubearRespuesta() throws Exception {
            responseBody = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(responseBody));
            when(request.getHeader("Authorization")).thenReturn(BEARER);
            when(jwtService.extractUsername(TOKEN))
                    .thenThrow(new JwtException("Firma inválida"));
        }

        @Test
        @DisplayName("responde con HTTP 401")
        void respondeConStatus401() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("el body contiene el mensaje de token inválido")
        void bodyContieneTokenInvalido() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(responseBody.toString()).contains("Token inválido");
        }

        @Test
        @DisplayName("NO pasa al siguiente filtro de la cadena")
        void noPasaAlSiguienteFiltro() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            verifyNoInteractions(filterChain);
        }
    }
}