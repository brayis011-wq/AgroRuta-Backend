package com.agroruta.crop.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.CosechaUseCase;
import com.agroruta.crop.domain.Cosecha;
import com.agroruta.crop.infrastructure.web.dto.CosechaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@ownership.siembraPropia(#req.siembraId, authentication)")
    public ResponseEntity<Cosecha> registrar(@Valid @RequestBody CosechaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                cosechaUseCase.registrarCosecha(
                        req.getFecha(), req.getCantidadKg(),
                        req.getCalidad(), req.getObservaciones(), req.getSiembraId(), req.getPrecioKg()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.cosechaPropia(#id, authentication)")
    public ResponseEntity<Cosecha> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(cosechaUseCase.buscarCosechaPorId(id));
    }

    /** La siembra del cuerpo se ignora: una cosecha no cambia de siembra */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.cosechaPropia(#id, authentication)")
    public ResponseEntity<Cosecha> actualizar(@PathVariable Long id, @Valid @RequestBody CosechaRequest req) {
        return ResponseEntity.ok(cosechaUseCase.actualizarCosecha(
                id, req.getFecha(), req.getCantidadKg(), req.getCalidad(), req.getObservaciones(), req.getPrecioKg()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.cosechaPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cosechaUseCase.eliminarCosecha(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/siembra/{siembraId}")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<List<Cosecha>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(cosechaUseCase.listarCosechasPorSiembra(siembraId));
    }

    @GetMapping("/siembra/{siembraId}/total-kg")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<Double> totalKgCosechado(@PathVariable Long siembraId) {
        return ResponseEntity.ok(cosechaUseCase.totalKgCosechado(siembraId));
    }
}