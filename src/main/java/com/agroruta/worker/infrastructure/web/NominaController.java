package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.NominaUseCase;
import com.agroruta.worker.infrastructure.web.dto.NominaRequest;
import com.agroruta.worker.infrastructure.web.dto.NominaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nominas")
public class NominaController {

    private final NominaUseCase nominaUseCase;

    public NominaController(NominaUseCase nominaUseCase) {
        this.nominaUseCase = nominaUseCase;
    }

    @PostMapping("/generar")
    public ResponseEntity<NominaResponse> generar(@RequestBody NominaRequest req) {
        NominaResponse response = NominaResponse.from(
                nominaUseCase.generarNomina(req.trabajadorId, req.periodoInicio, req.periodoFin)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NominaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.buscarPorId(id)));
    }

    @GetMapping("/trabajador/{trabajadorId}")
    public ResponseEntity<List<NominaResponse>> porTrabajador(@PathVariable Long trabajadorId) {
        List<NominaResponse> nominas = nominaUseCase.listarPorTrabajador(trabajadorId)
                .stream().map(NominaResponse::from).toList();
        return ResponseEntity.ok(nominas);
    }

    @GetMapping
    public ResponseEntity<List<NominaResponse>> listarTodas() {
        List<NominaResponse> nominas = nominaUseCase.listarTodas()
                .stream().map(NominaResponse::from).toList();
        return ResponseEntity.ok(nominas);
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<NominaResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(NominaResponse.from(nominaUseCase.aprobarNomina(id)));
    }

    @PatchMapping("/{id}/anular")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        nominaUseCase.anularNomina(id);
        return ResponseEntity.noContent().build();
    }
}