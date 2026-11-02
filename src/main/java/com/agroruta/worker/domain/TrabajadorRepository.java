package com.agroruta.worker.domain;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository {

    Trabajador guardar(Trabajador trabajador);
    Optional<Trabajador> buscarPorId(Long id);
    Optional<Trabajador> buscarPorCedula(String cedula);
    List<Trabajador> listarTodos();
    List<Trabajador> listarActivos();
    List<Trabajador> listarTodosPorAgricultor(Long agricultorId);
    List<Trabajador> listarActivosPorAgricultor(Long agricultorId);
    void eliminar(Long id);
    boolean existePorCedula(String cedula);
}