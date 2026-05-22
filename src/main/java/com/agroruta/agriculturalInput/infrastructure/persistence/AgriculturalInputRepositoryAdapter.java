package com.agroruta.agriculturalInput.infrastructure.persistence;

import com.agroruta.agriculturalInput.application.ports.out.AgriculturalInputRepositoryPort;
import com.agroruta.agriculturalInput.domain.AgriculturalInput;
import com.agroruta.agriculturalInput.infrastructure.persistence.AgriculturalInputJpaRepository;
import com.agroruta.agriculturalInput.infrastructure.persistence.AgriculturalInputPersistenceMapper;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia (driven adapter).
 * Implementa el puerto de salida usando Spring Data JPA.
 * Es la única clase que conoce tanto el puerto como JPA.
 *
 * La dirección de dependencia es:
 *   Adapter → JpaRepository  (infraestructura)
 *   Adapter implements Port  (aplicación)
 * → El dominio y la aplicación nunca ven esta clase.
 */
public class AgriculturalInputRepositoryAdapter implements AgriculturalInputRepositoryPort {

    private final AgriculturalInputJpaRepository jpaRepository;

    public AgriculturalInputRepositoryAdapter(AgriculturalInputJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<AgriculturalInput> findAllActive() {
        return jpaRepository.findByActivoTrue()
                .stream()
                .map(AgriculturalInputPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<AgriculturalInput> searchByQuery(String query) {
        return jpaRepository.searchActiveByQuery(query)
                .stream()
                .map(AgriculturalInputPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AgriculturalInput> findById(Long id) {
        return jpaRepository.findByIdAndActivoTrue(id)
                .map(AgriculturalInputPersistenceMapper::toDomain);
    }

    @Override
    public AgriculturalInput save(AgriculturalInput domain) {
        var entity = AgriculturalInputPersistenceMapper.toEntity(domain);
        var saved  = jpaRepository.save(entity);
        return AgriculturalInputPersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}