package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.Cargo;

import java.util.List;
import java.util.Optional;

public interface CargoRepository {
    Cargo guardar(Cargo cargo);
    Optional<Cargo> buscarPorId(Long id);
    List<Cargo> listarTodos();
    List<Cargo> listarActivos();
    List<Cargo> listarActivosPorAgricultor(Long agricultorId);
    void eliminar(Long id);
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, Long id);
    boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId);
    boolean existsByNombreAndAgricultorIdAndIdNot(String nombre, Long agricultorId, Long id);
}