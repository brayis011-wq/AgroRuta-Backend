package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.domain.Labor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import com.agroruta.worker.application.ports.in.LaborUseCase;
import com.agroruta.worker.infrastructure.web.dto.LaborRequest;
import com.agroruta.worker.infrastructure.web.dto.LaborResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labors")
public class LaborController {

    private final LaborUseCase laborUseCase;

    public LaborController(LaborUseCase laborUseCase) {
        this.laborUseCase = laborUseCase;
    }

    /** El agricultor ve las labores comunes y las suyas; el Administrador las ve todas. */
    @GetMapping
    public ResponseEntity<List<LaborResponse>> listar(Authentication authentication) {
        List<Labor> lista = CurrentUser.esAdmin(authentication)
                ? laborUseCase.listarActivas()
                : laborUseCase.listarActivasPara(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(LaborResponse::from).toList());
    }

    /** La labor queda a nombre de quien la crea; si la crea el Administrador es común para todos. */
    @PostMapping
    public ResponseEntity<LaborResponse> crear(@Valid @RequestBody LaborRequest req, Authentication authentication) {
        Long dueno = CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
        LaborResponse response = LaborResponse.from(
                laborUseCase.crearActividad(req.nombre, req.descripcion, dueno)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.laborPropia(#id, authentication)")
    public ResponseEntity<LaborResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody LaborRequest req) {
        LaborResponse response = LaborResponse.from(
                laborUseCase.actualizarActividad(id, req.nombre, req.descripcion)
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("@ownership.laborPropia(#id, authentication)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        laborUseCase.desactivarActividad(id);
        return ResponseEntity.noContent().build();
    }
}