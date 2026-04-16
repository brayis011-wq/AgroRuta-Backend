package com.agroruta.worker.infrastructure.web;

import com.agroruta.worker.application.ports.in.CargoUseCase;
import com.agroruta.worker.infrastructure.web.dto.CargoRequest;
import com.agroruta.worker.infrastructure.web.dto.CargoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<CargoResponse>> listar() {
        List<CargoResponse> cargos = cargoUseCase.listarActivos()
                .stream().map(CargoResponse::from).toList();
        return ResponseEntity.ok(cargos);
    }

    @GetMapping("/{id}")
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
    public ResponseEntity<CargoResponse> actualizar(@PathVariable Long id,
                                                    @RequestBody CargoRequest req) {
        CargoResponse response = CargoResponse.from(
                cargoUseCase.actualizarCargo(id, req.nombre, req.descripcion, req.valorJornal)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        cargoUseCase.desactivarCargo(id);
        return ResponseEntity.noContent().build();
    }
}