package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import jakarta.validation.Valid;
import com.agroruta.crop.application.ports.in.FincaUseCase;
import com.agroruta.crop.domain.Finca;
import com.agroruta.crop.infrastructure.web.dto.ActualizarCentroideFincaRequest;
import com.agroruta.crop.infrastructure.web.dto.FincaRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@ownership.esAgricultor(#req.agricultorId, authentication)")
    public ResponseEntity<FincaResponse> registrar(@Valid @RequestBody FincaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(FincaResponse.from(fincaUseCase.registrarFinca(
                        req.getNombre(), req.getUbicacion(),
                        req.getHectareas(), req.getAgricultorId()
                )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.fincaPropia(#id, authentication)")
    public ResponseEntity<FincaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(FincaResponse.from(fincaUseCase.buscarFincaPorId(id)));
    }

    @GetMapping("/agricultor/{agricultorId}")
    @PreAuthorize("@ownership.esAgricultor(#agricultorId, authentication)")
    public ResponseEntity<List<FincaResponse>> listarPorAgricultor(@PathVariable Long agricultorId) {
        return ResponseEntity.ok(fincaUseCase.listarFincasPorAgricultor(agricultorId).stream().map(FincaResponse::from).toList());
    }

    /** Edita nombre, ubicación y hectáreas (el agricultor del cuerpo se ignora) */
    @PutMapping("/{id}")
    @PreAuthorize("@ownership.fincaPropia(#id, authentication)")
    public ResponseEntity<FincaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody FincaRequest req) {
        return ResponseEntity.ok(FincaResponse.from(fincaUseCase.actualizarFinca(
                id, req.getNombre(), req.getUbicacion(), req.getHectareas())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.fincaPropia(#id, authentication)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        fincaUseCase.eliminarFinca(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/centroide")
    @PreAuthorize("@ownership.fincaPropia(#id, authentication)")
    public ResponseEntity<FincaResponse> actualizarCentroide(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCentroideFincaRequest request) {

        Finca finca = fincaUseCase.actualizarCentroideFinca(
                id, request.centroideLat(), request.centroideLng()
        );
        return ResponseEntity.ok(FincaResponse.from(finca));
    }
}