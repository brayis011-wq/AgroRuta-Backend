package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Cargo;
import java.math.BigDecimal;
import java.util.List;

public interface CargoUseCase {

    Cargo crearCargo(String nombre, String descripcion, BigDecimal valorJornal);

    Cargo actualizarCargo(Long id, String nombre, String descripcion, BigDecimal valorJornal);

    void desactivarCargo(Long id);

    Cargo buscarPorId(Long id);

    List<Cargo> listarActivos();

    List<Cargo> listarTodos();
}