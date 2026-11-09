package com.agroruta.worker.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.out.CargoRepository;
import com.agroruta.worker.domain.Cargo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import com.agroruta.worker.application.ports.in.CargoUseCase;

@Service
public class CargoService implements CargoUseCase {
    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
    }

    @Override
    public Cargo crearCargo(String nombre, String descripcion, BigDecimal valorJornal, Long agricultorId) {

        // El dueño lo pasa el controlador desde la sesión, nunca un valor enviado por el cliente

        if (cargoRepository.existsByNombreAndAgricultorId(nombre, agricultorId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe un cargo con el nombre '%s'.", nombre)
            );
        }

        Cargo cargo = new Cargo(null, nombre, descripcion, valorJornal, true);
        cargo.setAgricultorId(agricultorId);
        return cargoRepository.guardar(cargo);
    }

    @Override
    public Cargo actualizarCargo(Long id, String nombre, String descripcion, BigDecimal valorJornal) {

        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo", id));

        if (cargoRepository.existsByNombreAndAgricultorIdAndIdNot(nombre, cargo.getAgricultorId(), id)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS,
                    String.format("Ya existe otro cargo con el nombre '%s'.", nombre)
            );
        }

        cargo.actualizar(nombre, descripcion, valorJornal);
        return cargoRepository.guardar(cargo);
    }

    @Override
    public void desactivarCargo(Long id) {
        Cargo cargo = cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo", id));
        cargo.desactivar();
        cargoRepository.guardar(cargo);
    }

    @Override
    public Cargo buscarPorId(Long id) {
        return cargoRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo", id));
    }


    public List<Cargo> listarActivos() {
        return cargoRepository.listarActivos();
    }

    public List<Cargo> listarActivosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return cargoRepository.listarActivosPorAgricultor(agricultorId);
    }

    public List<Cargo> listarTodos() {
        return cargoRepository.listarTodos();
    }
}