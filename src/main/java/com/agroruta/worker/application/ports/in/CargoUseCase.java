package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.Cargo;
import java.math.BigDecimal;
import java.util.List;

public interface CargoUseCase {

    /** @param agricultorId dueño del cargo: el usuario autenticado (lo pasa el controlador) */
    Cargo crearCargo(String nombre, String descripcion, BigDecimal valorJornal, Long agricultorId);

    Cargo actualizarCargo(Long id, String nombre, String descripcion, BigDecimal valorJornal);

    void desactivarCargo(Long id);

    Cargo buscarPorId(Long id);

    /** Todos los cargos activos (uso del Administrador). */
    List<Cargo> listarActivos();

    /** Cargos activos de un agricultor. Si agricultorId es null devuelve lista vacía. */
    List<Cargo> listarActivosDe(Long agricultorId);

    List<Cargo> listarTodos();
}