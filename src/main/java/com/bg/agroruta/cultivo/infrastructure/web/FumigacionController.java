package com.bg.agroruta.cultivo.infrastructure.web;

import com.bg.agroruta.cultivo.application.ports.in.FumigacionUseCase;
import com.bg.agroruta.cultivo.domain.Fumigacion;
import com.bg.agroruta.cultivo.domain.UnidadMedida;
import com.bg.agroruta.cultivo.infrastructure.web.dto.FumigacionRequest;
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
    public ResponseEntity<Fumigacion> registrar(@RequestBody FumigacionRequest request) {
        Fumigacion fumigacion = new Fumigacion(
                null,
                request.getFecha(),
                request.getProducto(),
                request.getDosis(),
                UnidadMedida.valueOf(request.getUnidadMedida().toUpperCase()),
                request.getAreaAplicada(),
                request.getObservaciones(),
                request.getSiembraId()
        );
        return ResponseEntity.ok(fumigacionUseCase.registrarFumigacion(fumigacion));
    }

    @GetMapping("/siembra/{siembraId}")
    public ResponseEntity<List<Fumigacion>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(fumigacionUseCase.listarFumigacionesPorSiembra(siembraId));
    }
}