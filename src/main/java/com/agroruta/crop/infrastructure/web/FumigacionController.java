package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.FumigacionUseCase;
import com.agroruta.crop.infrastructure.web.dto.FumigacionRequest;
import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sprayings")
public class FumigacionController {

    private final FumigacionUseCase fumigacionUseCase;

    public FumigacionController(FumigacionUseCase fumigacionUseCase) {
        this.fumigacionUseCase = fumigacionUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.siembraPropia(#req.siembraId, authentication)")
    public ResponseEntity<FumigacionResponse> registrar(@Valid @RequestBody FumigacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FumigacionResponse.from(fumigacionUseCase.registrarFumigacion(
                        req.fecha(),
                        req.producto(),
                        req.agriculturalInputId(), // nullable
                        req.dosis(),
                        req.unidadMedida(),
                        req.areaAplicada(),
                        req.observaciones(),
                        req.siembraId(),
                        req.restricciones())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.fumigacionPropia(#id, authentication)")
    public ResponseEntity<FumigacionResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(FumigacionResponse.from(fumigacionUseCase.buscarFumigacionPorId(id)));
    }

    /** La siembra del cuerpo se ignora */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.fumigacionPropia(#id, authentication)")
    public ResponseEntity<FumigacionResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody FumigacionRequest req) {
        return ResponseEntity.ok(FumigacionResponse.from(fumigacionUseCase.actualizarFumigacion(
                id,
                req.fecha(),
                req.producto(),
                req.agriculturalInputId(),
                req.dosis(),
                req.unidadMedida(),
                req.areaAplicada(),
                req.observaciones(),
                req.restricciones())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.fumigacionPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        fumigacionUseCase.eliminarFumigacion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/planting/{siembraId}")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<List<FumigacionResponse>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(fumigacionUseCase.listarFumigacionesPorSiembra(siembraId).stream()
                .map(FumigacionResponse::from)
                .toList());
    }
}
