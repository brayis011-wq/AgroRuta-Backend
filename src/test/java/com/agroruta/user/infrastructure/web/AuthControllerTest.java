package com.agroruta.user.infrastructure.web;

import com.agroruta.user.application.AuthService;
import com.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Pruebas Unitarias")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse authResponseBase;

    @BeforeEach
    void setUp() {
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

            when(authService.login(any(LoginRequest.class))).thenReturn(authResponseBase);

            ResponseEntity<AuthResponse> respuesta = authController.login(request);

            assertNotNull(respuesta);
            assertEquals(HttpStatus.OK,        respuesta.getStatusCode());
            assertNotNull(respuesta.getBody());
            assertEquals(1L,                   respuesta.getBody().getId());
            assertEquals("jwt.token.ejemplo",  respuesta.getBody().getToken());
            assertEquals("juan@agroruta.com",  respuesta.getBody().getEmail());
            assertEquals("Juan Pérez",         respuesta.getBody().getNombre());
            assertEquals("Agricultor",         respuesta.getBody().getRol());
            verify(authService, times(1)).login(any(LoginRequest.class));
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

            when(authService.login(any(LoginRequest.class))).thenReturn(otraRespuesta);

            ResponseEntity<AuthResponse> respuesta = authController.login(request);

            assertEquals(HttpStatus.OK,      respuesta.getStatusCode());
            assertEquals("otro.jwt.token",   respuesta.getBody().getToken());
            assertEquals("ana@agroruta.com", respuesta.getBody().getEmail());
            verify(authService, times(1)).login(any(LoginRequest.class));
        }
    }
}