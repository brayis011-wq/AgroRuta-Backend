package com.agroruta.agriculturalInput.infrastructure.web;

import com.agroruta.agriculturalInput.application.ports.in.*;
import com.agroruta.agriculturalInput.infrastructure.web.dto.AgriculturalInputRequest;
import com.agroruta.agriculturalInput.infrastructure.web.dto.AgriculturalInputResponse;
import com.agroruta.agriculturalInput.infrastructure.web.dto.AgriculturalInputWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST (driving adapter).
 * Completamente delgado: valida entrada, delega al caso de uso, mapea salida.
 * No contiene lógica de negocio. Cada endpoint inyecta solo el caso de uso que usa.
 *

 */
@RestController
@RequestMapping("/agricultural-inputs")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class AgriculturalInputController {

    // Cada campo es un caso de uso independiente (respeta ISP)
    private final GetAllAgriculturalInputsUseCase  getAllUseCase;
    private final SearchAgriculturalInputsUseCase  searchUseCase;
    private final GetAgriculturalInputByIdUseCase  getByIdUseCase;
    private final CreateAgriculturalInputUseCase   createUseCase;
    private final UpdateAgriculturalInputUseCase   updateUseCase;
    private final DeleteAgriculturalInputUseCase   deleteUseCase;

    public AgriculturalInputController(
            GetAllAgriculturalInputsUseCase  getAllUseCase,
            SearchAgriculturalInputsUseCase  searchUseCase,
            GetAgriculturalInputByIdUseCase  getByIdUseCase,
            CreateAgriculturalInputUseCase   createUseCase,
            UpdateAgriculturalInputUseCase   updateUseCase,
            DeleteAgriculturalInputUseCase   deleteUseCase) {
        this.getAllUseCase   = getAllUseCase;
        this.searchUseCase  = searchUseCase;
        this.getByIdUseCase = getByIdUseCase;
        this.createUseCase  = createUseCase;
        this.updateUseCase  = updateUseCase;
        this.deleteUseCase  = deleteUseCase;
    }

    // ── GET /api/v1/agricultural-inputs ──────────────────────────────────────
    /**
     * Lista todos los insumos activos del catálogo.
     * Usado por el formulario de fumigación para poblar el buscador.
     * Si se pasa ?q=mancozeb filtra por nombre o tipo.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AgriculturalInputResponse>> getAll(
            @RequestParam(required = false) String q) {

        List<AgriculturalInputResponse> result = q != null && !q.isBlank()
                ? searchUseCase.search(q).stream().map(AgriculturalInputWebMapper::toResponse).toList()
                : getAllUseCase.getAll().stream().map(AgriculturalInputWebMapper::toResponse).toList();

        return ResponseEntity.ok(result);
    }

    // ── GET /api/v1/agricultural-inputs/{id} ─────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgriculturalInputResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                AgriculturalInputWebMapper.toResponse(getByIdUseCase.getById(id))
        );
    }

    // ── POST /api/v1/agricultural-inputs ─────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgriculturalInputResponse> create(
            @Valid @RequestBody AgriculturalInputRequest request) {

        var created = createUseCase.create(
                AgriculturalInputWebMapper.toCreateCommand(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AgriculturalInputWebMapper.toResponse(created));
    }

    // ── PUT /api/v1/agricultural-inputs/{id} ─────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgriculturalInputResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AgriculturalInputRequest request) {

        var updated = updateUseCase.update(id,
                AgriculturalInputWebMapper.toUpdateCommand(request)
        );
        return ResponseEntity.ok(AgriculturalInputWebMapper.toResponse(updated));
    }

    // ── DELETE /api/v1/agricultural-inputs/{id} ───────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}