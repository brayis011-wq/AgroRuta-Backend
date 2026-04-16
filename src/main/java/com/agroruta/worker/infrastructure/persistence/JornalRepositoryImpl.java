package com.agroruta.worker.infrastructure.persistence;


import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.domain.JornalRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class JornalRepositoryImpl implements JornalRepository {

    private final JpaJornalRepository jpa;

    JornalRepositoryImpl(JpaJornalRepository jpa) { this.jpa = jpa; }

    @Override public Jornal guardar(Jornal jornal) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(jornal)));
    }

    @Override public Optional<Jornal> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Jornal> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findByTrabajadorIdAndFechaBetween(trabajadorId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorCultivo(Long cultivoId) {
        return jpa.findByCultivoId(cultivoId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarPorCultivoYPeriodo(Long cultivoId, LocalDate inicio, LocalDate fin) {
        return jpa.findByCultivoIdAndFechaBetween(cultivoId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarNoLiquidadosPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorIdAndLiquidadoFalse(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Jornal> buscarNoLiquidadosPorTrabajadorYPeriodo(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(trabajadorId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}
