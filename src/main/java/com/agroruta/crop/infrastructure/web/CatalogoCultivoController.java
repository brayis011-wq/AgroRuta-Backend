package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.infrastructure.web.dto.Respuestas.*;
import com.agroruta.crop.application.ports.in.CatalogoCultivoUseCase;
import com.agroruta.crop.infrastructure.web.dto.NombreCatalogoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de cultivos: lectura para agricultores, edición solo para el administrador. */
@RestController
@RequestMapping("/api/crop-catalog")
public class CatalogoCultivoController {

    private final CatalogoCultivoUseCase catalogo;

    public CatalogoCultivoController(CatalogoCultivoUseCase catalogo) {
        this.catalogo = catalogo;
    }

    /** Cultivos y variedades activos (para registrar una siembra) */
    @GetMapping
    public ResponseEntity<List<TipoCultivoResponse>> activos() {
        return ResponseEntity.ok(catalogo.listar(true).stream().map(TipoCultivoResponse::from).toList());
    }

    /** Todo el catálogo, incluidos los desactivados (pantalla de administración) */
    @GetMapping("/all")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<TipoCultivoResponse>> todos() {
        return ResponseEntity.ok(catalogo.listar(false).stream().map(TipoCultivoResponse::from).toList());
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<TipoCultivoResponse> crearTipo(@Valid @RequestBody NombreCatalogoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TipoCultivoResponse.from(catalogo.crearTipo(req.nombre())));
    }

    @PostMapping("/types/{id}/varieties")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<VariedadCultivoResponse> crearVariedad(@PathVariable Long id,
                                                         @Valid @RequestBody NombreCatalogoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VariedadCultivoResponse.from(catalogo.crearVariedad(id, req.nombre())));
    }

    @PatchMapping("/types/{id}/status")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<TipoCultivoResponse> estadoTipo(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(TipoCultivoResponse.from(catalogo.cambiarEstadoTipo(id, activo)));
    }

    @PatchMapping("/varieties/{id}/status")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<VariedadCultivoResponse> estadoVariedad(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(VariedadCultivoResponse.from(catalogo.cambiarEstadoVariedad(id, activo)));
    }
}
