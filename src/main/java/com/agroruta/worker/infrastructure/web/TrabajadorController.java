package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorRequest;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorResponse;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
public class TrabajadorController {

    private final TrabajadorUseCase trabajadorUseCase;

    public TrabajadorController(TrabajadorUseCase trabajadorUseCase) {
        this.trabajadorUseCase = trabajadorUseCase;
    }

    @GetMapping
    public ResponseEntity<List<TrabajadorResponse>> listar() {
        List<TrabajadorResponse> trabajadores = trabajadorUseCase.listarActivos()
                .stream().map(TrabajadorResponse::from).toList();
        return ResponseEntity.ok(trabajadores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrabajadorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<TrabajadorResponse> registrar(@RequestBody TrabajadorRequest req) {
        TrabajadorResponse response = TrabajadorResponse.from(
                trabajadorUseCase.registrarTrabajador(
                        req.nombre, req.apellido, req.cedula, req.telefono,
                        req.direccion, req.fechaIngreso, req.tipoContrato, req.cargoId
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrabajadorResponse> actualizar(@PathVariable Long id,
                                                         @RequestBody TrabajadorUpdateRequest req) {
        TrabajadorResponse response = TrabajadorResponse.from(
                trabajadorUseCase.actualizarTrabajador(
                        id, req.nombre, req.apellido, req.telefono, req.direccion, req.tipoContrato
                )
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cargo/{cargoId}")
    public ResponseEntity<TrabajadorResponse> cambiarCargo(@PathVariable Long id,
                                                           @PathVariable Long cargoId) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.cambiarCargo(id, cargoId)));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        trabajadorUseCase.desactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspender")
    public ResponseEntity<Void> suspender(@PathVariable Long id) {
        trabajadorUseCase.suspenderTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        trabajadorUseCase.reactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }
}