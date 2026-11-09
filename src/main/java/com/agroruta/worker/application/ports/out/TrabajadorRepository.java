package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.Trabajador;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository {

    Trabajador guardar(Trabajador trabajador);
    Optional<Trabajador> buscarPorId(Long id);
    Optional<Trabajador> buscarPorCedula(String cedula);

    /** Busca por cédula ignorando puntos, espacios y guiones (1.052.334 = 1052334). */
    Optional<Trabajador> buscarPorCedulaNormalizada(String cedulaSoloAlfanumerica);

    /** Trabajador vinculado a una cuenta de usuario (portal del trabajador). */
    Optional<Trabajador> buscarPorUsuarioId(Long usuarioId);
    List<Trabajador> listarTodos();
    List<Trabajador> listarActivos();
    List<Trabajador> listarTodosPorAgricultor(Long agricultorId);
    List<Trabajador> listarActivosPorAgricultor(Long agricultorId);
    void eliminar(Long id);
    boolean existePorCedula(String cedula);
}