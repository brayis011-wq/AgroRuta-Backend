package com.bg.agroruta.cultivo.infrastructure.web;

import com.bg.agroruta.cultivo.application.ports.in.FincaUseCase;
import com.bg.agroruta.cultivo.domain.Finca;
import com.bg.agroruta.cultivo.infrastructure.web.dto.FincaRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fincas")
public class FincaController {

    private final FincaUseCase fincaUseCase;

    public FincaController(FincaUseCase fincaUseCase) {
        this.fincaUseCase = fincaUseCase;
    }

    @PostMapping
    public ResponseEntity<Finca> registrar(@RequestBody FincaRequest request) {
        Finca finca = new Finca(
                null,
                request.getNombre(),
                request.getUbicacion(),
                request.getHectareas(),
                request.getAgricultorId()
        );
        return ResponseEntity.ok(fincaUseCase.registrarFinca(finca));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Finca> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(fincaUseCase.buscarFincaPorId(id));
    }

    @GetMapping("/agricultor/{agricultorId}")
    public ResponseEntity<List<Finca>> listarPorAgricultor(@PathVariable Long agricultorId) {
        return ResponseEntity.ok(fincaUseCase.listarFincasPorAgricultor(agricultorId));
    }
}