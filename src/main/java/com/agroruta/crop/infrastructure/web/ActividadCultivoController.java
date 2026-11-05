package com.agroruta.crop.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.ActividadCultivoUseCase;
import com.agroruta.crop.domain.ActividadCultivo;
import com.agroruta.crop.infrastructure.web.dto.ActividadRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ActividadCultivoController {

    private final ActividadCultivoUseCase actividadUseCase;

    public ActividadCultivoController(ActividadCultivoUseCase actividadUseCase) {
        this.actividadUseCase = actividadUseCase;
    }

    @PostMapping
    @PreAuthorize("@ownership.siembraPropia(#req.siembraId, authentication)")
    public ResponseEntity<ActividadCultivo> registrar(@Valid @RequestBody ActividadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                actividadUseCase.registrarActividad(
                        req.getTipo(), req.getDescripcion(),
                        req.getFecha(), req.getSiembraId(), req.getCosto()
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.actividadPropia(#id, authentication)")
    public ResponseEntity<ActividadCultivo> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(actividadUseCase.buscarActividadPorId(id));
    }

    /** La siembra del cuerpo se ignora */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.actividadPropia(#id, authentication)")
    public ResponseEntity<ActividadCultivo> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody ActividadRequest req) {
        return ResponseEntity.ok(actividadUseCase.actualizarActividad(
                id, req.getTipo(), req.getDescripcion(), req.getFecha(), req.getCosto()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.actividadPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        actividadUseCase.eliminarActividad(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/siembra/{siembraId}")
    @PreAuthorize("@ownership.siembraPropia(#siembraId, authentication)")
    public ResponseEntity<List<ActividadCultivo>> listarPorSiembra(@PathVariable Long siembraId) {
        return ResponseEntity.ok(actividadUseCase.listarActividadesPorSiembra(siembraId));
    }
}