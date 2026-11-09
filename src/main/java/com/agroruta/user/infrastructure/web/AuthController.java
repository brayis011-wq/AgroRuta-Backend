package com.agroruta.user.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.user.application.AuthService;
import com.agroruta.user.infrastructure.web.dto.AuthResponse;
import com.agroruta.user.infrastructure.web.dto.LoginRequest;
import com.agroruta.user.infrastructure.web.dto.RegisterRequest;
import com.agroruta.user.infrastructure.web.dto.ReenviarVerificacionRequest;
import com.agroruta.user.infrastructure.web.dto.VerificarRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Enlace del correo de confirmación: confirma la cuenta e inicia sesión */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verificar(@Valid @RequestBody VerificarRequest request) {
        return ResponseEntity.ok(authService.verificarCorreo(request.token()));
    }

    /** Siempre responde lo mismo, exista o no la cuenta */
    @PostMapping("/resend-verification")
    public ResponseEntity<java.util.Map<String, String>> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionRequest request) {
        authService.reenviarVerificacion(request.email());
        return ResponseEntity.ok(java.util.Map.of("mensaje",
                "Si la cuenta existe y falta confirmarla, te enviamos un enlace nuevo."));
    }
}
