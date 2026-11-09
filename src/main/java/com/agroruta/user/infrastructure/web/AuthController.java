package com.agroruta.user.infrastructure.web;

import com.agroruta.shared.security.CookieSesion;
import com.agroruta.user.application.AuthService;
import com.agroruta.user.application.ports.in.SesionUseCase;
import com.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.ReenviarVerificacionRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import com.agroruta.user.infrastructure.web.dto.VerificarRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Inicio y cierre de sesión. El token de acceso (JWT, 15 min) va en el cuerpo y el frontend lo
 * guarda solo en memoria; el token de sesión (30 días) va en una cookie httpOnly y sirve para
 * pedir tokens de acceso nuevos en /refresh.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SesionUseCase sesiones;
    private final CookieSesion cookie;

    public AuthController(AuthService authService, SesionUseCase sesiones, CookieSesion cookie) {
        this.authService = authService;
        this.sesiones = sesiones;
        this.cookie = cookie;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        return conSesion(authService.registrar(request), response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        return conSesion(authService.login(request), response);
    }

    /** Enlace del correo de confirmación: confirma la cuenta e inicia sesión */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verificar(@Valid @RequestBody VerificarRequest request,
                                                  HttpServletResponse response) {
        return conSesion(authService.verificarCorreo(request.token()), response);
    }

    /** Cambia la cookie de sesión por una nueva y entrega un token de acceso nuevo. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> renovar(@CookieValue(name = CookieSesion.NOMBRE, required = false) String token,
                                                HttpServletResponse response) {
        SesionUseCase.Renovacion r;
        try {
            r = sesiones.renovar(token);
        } catch (RuntimeException e) {
            cookie.borrar(response);
            throw e;
        }
        cookie.escribir(response, r.token());
        return ResponseEntity.ok(authService.respuestaDeSesion(r.usuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = CookieSesion.NOMBRE, required = false) String token,
                                       HttpServletResponse response) {
        sesiones.cerrar(token);
        cookie.borrar(response);
        return ResponseEntity.noContent().build();
    }

    /** Siempre responde lo mismo, exista o no la cuenta */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        authService.reenviarVerificacion(request.email());
        return ResponseEntity.ok(Map.of("mensaje",
                "Si la cuenta existe y falta confirmarla, te enviamos un enlace nuevo."));
    }

    /** Si la respuesta trae token de acceso (la cuenta ya puede entrar), abre la sesión renovable. */
    private ResponseEntity<AuthResponse> conSesion(AuthResponse auth, HttpServletResponse response) {
        if (auth.getToken() != null) {
            cookie.escribir(response, sesiones.abrir(auth.getId()));
        }
        return ResponseEntity.ok(auth);
    }
}
