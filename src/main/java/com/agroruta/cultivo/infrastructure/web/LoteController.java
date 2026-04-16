package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.LoteUseCase;
import com.agroruta.cultivo.domain.Lote;
import com.agroruta.cultivo.infrastructure.web.dto.LoteRequest;
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
    public ResponseEntity<Lote> registrar(@RequestBody LoteRequest request) {
        Lote lote = new Lote(
                null,
                request.getNombre(),
                request.getArea(),
                request.getFincaId()
        );
        return ResponseEntity.ok(loteUseCase.registrarLote(lote));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lote> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(loteUseCase.buscarLotePorId(id));
    }

    @GetMapping("/finca/{fincaId}")
    public ResponseEntity<List<Lote>> listarPorFinca(@PathVariable Long fincaId) {
        return ResponseEntity.ok(loteUseCase.listarLotesPorFinca(fincaId));
    }
}