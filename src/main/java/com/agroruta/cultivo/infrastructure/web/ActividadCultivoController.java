package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.cultivo.domain.ActividadCultivo;
import com.agroruta.cultivo.infrastructure.web.dto.ActividadRequest;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ActividadCultivo> registrar(@RequestBody ActividadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                actividadUseCase.registrarActividad(
                        req.getTipo(), req.getDescripcion(),
                        req.getFecha(), req.getSiembraId()
                )
        );
    }

    @GetMapping("/siembra/{siembraId}")
    public ResponseEntity<List<ActividadCultivo>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(actividadUseCase.listarActividadesPorSiembra(siembraId));
    }
}