package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Labor;
import com.agroruta.worker.domain.LaborRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class LaborRepositoryImpl implements LaborRepository {

    private final JpaLaborRepository jpa;

    LaborRepositoryImpl(JpaLaborRepository jpa) { this.jpa = jpa; }

    @Override public Labor guardar(Labor actividad) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(actividad)));
    }
    @Override
    public boolean existsByNombre(String nombre) {
        return jpa.existsByNombre(nombre);
    }

    @Override
    public boolean existsByNombreAndIdNot(String nombre, Long id) {
        return jpa.existsByNombreAndIdNot(nombre, id);
    }
    @Override public Optional<Labor> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Labor> listarTodas() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Labor> listarActivas() {
        return jpa.findByActivaTrue().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Labor> listarActivasPara(Long agricultorId) {
        return jpa.activasPara(agricultorId).stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public boolean existeNombreVisible(String nombre, Long agricultorId, Long excluirId) {
        return jpa.nombreVisible(nombre, agricultorId, excluirId);
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}