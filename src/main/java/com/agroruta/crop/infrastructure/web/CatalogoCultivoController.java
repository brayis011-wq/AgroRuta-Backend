package com.agroruta.crop.infrastructure.web;

import com.agroruta.crop.application.ports.in.CatalogoCultivoUseCase;
import com.agroruta.crop.domain.TipoCultivo;
import com.agroruta.crop.domain.VariedadCultivo;
import com.agroruta.crop.infrastructure.web.dto.NombreCatalogoRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo de cultivos: lectura para agricultores, edición solo para el administrador. */
@RestController
@RequestMapping("/api/catalogo-cultivos")
public class CatalogoCultivoController {

    private final CatalogoCultivoUseCase catalogo;

    public CatalogoCultivoController(CatalogoCultivoUseCase catalogo) {
        this.catalogo = catalogo;
    }

    /** Cultivos y variedades activos (para registrar una siembra) */
    @GetMapping
    public ResponseEntity<List<TipoCultivo>> activos() {
        return ResponseEntity.ok(catalogo.listar(true));
    }

    /** Todo el catálogo, incluidos los desactivados (pantalla de administración) */
    @GetMapping("/todos")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<List<TipoCultivo>> todos() {
        return ResponseEntity.ok(catalogo.listar(false));
    }

    @PostMapping("/tipos")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<TipoCultivo> crearTipo(@Valid @RequestBody NombreCatalogoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogo.crearTipo(req.nombre()));
    }

    @PostMapping("/tipos/{id}/variedades")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<VariedadCultivo> crearVariedad(@PathVariable Long id,
                                                         @Valid @RequestBody NombreCatalogoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogo.crearVariedad(id, req.nombre()));
    }

    @PatchMapping("/tipos/{id}/estado")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<TipoCultivo> estadoTipo(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(catalogo.cambiarEstadoTipo(id, activo));
    }

    @PatchMapping("/variedades/{id}/estado")
    @PreAuthorize("hasRole('Administrador')")
    public ResponseEntity<VariedadCultivo> estadoVariedad(@PathVariable Long id, @RequestParam boolean activo) {
        return ResponseEntity.ok(catalogo.cambiarEstadoVariedad(id, activo));
    }
}
