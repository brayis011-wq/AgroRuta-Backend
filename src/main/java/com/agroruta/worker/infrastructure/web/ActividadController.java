package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.ActividadUseCase;
import com.agroruta.worker.infrastructure.web.dto.ActividadRequest;
import com.agroruta.worker.infrastructure.web.dto.ActividadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worker/actividades")
public class ActividadController {

    private final ActividadUseCase actividadUseCase;

    public ActividadController(ActividadUseCase actividadUseCase) {
        this.actividadUseCase = actividadUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ActividadResponse>> listar() {
        List<ActividadResponse> actividades = actividadUseCase.listarActivas()
                .stream().map(ActividadResponse::from).toList();
        return ResponseEntity.ok(actividades);
    }

    @PostMapping
    public ResponseEntity<ActividadResponse> crear(@RequestBody ActividadRequest req) {
        ActividadResponse response = ActividadResponse.from(
                actividadUseCase.crearActividad(req.nombre, req.descripcion)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadResponse> actualizar(@PathVariable Long id,
                                                        @RequestBody ActividadRequest req) {
        ActividadResponse response = ActividadResponse.from(
                actividadUseCase.actualizarActividad(id, req.nombre, req.descripcion)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        actividadUseCase.desactivarActividad(id);
        return ResponseEntity.noContent().build();
    }
}