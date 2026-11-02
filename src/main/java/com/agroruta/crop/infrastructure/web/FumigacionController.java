package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.domain.Fumigacion;
import com.agroruta.crop.infrastructure.web.dto.FumigacionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@ownership.siembraPropia(#req.siembraId, authentication)")
    public ResponseEntity<Fumigacion> registrar(@RequestBody FumigacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                fumigacionUseCase.registrarFumigacion(
                        req.getFecha(),
                        req.getProducto(),
                        req.getAgriculturalInputId(), // nullable
                        req.getDosis(),
                        req.getUnidadMedida(),
                        req.getAreaAplicada(),
                        req.getObservaciones(),
                        req.getSiembraId()
                )
        );
    }

    @GetMapping("/siembra/{siembraId}")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<List<Fumigacion>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(fumigacionUseCase.listarFumigacionesPorSiembra(siembraId));
    }
}