package com.agroruta.worker.infrastructure.web;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.CargoUseCase;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.infrastructure.web.dto.CargoRequest;
import com.agroruta.worker.infrastructure.web.dto.CargoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
public class CargoController {

    private final CargoUseCase cargoUseCase;

    public CargoController(CargoUseCase cargoUseCase) {
        this.cargoUseCase = cargoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CargoResponse>> listar(Authentication authentication) {
        List<Cargo> lista = CurrentUser.esAdmin(authentication)
                ? cargoUseCase.listarActivos()
                : cargoUseCase.listarActivosDe(CurrentUser.id(authentication));
        return ResponseEntity.ok(lista.stream().map(CargoResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ownership.cargoPropio(#id, authentication)")
    public ResponseEntity<CargoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(CargoResponse.from(cargoUseCase.buscarPorId(id)));
    }

    @PostMapping
    public ResponseEntity<CargoResponse> crear(@RequestBody CargoRequest req) {
        CargoResponse response = CargoResponse.from(
                cargoUseCase.crearCargo(req.nombre, req.descripcion, req.valorJornal)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ownership.cargoPropio(#id, authentication)")
    public ResponseEntity<CargoResponse> actualizar(@PathVariable Long id,
                                                    @RequestBody CargoRequest req) {
        CargoResponse response = CargoResponse.from(
                cargoUseCase.actualizarCargo(id, req.nombre, req.descripcion, req.valorJornal)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ownership.cargoPropio(#id, authentication)")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cargoUseCase.desactivarCargo(id);
        return ResponseEntity.noContent().build();
    }
}