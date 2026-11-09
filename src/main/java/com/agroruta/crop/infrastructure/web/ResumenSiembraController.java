package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ResumenSiembraService;
import com.agroruta.crop.domain.ResumenSiembra;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plantings")
public class ResumenSiembraController {

    private final ResumenSiembraService service;

    public ResumenSiembraController(ResumenSiembraService service) {
        this.service = service;
    }

    /** Rendimiento, cosechas por semana, carencia y reentrada vigentes y temporadas anteriores */
    @GetMapping("/{id}/summary")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<ResumenSiembra> resumen(@PathVariable Long id) {
        return ResponseEntity.ok(service.resumen(id));
    }
}
