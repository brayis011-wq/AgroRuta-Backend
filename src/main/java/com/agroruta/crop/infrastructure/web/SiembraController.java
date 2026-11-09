package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.infrastructure.web.dto.FinalizarSiembraRequest;
import com.agroruta.crop.infrastructure.web.dto.SiembraRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/plantings")
public class SiembraController {

    private final SiembraUseCase siembraUseCase;

    public SiembraController(SiembraUseCase siembraUseCase) {
        this.siembraUseCase = siembraUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.lotePropio(#req.loteId, authentication)")
    public ResponseEntity<SiembraResponse> registrar(@Valid @RequestBody SiembraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SiembraResponse.from(siembraUseCase.registrarSiembra(
                        req.getFechaSiembra(), req.getCantidadPlantas(),
                        req.getVariedadId(), req.getLoteId()
                )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<SiembraResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.buscarSiembraPorId(id)));
    }

    @GetMapping("/plot/{loteId}")
    @PreAuthorize("@ownership.lotePropio(#loteId, authentication)")
    public ResponseEntity<SiembraResponse> buscarPorLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.buscarSiembraPorLote(loteId)));
    }

    /** Todas las temporadas del lote, la más reciente primero */
    @GetMapping("/plot/{loteId}/history")
    @PreAuthorize("@ownership.lotePropio(#loteId, authentication)")
    public ResponseEntity<List<SiembraResponse>> historialPorLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(siembraUseCase.listarHistorialPorLote(loteId).stream().map(SiembraResponse::from).toList());
    }

    /** Corrige fecha, plantas o variedad (el lote del cuerpo se ignora) */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<SiembraResponse> actualizar(@PathVariable Long id, @Valid @RequestBody SiembraRequest req) {
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.actualizarSiembra(
                id, req.getFechaSiembra(), req.getCantidadPlantas(), req.getVariedadId())));
    }

    /** ?fecha=2026-11-04 opcional (por defecto hoy) */
    @PatchMapping("/{id}/advance-stage")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<SiembraResponse> avanzarEtapa(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.avanzarEtapa(id, fecha)));
    }

    @PatchMapping("/{id}/revert-stage")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<SiembraResponse> retrocederEtapa(@PathVariable Long id) {
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.retrocederEtapa(id)));
    }

    @PatchMapping("/{id}/finish")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<SiembraResponse> finalizar(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) FinalizarSiembraRequest req) {
        LocalDate fecha = req != null ? req.fecha() : null;
        boolean descanso = req != null && req.dejarEnDescanso();
        return ResponseEntity.ok(SiembraResponse.from(siembraUseCase.finalizarSiembra(id, fecha, descanso)));
    }

    @GetMapping("/{id}/stages")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<List<EtapaSiembraResponse>> etapas(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.listarEtapas(id).stream().map(EtapaSiembraResponse::from).toList());
    }

    // Devuelve siembras de TODOS los usuarios: solo administrador
    @GetMapping("/status/{estado}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<SiembraResponse>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(siembraUseCase.listarSiembrasPorEstado(estado).stream().map(SiembraResponse::from).toList());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        siembraUseCase.eliminarSiembra(id);
        return ResponseEntity.noContent().build();
    }
}