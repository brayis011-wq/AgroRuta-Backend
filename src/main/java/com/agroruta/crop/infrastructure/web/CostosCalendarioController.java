package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.CostosCalendarioService;
import com.agroruta.crop.domain.CostosSiembra;
import com.agroruta.crop.domain.EventoCalendario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CostosCalendarioController {

    private final CostosCalendarioService service;

    public CostosCalendarioController(CostosCalendarioService service) {
        this.service = service;
    }

    /** Mano de obra (jornales), insumos, otros costos, ingresos y ganancia de la temporada */
    @GetMapping("/plantings/{id}/costs")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<CostosSiembra> costos(@PathVariable Long id) {
        return ResponseEntity.ok(service.costos(id));
    }

    /** Eventos de la siembra entre dos fechas (por defecto 30 días atrás y 60 adelante) */
    @GetMapping("/plantings/{id}/calendar")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<List<EventoCalendario>> calendarioSiembra(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.calendarioSiembra(id, desde, hasta));
    }

    /** Eventos de todas las siembras de la finca entre dos fechas */
    @GetMapping("/farms/{id}/calendar")
    @PreAuthorize("@ownership.fincaPropia(#id, authentication)")
    public ResponseEntity<List<EventoCalendario>> calendarioFinca(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.calendarioFinca(id, desde, hasta));
    }
}
