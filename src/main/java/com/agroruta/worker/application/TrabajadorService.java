package com.agroruta.worker.application;

import com.agroruta.shared.security.CurrentUser;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.CargoRepository;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrabajadorService implements TrabajadorUseCase {

    private final TrabajadorRepository trabajadorRepository;
    private final CargoRepository cargoRepository;

    public TrabajadorService(TrabajadorRepository trabajadorRepository,
                             CargoRepository cargoRepository) {
        this.trabajadorRepository = trabajadorRepository;
        this.cargoRepository = cargoRepository;
    }

    public Trabajador registrarTrabajador(String nombre, String apellido, String cedula,
                                          String telefono, String direccion,
                                          LocalDate fechaIngreso, TipoContrato tipoContrato,
                                          Long cargoId) {
        if (trabajadorRepository.existePorCedula(cedula)) {
            throw new IllegalArgumentException("Ya existe un trabajador con cédula: " + cedula);
        }

        Cargo cargo = cargoRepository.buscarPorId(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con id: " + cargoId));

        Trabajador trabajador = new Trabajador(
                null, nombre, apellido, cedula,
                telefono, direccion, fechaIngreso, tipoContrato, cargo
        );
        // El dueño es siempre el usuario autenticado, nunca un valor enviado por el cliente
        trabajador.setAgricultorId(CurrentUser.idActual());

        return trabajadorRepository.guardar(trabajador);
    }

    public Trabajador actualizarTrabajador(Long id, String nombre, String apellido,
                                           String telefono, String direccion,
                                           TipoContrato tipoContrato) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + id));

        trabajador.setNombre(nombre);
        trabajador.setApellido(apellido);
        trabajador.setTelefono(telefono);
        trabajador.setDireccion(direccion);
        trabajador.setTipoContrato(tipoContrato);

        return trabajadorRepository.guardar(trabajador);
    }

    public Trabajador cambiarCargo(Long trabajadorId, Long cargoId) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(trabajadorId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + trabajadorId));

        Cargo nuevoCargo = cargoRepository.buscarPorId(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado con id: " + cargoId));

        trabajador.cambiarCargo(nuevoCargo);
        return trabajadorRepository.guardar(trabajador);
    }

    public void desactivarTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + id));
        trabajador.desactivar();
        trabajadorRepository.guardar(trabajador);
    }

    public void suspenderTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + id));
        trabajador.suspender();
        trabajadorRepository.guardar(trabajador);
    }

    public void reactivarTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + id));
        trabajador.reactivar();
        trabajadorRepository.guardar(trabajador);
    }

    public Trabajador buscarPorId(Long id) {
        return trabajadorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Trabajador no encontrado con id: " + id));
    }

    public List<Trabajador> listarActivos() {
        return trabajadorRepository.listarActivos();
    }

    public List<Trabajador> listarTodos() {
        return trabajadorRepository.listarTodos();
    }

    public List<Trabajador> listarActivosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return trabajadorRepository.listarActivosPorAgricultor(agricultorId);
    }

    public List<Trabajador> listarTodosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return trabajadorRepository.listarTodosPorAgricultor(agricultorId);
    }
}