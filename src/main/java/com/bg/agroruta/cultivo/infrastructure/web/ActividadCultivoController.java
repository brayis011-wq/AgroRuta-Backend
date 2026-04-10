package com.bg.agroruta.cultivo.infrastructure.web;

import com.bg.agroruta.cultivo.application.ports.in.ActividadCultivoUseCase;
import com.bg.agroruta.cultivo.domain.ActividadCultivo;
import com.bg.agroruta.cultivo.domain.TipoActividad;
import com.bg.agroruta.cultivo.infrastructure.web.dto.ActividadRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ActividadCultivoController {

    private final ActividadCultivoUseCase actividadUseCase;

    public ActividadCultivoController(ActividadCultivoUseCase actividadUseCase) {
        this.actividadUseCase = actividadUseCase;
    }

    @PostMapping
    public ResponseEntity<ActividadCultivo> registrar(@RequestBody ActividadRequest request) {
        ActividadCultivo actividad = new ActividadCultivo(
                null,
                TipoActividad.valueOf(request.getTipo().toUpperCase()),
                request.getDescripcion(),
                request.getFecha(),
                request.getSiembraId()
        );
        return ResponseEntity.ok(actividadUseCase.registrarActividad(actividad));
    }

    @GetMapping("/siembra/{siembraId}")
    public ResponseEntity<List<ActividadCultivo>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(actividadUseCase.listarActividadesPorSiembra(siembraId));
    }
}