package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.AvisosPushUseCase;
import com.agroruta.notification.domain.AvisoPush;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.user.domain.Dispositivo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Avisos push de quien inició sesión (cualquier rol): activar, desactivar y probar en este dispositivo. */
@RestController
@RequestMapping("/api/notifications/push")
public class PushController {

    public record SuscripcionRequest(@NotBlank @Size(max = 700) String endpoint,
                                     @NotBlank @Size(max = 120) String p256dh,
                                     @NotBlank @Size(max = 40) String auth) {}

    public record EndpointRequest(@NotBlank @Size(max = 700) String endpoint) {}

    private final AvisosPushUseCase push;

    public PushController(AvisosPushUseCase push) {
        this.push = push;
    }

    /** La clave pública del servidor, que el navegador pide para suscribirse */
    @GetMapping("/key")
    public ResponseEntity<Map<String, String>> clave() {
        return ResponseEntity.ok(Map.of("clave", push.clavePublica()));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> suscribir(@Valid @RequestBody SuscripcionRequest req, Authentication auth,
                                          @RequestHeader(value = "User-Agent", required = false) String navegador) {
        push.suscribir(CurrentUser.id(auth), req.endpoint(), req.p256dh(), req.auth(), Dispositivo.describir(navegador));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> desuscribir(@Valid @RequestBody EndpointRequest req, Authentication auth) {
        push.desuscribir(CurrentUser.id(auth), req.endpoint());
        return ResponseEntity.noContent().build();
    }

    /** Manda un aviso de prueba a los dispositivos de la persona, para confirmar que llegan */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Integer>> probar(Authentication auth) {
        int enviados = push.enviar(CurrentUser.id(auth), new AvisoPush("AgroRuta",
                "Los avisos están activos en este dispositivo.", "/", "prueba"));
        return ResponseEntity.ok(Map.of("enviados", enviados));
    }
}
