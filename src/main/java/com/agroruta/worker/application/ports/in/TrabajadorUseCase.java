package com.agroruta.worker.application.ports.in;

import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;

import java.time.LocalDate;
import java.util.List;

public interface TrabajadorUseCase {

    Trabajador registrarTrabajador(String nombre, String apellido, String cedula,
                                   String telefono, String direccion,
                                   LocalDate fechaIngreso, TipoContrato tipoContrato,
                                   Long cargoId);

    Trabajador actualizarTrabajador(Long id, String nombre, String apellido,
                                    String telefono, String direccion,
                                    TipoContrato tipoContrato);

    Trabajador cambiarCargo(Long trabajadorId, Long cargoId);

    void desactivarTrabajador(Long id);

    void suspenderTrabajador(Long id);

    void reactivarTrabajador(Long id);

    Trabajador buscarPorId(Long id);

    /** Todos los activos (uso del Administrador). */
    List<Trabajador> listarActivos();

    /** Todos, de cualquier estado (uso del Administrador). */
    List<Trabajador> listarTodos();

    /** Activos de un agricultor. Si agricultorId es null devuelve lista vacía. */
    List<Trabajador> listarActivosDe(Long agricultorId);

    /** Todos los de un agricultor. Si agricultorId es null devuelve lista vacía. */
    List<Trabajador> listarTodosDe(Long agricultorId);
}