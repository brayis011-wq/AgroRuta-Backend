package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.LoteUseCase;
import com.agroruta.crop.domain.Lote;
import com.agroruta.crop.infrastructure.web.dto.ActualizarGeometriaLoteRequest;
import com.agroruta.crop.infrastructure.web.dto.CambiarEstadoLoteRequest;
import com.agroruta.crop.infrastructure.web.dto.LoteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plots")
public class LoteController {

    private final LoteUseCase loteUseCase;

    public LoteController(LoteUseCase loteUseCase) {
        this.loteUseCase = loteUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.fincaPropia(#req.fincaId, authentication)")
    public ResponseEntity<LoteResponse> registrar(@Valid @RequestBody LoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(LoteResponse.from(loteUseCase.registrarLote(req.getNombre(), req.getArea(), req.getFincaId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<LoteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(LoteResponse.from(loteUseCase.buscarLotePorId(id)));
    }

    @GetMapping("/farm/{fincaId}")
    @PreAuthorize("@ownership.fincaPropia(#fincaId, authentication)")
    public ResponseEntity<List<LoteResponse>> listarPorFinca(@PathVariable Long fincaId) {
        return ResponseEntity.ok(loteUseCase.listarLotesPorFinca(fincaId).stream().map(LoteResponse::from).toList());
    }

    /** Edita nombre y área (la finca del cuerpo se ignora: un lote no cambia de finca) */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<LoteResponse> actualizar(@PathVariable Long id, @Valid @RequestBody LoteRequest req) {
        return ResponseEntity.ok(LoteResponse.from(loteUseCase.actualizarLote(id, req.getNombre(), req.getArea())));
    }

    /** Disponible o en descanso, cuando el lote no tiene siembra en curso */
    @PatchMapping("/{id}/status")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<LoteResponse> cambiarEstado(@PathVariable Long id,
                                              @Valid @RequestBody CambiarEstadoLoteRequest req) {
        return ResponseEntity.ok(LoteResponse.from(loteUseCase.cambiarEstado(id, req.estado())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        loteUseCase.eliminarLote(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/geometry")
    @PreAuthorize("@ownership.lotePropio(#id, authentication)")
    public ResponseEntity<LoteResponse> actualizarGeometria(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarGeometriaLoteRequest request) {

        Lote lote = loteUseCase.actualizarGeometriaLote(
                id,
                request.coordenadas(),
                request.area(),
                request.centroideLat(),
                request.centroideLng()
        );
        return ResponseEntity.ok(LoteResponse.from(lote));
    }
}