package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.EstadoTrabajador;
import com.agroruta.worker.domain.Trabajador;
import com.agroruta.worker.domain.TrabajadorRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class TrabajadorRepositoryImpl implements TrabajadorRepository {

    private final JpaTrabajadorRepository jpa;

    TrabajadorRepositoryImpl(JpaTrabajadorRepository jpa) { this.jpa = jpa; }

    @Override public Trabajador guardar(Trabajador trabajador) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(trabajador)));
    }

    @Override public Optional<Trabajador> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public Optional<Trabajador> buscarPorCedula(String cedula) {
        return jpa.findByCedula(cedula).map(WorkerMapper::toDomain);
    }

    @Override public List<Trabajador> listarTodos() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Trabajador> listarActivos() {
        return jpa.findByEstado(EstadoTrabajador.ACTIVO).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }

    @Override public boolean existePorCedula(String cedula) {
        return jpa.existsByCedula(cedula);
    }
}