package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.RecuperarContrasenaUseCase;
import com.agroruta.notification.infrastructure.web.dto.RecuperarRequest;
import com.agroruta.notification.infrastructure.web.dto.RestablecerRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class RecuperarContrasenaController {

    private final RecuperarContrasenaUseCase recuperar;

    public RecuperarContrasenaController(RecuperarContrasenaUseCase recuperar) {
        this.recuperar = recuperar;
    }

    /** Siempre responde lo mismo, exista o no la cuenta, para no revelar qué correos están registrados. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> recuperar(@Valid @RequestBody RecuperarRequest request) {
        recuperar.solicitar(request.email());
        return ResponseEntity.ok(Map.of("mensaje",
                "Si el correo tiene una cuenta, te enviamos un enlace para crear una contraseña nueva."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody RestablecerRequest request) {
        recuperar.restablecer(request.token(), request.password());
        return ResponseEntity.ok(Map.of("mensaje", "Listo, ya puedes iniciar sesión con tu contraseña nueva."));
    }
}
