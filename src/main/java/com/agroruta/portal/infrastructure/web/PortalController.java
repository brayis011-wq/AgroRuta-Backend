package com.agroruta.portal.infrastructure.web;

import com.agroruta.portal.application.ports.in.PortalTrabajadorUseCase;
import com.agroruta.portal.infrastructure.web.dto.EstadoPortalResponse;
import com.agroruta.portal.infrastructure.web.dto.MiJornalResponse;
import com.agroruta.portal.infrastructure.web.dto.MiNominaResponse;
import com.agroruta.portal.infrastructure.web.dto.MiPagoResponse;
import com.agroruta.portal.infrastructure.web.dto.SolicitudAccesoRequest;
import com.agroruta.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Portal del trabajador: solo ve sus propios datos y solo cuando su empleador lo aprobó. */
@RestController
@RequestMapping("/api/portal")
@PreAuthorize("hasRole('Trabajador')")
public class PortalController {

    private final PortalTrabajadorUseCase portal;

    public PortalController(PortalTrabajadorUseCase portal) {
        this.portal = portal;
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoPortalResponse> estado(Authentication auth) {
        return ResponseEntity.ok(EstadoPortalResponse.from(portal.estado(CurrentUser.id(auth))));
    }

    @PostMapping("/solicitud")
    public ResponseEntity<EstadoPortalResponse> solicitar(@Valid @RequestBody SolicitudAccesoRequest req,
                                                          Authentication auth) {
        return ResponseEntity.ok(EstadoPortalResponse.from(portal.solicitar(CurrentUser.id(auth), req.cedula())));
    }

    @GetMapping("/jornales")
    public ResponseEntity<List<MiJornalResponse>> jornales(Authentication auth) {
        return ResponseEntity.ok(portal.misJornales(CurrentUser.id(auth)).stream().map(MiJornalResponse::from).toList());
    }

    @GetMapping("/nominas")
    public ResponseEntity<List<MiNominaResponse>> nominas(Authentication auth) {
        return ResponseEntity.ok(portal.misNominas(CurrentUser.id(auth)).stream().map(MiNominaResponse::from).toList());
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<MiPagoResponse>> pagos(Authentication auth) {
        return ResponseEntity.ok(portal.misPagos(CurrentUser.id(auth)).stream().map(MiPagoResponse::from).toList());
    }
}
