package com.agroruta.cultivo.infrastructure.web;

import com.agroruta.cultivo.application.ports.in.FincaUseCase;
import com.agroruta.cultivo.domain.Finca;
import com.agroruta.cultivo.infrastructure.web.dto.FincaRequest;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<Finca> registrar(@RequestBody FincaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                fincaUseCase.registrarFinca(
                        req.getNombre(), req.getUbicacion(),
                        req.getHectareas(), req.getAgricultorId()
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Finca> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(fincaUseCase.buscarFincaPorId(id));
    }

    @GetMapping("/agricultor/{agricultorId}")
    public ResponseEntity<List<Finca>> listarPorAgricultor(@PathVariable Long agricultorId) {
        return ResponseEntity.ok(fincaUseCase.listarFincasPorAgricultor(agricultorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        fincaUseCase.eliminarFinca(id);
        return ResponseEntity.noContent().build();
    }
}