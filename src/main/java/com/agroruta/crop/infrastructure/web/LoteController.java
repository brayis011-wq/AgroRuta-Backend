package com.agroruta.crop.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.infrastructure.web.dto.ActualizarGeometriaLoteRequest;
import com.agroruta.crop.infrastructure.web.dto.LoteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteUseCase loteUseCase;

    public LoteController(LoteUseCase loteUseCase) {
        this.loteUseCase = loteUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.fincaPropia(#req.fincaId, authentication)")
    public ResponseEntity<Lote> registrar(@Valid @RequestBody LoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                loteUseCase.registrarLote(req.getNombre(), req.getArea(), req.getFincaId())
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<Lote> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteUseCase.buscarLotePorId(id));
    }

    @GetMapping("/finca/{fincaId}")
    @PreAuthorize("@ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<List<Lote>> listarPorFinca(@PathVariable Long fincaId) {
        return ResponseEntity.ok(loteUseCase.listarLotesPorFinca(fincaId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        loteUseCase.eliminarLote(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/geometria")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<Lote> actualizarGeometria(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGeometriaLoteRequest request) {

        Lote lote = loteUseCase.actualizarGeometriaLote(
                id,
                request.coordenadas(),
                request.area(),
                request.centroideLat(),
                request.centroideLng()
        );
        return ResponseEntity.ok(lote);
    }
}