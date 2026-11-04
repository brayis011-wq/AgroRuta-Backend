package com.agroruta.crop.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.SiembraUseCase;
import com.agroruta.crop.domain.EtapaSiembra;
import com.agroruta.crop.domain.Siembra;
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
@RequestMapping("/api/siembras")
public class SiembraController {

    private final SiembraUseCase siembraUseCase;

    public SiembraController(SiembraUseCase siembraUseCase) {
        this.siembraUseCase = siembraUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.lotePropio(#req.loteId, authentication)")
    public ResponseEntity<Siembra> registrar(@Valid @RequestBody SiembraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                siembraUseCase.registrarSiembra(
                        req.getFechaSiembra(), req.getCantidadPlantas(),
                        req.getVariedadId(), req.getLoteId()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Siembra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.buscarSiembraPorId(id));
    }

    @GetMapping("/lote/{loteId}")
    @PreAuthorize("@ownership.lotePropio(#loteId, authentication)")
    public ResponseEntity<Siembra> buscarPorLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(siembraUseCase.buscarSiembraPorLote(loteId));
    }

    /** Todas las temporadas del lote, la más reciente primero */
    @GetMapping("/lote/{loteId}/historial")
    @PreAuthorize("@ownership.lotePropio(#loteId, authentication)")
    public ResponseEntity<List<Siembra>> historialPorLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(siembraUseCase.listarHistorialPorLote(loteId));
    }

    /** Corrige fecha, plantas o variedad (el lote del cuerpo se ignora) */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Siembra> actualizar(@PathVariable Long id, @Valid @RequestBody SiembraRequest req) {
        return ResponseEntity.ok(siembraUseCase.actualizarSiembra(
                id, req.getFechaSiembra(), req.getCantidadPlantas(), req.getVariedadId()));
    }

    /** ?fecha=2026-11-04 opcional (por defecto hoy) */
    @PutMapping("/{id}/avanzar-etapa")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Siembra> avanzarEtapa(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(siembraUseCase.avanzarEtapa(id, fecha));
    }

    @PutMapping("/{id}/retroceder-etapa")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Siembra> retrocederEtapa(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.retrocederEtapa(id));
    }

    @PutMapping("/{id}/finalizar")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Siembra> finalizar(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) FinalizarSiembraRequest req) {
        LocalDate fecha = req != null ? req.fecha() : null;
        boolean descanso = req != null && req.dejarEnDescanso();
        return ResponseEntity.ok(siembraUseCase.finalizarSiembra(id, fecha, descanso));
    }

    @GetMapping("/{id}/etapas")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<List<EtapaSiembra>> etapas(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.listarEtapas(id));
    }

    // Devuelve siembras de TODOS los usuarios: solo administrador
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<Siembra>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(siembraUseCase.listarSiembrasPorEstado(estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.siembraPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        siembraUseCase.eliminarSiembra(id);
        return ResponseEntity.noContent().build();
    }
}