package com.agroruta.user.infrastructure.web;

import com.agroruta.shared.exception.SesionInvalidaException;
import com.agroruta.shared.security.CookieSesion;
import com.agroruta.user.application.AuthService;
import com.agroruta.user.application.ports.in.SesionUseCase;
import com.agroruta.user.domain.Usuario;
import com.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Pruebas Unitarias")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private SesionUseCase sesiones;

    private AuthController authController;
    private MockHttpServletResponse response;

    private AuthResponse authResponseBase;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, sesiones, new CookieSesion(true, 30));
        response = new MockHttpServletResponse();
        authResponseBase = new AuthResponse(
                1L,
                "jwt.token.ejemplo",
                "juan@agroruta.com",
                "Juan Pérez",
                "Agricultor"
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    //  registro()
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("registro()")
    class Registro {

        @Test
        @DisplayName("Debe retornar 200 y el AuthResponse cuando el registro es exitoso")
        void debeRetornar200ConAuthResponseAlRegistrar() {
            RegisterRequest request = new RegisterRequest();
            request.setNombre("Juan Pérez");
            request.setEmail("juan@agroruta.com");
            request.setPassword("password123");
            request.setRol("Agricultor");

            when(authService.registrar(any(RegisterRequest.class))).thenReturn(authResponseBase);

            ResponseEntity<AuthResponse> respuesta = authController.registro(request);

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,            respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals(1L,                       respuesta.getBody().getId());
            assertEquals("jwt.token.ejemplo",      respuesta.getBody().getToken());
            assertEquals("juan@agroruta.com",      respuesta.getBody().getEmail());
            assertEquals("Juan Pérez",             respuesta.getBody().getNombre());
            assertEquals("Agricultor",             respuesta.getBody().getRol());
            verify(authService, times(1)).registrar(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Debe delegar al authService el request de registro completo")
        void debeDelegarAlServiceElRequestDeRegistro() {
            RegisterRequest request = new RegisterRequest();
            request.setNombre("Ana López");
            request.setEmail("ana@agroruta.com");
            request.setPassword("pass456");
            request.setRol("Administrador");

            AuthResponse otraRespuesta = new AuthResponse(
                    2L, "otro.jwt.token", "ana@agroruta.com", "Ana López", "Administrador"
            );

            when(authService.registrar(any(RegisterRequest.class))).thenReturn(otraRespuesta);

            ResponseEntity<AuthResponse> respuesta = authController.registro(request);

            assertEquals(HttpStatus.OK,       respuesta.getStatusCode());
            assertEquals("ana@agroruta.com",  respuesta.getBody().getEmail());
            assertEquals("Administrador",     respuesta.getBody().getRol());
            verify(authService, times(1)).registrar(any(RegisterRequest.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  login()
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Debe retornar 200 y el AuthResponse cuando el login es exitoso")
        void debeRetornar200ConAuthResponseAlLogin() {
            LoginRequest request = new LoginRequest();
            request.setEmail("juan@agroruta.com");
            request.setPassword("password123");

            when(authService.login(any(LoginRequest.class), any())).thenReturn(authResponseBase);

            ResponseEntity<AuthResponse> respuesta = authController.login(request, null, response);

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,        respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals(1L,                   respuesta.getBody().getId());
            assertEquals("jwt.token.ejemplo",  respuesta.getBody().getToken());
            assertEquals("juan@agroruta.com",  respuesta.getBody().getEmail());
            assertEquals("Juan Pérez",         respuesta.getBody().getNombre());
            assertEquals("Agricultor",         respuesta.getBody().getRol());
            verify(authService, times(1)).login(any(LoginRequest.class), any());
        }

        @Test
        @DisplayName("Debe delegar al authService el request de login completo")
        void debeDelegarAlServiceElRequestDeLogin() {
            LoginRequest request = new LoginRequest();
            request.setEmail("ana@agroruta.com");
            request.setPassword("pass456");

            AuthResponse otraRespuesta = new AuthResponse(
                    2L, "otro.jwt.token", "ana@agroruta.com", "Ana López", "Administrador"
            );

            when(authService.login(any(LoginRequest.class), any())).thenReturn(otraRespuesta);

            ResponseEntity<AuthResponse> respuesta = authController.login(request, null, response);

            assertEquals(HttpStatus.OK,      respuesta.getStatusCode());
            assertEquals("otro.jwt.token",   respuesta.getBody().getToken());
            assertEquals("ana@agroruta.com", respuesta.getBody().getEmail());
            verify(authService, times(1)).login(any(LoginRequest.class), any());
        }
    }

    @Test
    @DisplayName("verificar() confirma el correo y devuelve la sesión")
    void verificarDevuelveSesion() {
        when(authService.verificarCorreo("tok")).thenReturn(authResponseBase);
        var r = authController.verificar(new com.agroruta.user.infrastructure.web.dto.VerificarRequest("tok"), response);
        org.junit.jupiter.api.Assertions.assertEquals(200, r.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals(authResponseBase, r.getBody());
    }

    @Test
    @DisplayName("reenviarVerificacion() responde lo mismo exista o no la cuenta")
    void reenviarVerificacion() {
        var r = authController.reenviarVerificacion(
                new com.agroruta.user.infrastructure.web.dto.ReenviarVerificacionRequest("ana@x.com"));
        org.junit.jupiter.api.Assertions.assertEquals(200, r.getStatusCode().value());
        verify(authService).reenviarVerificacion("ana@x.com");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Cookie de sesión (refresh token)
    // ══════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cookie de sesión")
    class CookieDeSesion {

        @Test
        @DisplayName("login abre una sesión y la manda en una cookie httpOnly, Strict, solo para /api/auth")
        void loginEscribeLaCookie() {
            when(authService.login(any(LoginRequest.class), any())).thenReturn(authResponseBase);
            when(sesiones.abrir(1L)).thenReturn("refresh-1");

            authController.login(new LoginRequest(), null, response);

            String cookie = response.getHeader("Set-Cookie");
            assertNotNull(cookie);
            assertTrue(cookie.startsWith("agroruta_sesion=refresh-1"));
            assertTrue(cookie.contains("HttpOnly"));
            assertTrue(cookie.contains("Secure"));
            assertTrue(cookie.contains("SameSite=Strict"));
            assertTrue(cookie.contains("Path=/api/auth"));
        }

        @Test
        @DisplayName("entrar con el código recuerda el dispositivo 30 días en otra cookie httpOnly")
        void conCodigoRecuerdaElDispositivo() {
            LoginRequest request = new LoginRequest();
            request.setCodigo("123456");
            when(authService.login(request, null)).thenReturn(authResponseBase);
            when(sesiones.abrir(1L)).thenReturn("refresh-1");
            when(authService.recordarDispositivo(1L)).thenReturn(java.util.Optional.of("1.999.firma"));

            authController.login(request, null, response);

            String cookie = response.getHeaders("Set-Cookie").stream()
                    .filter(c -> c.startsWith("agroruta_dispositivo=")).findFirst().orElseThrow();
            assertTrue(cookie.startsWith("agroruta_dispositivo=1.999.firma"));
            assertTrue(cookie.contains("Max-Age=2592000"), "30 días");
            assertTrue(cookie.contains("HttpOnly"));
            assertTrue(cookie.contains("SameSite=Strict"));
            assertTrue(cookie.contains("Path=/api/auth"));
        }

        @Test
        @DisplayName("si desmarca la casilla no se recuerda (y se borra una marca anterior)")
        void sinRecordar() {
            LoginRequest request = new LoginRequest();
            request.setCodigo("123456");
            request.setRecordarDispositivo(false);
            when(authService.login(request, null)).thenReturn(authResponseBase);
            when(sesiones.abrir(1L)).thenReturn("refresh-1");

            authController.login(request, null, response);

            verify(authService, never()).recordarDispositivo(any());
            assertTrue(response.getHeaders("Set-Cookie").stream()
                    .anyMatch(c -> c.startsWith("agroruta_dispositivo=;") && c.contains("Max-Age=0")));
        }

        @Test
        @DisplayName("entrando sin código (dispositivo ya recordado o sin dos pasos) no se renueva la marca")
        void sinCodigoNoRenueva() {
            when(authService.login(any(LoginRequest.class), eq("1.999.firma"))).thenReturn(authResponseBase);
            when(sesiones.abrir(1L)).thenReturn("refresh-1");

            authController.login(new LoginRequest(), "1.999.firma", response);

            verify(authService).login(any(LoginRequest.class), eq("1.999.firma"));
            verify(authService, never()).recordarDispositivo(any());
            assertTrue(response.getHeaders("Set-Cookie").stream().noneMatch(c -> c.startsWith("agroruta_dispositivo")));
        }

        @Test
        @DisplayName("registro no abre sesión: la persona entra después por el login")
        void registroPendienteSinCookie() {
            when(authService.registrar(any(RegisterRequest.class))).thenReturn(authResponseBase);

            authController.registro(new RegisterRequest());

            assertNull(response.getHeader("Set-Cookie"));
            verifyNoInteractions(sesiones);
        }

        @Test
        @DisplayName("refresh cambia la cookie y devuelve un token de acceso nuevo")
        void refreshRenueva() {
            Usuario usuario = new Usuario();
            when(sesiones.renovar("viejo")).thenReturn(new SesionUseCase.Renovacion(usuario, "nuevo"));
            when(authService.respuestaDeSesion(usuario)).thenReturn(authResponseBase);

            ResponseEntity<AuthResponse> r = authController.renovar("viejo", response);

            assertEquals(authResponseBase, r.getBody());
            assertTrue(response.getHeader("Set-Cookie").startsWith("agroruta_sesion=nuevo"));
        }

        @Test
        @DisplayName("refresh con una cookie que no sirve la borra y responde 401")
        void refreshInvalidoBorraLaCookie() {
            when(sesiones.renovar("robado")).thenThrow(new SesionInvalidaException("Tu sesión terminó."));

            assertThrows(SesionInvalidaException.class, () -> authController.renovar("robado", response));
            assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
        }

        @Test
        @DisplayName("logout cierra la sesión y borra la cookie")
        void logoutCierra() {
            ResponseEntity<Void> r = authController.logout("tok", response);

            assertEquals(HttpStatus.NO_CONTENT, r.getStatusCode());
            verify(sesiones).cerrar("tok");
            assertTrue(response.getHeader("Set-Cookie").contains("Max-Age=0"));
        }
    }
}
