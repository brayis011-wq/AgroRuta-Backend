package com.agroruta.worker.domain;

import java.util.List;
import java.util.Optional;

public interface CargoRepository {
    Cargo guardar(Cargo cargo);
    Optional<Cargo> buscarPorId(Long id);
    List<Cargo> listarTodos();
    List<Cargo> listarActivos();
    void eliminar(Long id);
}