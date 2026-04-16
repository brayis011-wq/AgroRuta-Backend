package com.agroruta.worker.application;

import com.agroruta.worker.domain.CargoRepository;
import com.agroruta.worker.domain.Cargo;

import java.math.BigDecimal;
import java.util.List;

public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    public Cargo crearCargo(String nombre, String descripcion, BigDecimal valorJornal) {
        Cargo cargo = new Cargo(null, nombre, descripcion, valorJornal, true);
        return cargoRepository.guardar(cargo);
    }

    public Cargo actualizarCargo(Long id, String nombre, String descripcion, BigDecimal valorJornal) {
        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con id: " + id));
        cargo.actualizar(nombre, descripcion, valorJornal);
        return cargoRepository.guardar(cargo);
    }

    public void desactivarCargo(Long id) {
        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con id: " + id));
        cargo.desactivar();
        cargoRepository.guardar(cargo);
    }

    public Cargo buscarPorId(Long id) {
        return cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con id: " + id));
    }

    public List<Cargo> listarActivos() {
        return cargoRepository.listarActivos();
    }

    public List<Cargo> listarTodos() {
        return cargoRepository.listarTodos();
    }
}