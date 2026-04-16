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

    List<Trabajador> listarActivos();

    List<Trabajador> listarTodos();
}