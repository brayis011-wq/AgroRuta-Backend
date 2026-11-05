package com.agroruta.notification.infrastructure.web;

import com.agroruta.notification.application.ports.in.PreferenciasCorreoUseCase;
import com.agroruta.notification.infrastructure.web.dto.PreferenciasCorreoDto;
import com.agroruta.shared.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Los correos opcionales que quiere recibir quien inició sesión. */
@RestController
@RequestMapping("/api/notificaciones/preferencias")
public class PreferenciasCorreoController {

    private final PreferenciasCorreoUseCase preferencias;

    public PreferenciasCorreoController(PreferenciasCorreoUseCase preferencias) {
        this.preferencias = preferencias;
    }

    @GetMapping
    public ResponseEntity<PreferenciasCorreoDto> obtener(Authentication auth) {
        return ResponseEntity.ok(PreferenciasCorreoDto.de(preferencias.obtener(CurrentUser.id(auth))));
    }

    @PutMapping
    public ResponseEntity<PreferenciasCorreoDto> guardar(Authentication auth, @RequestBody PreferenciasCorreoDto body) {
        return ResponseEntity.ok(PreferenciasCorreoDto.de(
                preferencias.guardar(CurrentUser.id(auth), body.alertasCultivo(), body.avisosTrabajadores())));
    }
}
