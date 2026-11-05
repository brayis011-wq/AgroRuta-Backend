package com.agroruta.agriculturalInput.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;
import com.agroruta.agriculturalInput.application.ports.in.*;
import com.agroruta.agriculturalInput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.agriculturalInput.domain.exception.AgriculturalInputNotFoundException;
import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import com.agroruta.agriculturalInput.domain.AgriculturalInputType;
import com.agroruta.agriculturalInput.domain.MeasurementUnit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación.
 * Orquesta el dominio y los puertos; no contiene lógica de negocio propia.
 * Implementa todos los casos de uso del módulo usando métodos con nombres distintos
 * para respetar ISP y evitar conflictos de firmas.
 *
 * Nota: @Service se declara en AgriculturalInputConfig para mantener
 * la capa de aplicación libre de anotaciones de framework.
 */
@Transactional
public class AgriculturalInputService
        implements GetAllAgriculturalInputsUseCase,
        SearchAgriculturalInputsUseCase,
        GetAgriculturalInputByIdUseCase,
        CreateAgriculturalInputUseCase,
        UpdateAgriculturalInputUseCase,
        DeleteAgriculturalInputUseCase,
        ManageAgriculturalInputStatusUseCase {

    private final AgriculturalInputRepositoryPort repository;

    public AgriculturalInputService(AgriculturalInputRepositoryPort repository) {
        this.repository = repository;
    }

    // ── Queries (read-only) ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AgriculturalInput> getAll() {
        return repository.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgriculturalInput> search(String query) {
        if (query == null || query.isBlank()) {
            return repository.findAllActive();
        }
        return repository.searchByQuery(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public AgriculturalInput getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AgriculturalInputNotFoundException(id));
    }

    // ── Commands (write) ──────────────────────────────────────────────────────

    @Override
    public AgriculturalInput create(CreateAgriculturalInputCommand command) {
        String nombre = limpiarNombre(command.nombre());
        exigirNombreLibre(nombre, null);
        AgriculturalInput input = AgriculturalInput.create(
                nombre,
                AgriculturalInputType.valueOf(command.tipo()),
                MeasurementUnit.valueOf(command.unidadSugerida()),
                command.dosisSugerida(),
                command.reentradaHoras(),
                command.diasCarencia()
        );
        return repository.save(input);
    }

    @Override
    public AgriculturalInput update(Long id, UpdateAgriculturalInputCommand command) {
        AgriculturalInput input = repository.findById(id)
                .orElseThrow(() -> new AgriculturalInputNotFoundException(id));

        String nombre = limpiarNombre(command.nombre());
        exigirNombreLibre(nombre, id);
        input.update(
                nombre,
                AgriculturalInputType.valueOf(command.tipo()),
                MeasurementUnit.valueOf(command.unidadSugerida()),
                command.dosisSugerida(),
                command.reentradaHoras(),
                command.diasCarencia()
        );
        return repository.save(input);
    }

    @Override
    public void delete(Long id) {
        AgriculturalInput input = repository.findById(id)
                .orElseThrow(() -> new AgriculturalInputNotFoundException(id));
        input.deactivate();
        repository.save(input);
    }

    // ── Vista del Administrador ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AgriculturalInput> getAllIncludingInactive() {
        return repository.findAll();
    }

    @Override
    public AgriculturalInput changeStatus(Long id, boolean activo) {
        AgriculturalInput input = repository.findAnyById(id)
                .orElseThrow(() -> new AgriculturalInputNotFoundException(id));
        if (activo == input.isActivo()) {
            return input;
        }
        if (activo) {
            exigirNombreLibre(input.getNombre().trim(), id);
            input.activate();
        } else {
            input.deactivate();
        }
        return repository.save(input);
    }

    // ── Auxiliares ────────────────────────────────────────────────────────────

    /** Quita espacios de más: "  Mancozeb   80 WP " → "Mancozeb 80 WP" */
    static String limpiarNombre(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El nombre del insumo es obligatorio.");
        }
        return limpio;
    }

    /** Evita dos insumos activos con el mismo nombre (los agricultores no sabrían cuál elegir). */
    private void exigirNombreLibre(String nombre, Long excluirId) {
        if (repository.existsActiveByNombre(nombre, excluirId)) {
            throw new BusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "Ya existe un insumo activo llamado " + nombre + ".");
        }
    }
}
