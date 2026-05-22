package com.agroruta.agriculturalInput.application;

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
        DeleteAgriculturalInputUseCase {

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
        AgriculturalInput input = AgriculturalInput.create(
                command.nombre(),
                AgriculturalInputType.valueOf(command.tipo()),
                MeasurementUnit.valueOf(command.unidadSugerida()),
                command.dosisSugerida(),
                command.reentradaHoras()
        );
        return repository.save(input);
    }

    @Override
    public AgriculturalInput update(Long id, UpdateAgriculturalInputCommand command) {
        AgriculturalInput input = repository.findById(id)
                .orElseThrow(() -> new AgriculturalInputNotFoundException(id));

        input.update(
                command.nombre(),
                AgriculturalInputType.valueOf(command.tipo()),
                MeasurementUnit.valueOf(command.unidadSugerida()),
                command.dosisSugerida(),
                command.reentradaHoras()
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
}