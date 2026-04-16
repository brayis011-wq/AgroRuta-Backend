package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.CosechaUseCase;
import com.agroruta.cultivo.domain.Cosecha;
import com.agroruta.cultivo.infrastructure.web.dto.CosechaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cosechas")
public class CosechaController {

    private final CosechaUseCase cosechaUseCase;

    public CosechaController(CosechaUseCase cosechaUseCase) {
        this.cosechaUseCase = cosechaUseCase;
    }

    @PostMapping
    public ResponseEntity<Cosecha> registrar(@RequestBody CosechaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                cosechaUseCase.registrarCosecha(
                        req.getFecha(), req.getCantidadKg(),
                        req.getCalidad(), req.getObservaciones(), req.getSiembraId()
                )
        );
    }

    @GetMapping("/siembra/{siembraId}")
    public ResponseEntity<List<Cosecha>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(cosechaUseCase.listarCosechasPorSiembra(siembraId));
    }

    @GetMapping("/siembra/{siembraId}/total-kg")
    public ResponseEntity<Double> totalKgCosechado(@PathVariable Long siembraId) {
        return ResponseEntity.ok(cosechaUseCase.totalKgCosechado(siembraId));
    }
}