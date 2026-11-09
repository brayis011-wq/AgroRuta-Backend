package com.agroruta.worker.infrastructure.web;

import jakarta.validation.Valid;
import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorRequest;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorResponse;
import com.agroruta.worker.infrastructure.web.dto.TrabajadorUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
public class TrabajadorController {

    private final TrabajadorUseCase trabajadorUseCase;

    public TrabajadorController(TrabajadorUseCase trabajadorUseCase) {
        this.trabajadorUseCase = trabajadorUseCase;
    }

    @GetMapping
    public ResponseEntity<List<TrabajadorResponse>> listar(Authentication authentication) {
        List<Trabajador> lista = CurrentUser.esAdmin(authentication)
                ? trabajadorUseCase.listarActivos()
                : trabajadorUseCase.listarActivosDe(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(TrabajadorResponse::from).toList());
    }

    // ✅ /todos ANTES de /{id} para que Spring no lo confunda con un Long
    @GetMapping("/all")
    public ResponseEntity<List<TrabajadorResponse>> listarTodos(Authentication authentication) {
        List<Trabajador> lista = CurrentUser.esAdmin(authentication)
                ? trabajadorUseCase.listarTodos()
                : trabajadorUseCase.listarTodosDe(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(TrabajadorResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<TrabajadorResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.buscarPorId(id)));
    }

    // El cargo asignado debe pertenecer al usuario autenticado
    @PostMapping
    @PreAuthorize("@ownership.cargoPropio(#req.cargoId, authentication)")
    public ResponseEntity<TrabajadorResponse> registrar(@Valid @RequestBody TrabajadorRequest req,
                                                        Authentication authentication) {
        TrabajadorResponse response = TrabajadorResponse.from(
                trabajadorUseCase.registrarTrabajador(
                        req.nombre, req.apellido, req.cedula, req.telefono,
                        req.direccion, req.fechaIngreso, req.tipoContrato, req.cargoId,
                        CurrentUser.id(authentication)
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<TrabajadorResponse> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody TrabajadorUpdateRequest req) {
        TrabajadorResponse response = TrabajadorResponse.from(
                trabajadorUseCase.actualizarTrabajador(
                        id, req.nombre, req.apellido, req.telefono, req.direccion, req.tipoContrato
                )
        );
        return ResponseEntity.ok(response);
    }

    // Tanto el trabajador como el nuevo cargo deben ser del usuario autenticado
    @PatchMapping("/{id}/position/{cargoId}")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication) and @ownership.cargoPropio(#cargoId, authentication)")
    public ResponseEntity<TrabajadorResponse> cambiarCargo(@PathVariable Long id,
                                                           @PathVariable Long cargoId) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.cambiarCargo(id, cargoId)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        trabajadorUseCase.desactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> suspender(@PathVariable Long id) {
        trabajadorUseCase.suspenderTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        trabajadorUseCase.reactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    // ── Acceso al portal del trabajador ──────────────────────────────────────

    @PatchMapping("/{id}/access/approve")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<TrabajadorResponse> aprobarAcceso(@PathVariable Long id) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.aprobarAcceso(id)));
    }

    @PatchMapping("/{id}/access/reject")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<TrabajadorResponse> rechazarAcceso(@PathVariable Long id) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.rechazarAcceso(id)));
    }
}