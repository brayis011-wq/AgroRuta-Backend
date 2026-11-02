package com.agroruta.worker.infrastructure.web;

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
@RequestMapping("/api/trabajadores")
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
    @GetMapping("/todos")
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
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<TrabajadorResponse> actualizar(@PathVariable Long id,
                                                         @RequestBody TrabajadorUpdateRequest req) {
        TrabajadorResponse response = TrabajadorResponse.from(
                trabajadorUseCase.actualizarTrabajador(
                        id, req.nombre, req.apellido, req.telefono, req.direccion, req.tipoContrato
                )
        );
        return ResponseEntity.ok(response);
    }

    // Tanto el trabajador como el nuevo cargo deben ser del usuario autenticado
    @PatchMapping("/{id}/cargo/{cargoId}")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication) and @ownership.cargoPropio(#cargoId, authentication)")
    public ResponseEntity<TrabajadorResponse> cambiarCargo(@PathVariable Long id,
                                                           @PathVariable Long cargoId) {
        return ResponseEntity.ok(TrabajadorResponse.from(trabajadorUseCase.cambiarCargo(id, cargoId)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        trabajadorUseCase.desactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/suspender")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> suspender(@PathVariable Long id) {
        trabajadorUseCase.suspenderTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("@ownership.trabajadorPropio(#id, authentication)")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        trabajadorUseCase.reactivarTrabajador(id);
        return ResponseEntity.noContent().build();
    }
}