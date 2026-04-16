package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.SiembraUseCase;
import com.agroruta.cultivo.domain.Siembra;
import com.agroruta.cultivo.infrastructure.web.dto.SiembraRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/siembras")
public class SiembraController {

    private final SiembraUseCase siembraUseCase;

    public SiembraController(SiembraUseCase siembraUseCase) {
        this.siembraUseCase = siembraUseCase;
    }

    @PostMapping
    public ResponseEntity<Siembra> registrar(@RequestBody SiembraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                siembraUseCase.registrarSiembra(
                        req.getFechaSiembra(), req.getCantidadPlantas(),
                        req.getVariedad(), req.getLoteId()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Siembra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.buscarSiembraPorId(id));
    }

    @GetMapping("/lote/{loteId}")
    public ResponseEntity<Siembra> buscarPorLote(@PathVariable Long loteId) {
        return ResponseEntity.ok(siembraUseCase.buscarSiembraPorLote(loteId));
    }

    @PutMapping("/{id}/avanzar-etapa")
    public ResponseEntity<Siembra> avanzarEtapa(@PathVariable Long id) {
        return ResponseEntity.ok(siembraUseCase.avanzarEtapa(id));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Siembra>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(siembraUseCase.listarSiembrasPorEstado(estado));
    }
}