package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.FumigacionUseCase;
import com.agroruta.cultivo.domain.Fumigacion;
import com.agroruta.cultivo.infrastructure.web.dto.FumigacionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fumigaciones")
public class FumigacionController {

    private final FumigacionUseCase fumigacionUseCase;

    public FumigacionController(FumigacionUseCase fumigacionUseCase) {
        this.fumigacionUseCase = fumigacionUseCase;
    }

    @PostMapping
    public ResponseEntity<Fumigacion> registrar(@RequestBody FumigacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                fumigacionUseCase.registrarFumigacion(
                        req.getFecha(), req.getProducto(), req.getDosis(),
                        req.getUnidadMedida(), req.getAreaAplicada(),
                        req.getObservaciones(), req.getSiembraId()
                )
        );
    }

    @GetMapping("/siembra/{siembraId}")
    public ResponseEntity<List<Fumigacion>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(fumigacionUseCase.listarFumigacionesPorSiembra(siembraId));
    }
}