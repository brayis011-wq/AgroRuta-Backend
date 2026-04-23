package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.LoteUseCase;
import com.agroruta.cultivo.domain.Lote;
import com.agroruta.cultivo.infrastructure.web.dto.LoteRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Lote> registrar(@RequestBody LoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                loteUseCase.registrarLote(req.getNombre(), req.getArea(), req.getFincaId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteUseCase.buscarLotePorId(id));
    }

    @GetMapping("/finca/{fincaId}")
    public ResponseEntity<List<Lote>> listarPorFinca(@PathVariable Long fincaId) {
        return ResponseEntity.ok(loteUseCase.listarLotesPorFinca(fincaId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        loteUseCase.eliminarLote(id);
        return ResponseEntity.noContent().build();
    }
}