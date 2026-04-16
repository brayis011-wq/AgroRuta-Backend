package com.agroruta.worker.domain;

import com.agroRuta.worker.domain.Trabajador;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository {
    Trabajador guardar(Trabajador trabajador);
    Optional<Trabajador> buscarPorId(Long id);
    Optional<Trabajador> buscarPorCedula(String cedula);
    List<Trabajador> listarTodos();
    List<Trabajador> listarActivos();
    void eliminar(Long id);
    boolean existePorCedula(String cedula);
}
