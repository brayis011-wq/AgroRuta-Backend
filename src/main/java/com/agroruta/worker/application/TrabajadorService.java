package com.agroruta.worker.application;

import com.agroruta.shared.evento.AccesoPortalResuelto;
import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.worker.application.ports.in.TrabajadorUseCase;
import com.agroruta.worker.application.ports.out.CargoRepository;
import com.agroruta.worker.application.ports.out.TrabajadorRepository;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.TipoContrato;
import com.agroruta.worker.domain.Trabajador;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrabajadorService implements TrabajadorUseCase {

    private final TrabajadorRepository trabajadorRepository;
    private final CargoRepository cargoRepository;
    private final ApplicationEventPublisher eventos;

    public TrabajadorService(
            TrabajadorRepository trabajadorRepository,
            CargoRepository cargoRepository,
            ApplicationEventPublisher eventos) {
        this.trabajadorRepository = trabajadorRepository;
        this.cargoRepository = cargoRepository;
        this.eventos = eventos;
    }

    public Trabajador registrarTrabajador(
            String nombre,
            String apellido,
            String cedula,
            String telefono,
            String direccion,
            LocalDate fechaIngreso,
            TipoContrato tipoContrato,
            Long cargoId,
            Long agricultorId) {
        // Única por agricultor: otro agricultor puede tener registrada a la misma persona
        if (trabajadorRepository.existePorCedulaYAgricultor(cedula, agricultorId)) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_ALREADY_EXISTS, "Ya tienes un trabajador con cédula: " + cedula);
        }

        Cargo cargo = cargoRepository
                .buscarPorId(cargoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo no encontrado con id: " + cargoId));

        Trabajador trabajador =
                new Trabajador(null, nombre, apellido, cedula, telefono, direccion, fechaIngreso, tipoContrato, cargo);
        // El dueño lo pasa el controlador desde la sesión, nunca un valor enviado por el cliente
        trabajador.setAgricultorId(agricultorId);

        return trabajadorRepository.guardar(trabajador);
    }

    public Trabajador actualizarTrabajador(
            Long id, String nombre, String apellido, String telefono, String direccion, TipoContrato tipoContrato) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));

        trabajador.setNombre(nombre);
        trabajador.setApellido(apellido);
        trabajador.setTelefono(telefono);
        trabajador.setDireccion(direccion);
        trabajador.setTipoContrato(tipoContrato);

        return trabajadorRepository.guardar(trabajador);
    }

    public Trabajador cambiarCargo(Long trabajadorId, Long cargoId) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));

        Cargo nuevoCargo = cargoRepository
                .buscarPorId(cargoId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo no encontrado con id: " + cargoId));

        trabajador.cambiarCargo(nuevoCargo);
        return trabajadorRepository.guardar(trabajador);
    }

    public void desactivarTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
        trabajador.desactivar();
        trabajadorRepository.guardar(trabajador);
    }

    public void suspenderTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
        trabajador.suspender();
        trabajadorRepository.guardar(trabajador);
    }

    public void reactivarTrabajador(Long id) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
        trabajador.reactivar();
        trabajadorRepository.guardar(trabajador);
    }

    @Override
    public Trabajador aprobarAcceso(Long id) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
        trabajador.aprobarAcceso();
        Trabajador guardado = trabajadorRepository.guardar(trabajador);
        eventos.publishEvent(new AccesoPortalResuelto(id, true, false));
        return guardado;
    }

    @Override
    public Trabajador rechazarAcceso(Long id) {
        Trabajador trabajador = trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
        boolean estabaAprobado = trabajador.tieneAccesoAprobado();
        trabajador.rechazarAcceso();
        Trabajador guardado = trabajadorRepository.guardar(trabajador);
        eventos.publishEvent(new AccesoPortalResuelto(id, false, estabaAprobado));
        return guardado;
    }

    @Transactional(readOnly = true)
    public Trabajador buscarPorId(Long id) {
        return trabajadorRepository
                .buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Trabajador> listarActivos() {
        return trabajadorRepository.listarActivos();
    }

    @Transactional(readOnly = true)
    public List<Trabajador> listarTodos() {
        return trabajadorRepository.listarTodos();
    }

    @Transactional(readOnly = true)
    public List<Trabajador> listarActivosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return trabajadorRepository.listarActivosPorAgricultor(agricultorId);
    }

    @Transactional(readOnly = true)
    public List<Trabajador> listarTodosDe(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return trabajadorRepository.listarTodosPorAgricultor(agricultorId);
    }
}
