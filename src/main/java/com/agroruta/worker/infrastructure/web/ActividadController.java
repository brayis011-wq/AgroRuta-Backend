package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.domain.Actividad;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
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

    /** El agricultor ve las labores comunes y las suyas; el Administrador las ve todas. */
    @GetMapping
    public ResponseEntity<List<ActividadResponse>> listar(Authentication authentication) {
        List<Actividad> lista = CurrentUser.esAdmin(authentication)
                ? actividadUseCase.listarActivas()
                : actividadUseCase.listarActivasPara(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(ActividadResponse::from).toList());
    }

    /** La labor queda a nombre de quien la crea; si la crea el Administrador es común para todos. */
    @PostMapping
    public ResponseEntity<ActividadResponse> crear(@Valid @RequestBody ActividadRequest req, Authentication authentication) {
        Long dueno = CurrentUser.esAdmin(authentication) ? null : CurrentUser.id(authentication);
        ActividadResponse response = ActividadResponse.from(
                actividadUseCase.crearActividad(req.nombre, req.descripcion, dueno)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.laborPropia(#id, authentication)")
    public ResponseEntity<ActividadResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ActividadRequest req) {
        ActividadResponse response = ActividadResponse.from(
                actividadUseCase.actualizarActividad(id, req.nombre, req.descripcion)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.laborPropia(#id, authentication)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        actividadUseCase.desactivarActividad(id);
        return ResponseEntity.noContent().build();
    }
}