package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.application.ports.out.CargoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class CargoRepositoryImpl implements CargoRepository {

    private final JpaCargoRepository jpa;

    CargoRepositoryImpl(JpaCargoRepository jpa) { this.jpa = jpa; }

    @Override public Cargo guardar(Cargo cargo) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(cargo)));
    }

    @Override public Optional<Cargo> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Cargo> listarTodos() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Cargo> listarActivos() {
        return jpa.findByActivoTrue().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Cargo> listarActivosPorAgricultor(Long agricultorId) {
        return jpa.findByActivoTrueAndAgricultorId(agricultorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return jpa.existsByNombre(nombre);
    }

    @Override
    public boolean existsByNombreAndIdNot(String nombre, Long id) {
        return jpa.existsByNombreAndIdNot(nombre, id);
    }

    @Override
    public boolean existsByNombreAndAgricultorId(String nombre, Long agricultorId) {
        return jpa.existsByNombreAndAgricultorId(nombre, agricultorId);
    }

    @Override
    public boolean existsByNombreAndAgricultorIdAndIdNot(String nombre, Long agricultorId, Long id) {
        return jpa.existsByNombreAndAgricultorIdAndIdNot(nombre, agricultorId, id);
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}