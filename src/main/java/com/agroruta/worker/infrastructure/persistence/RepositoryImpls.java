package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.application.ports.*;
import com.agroruta.worker.domain.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// ── CargoRepositoryImpl ───────────────────────────────────────────────────────

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

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}

// ── ActividadRepositoryImpl ───────────────────────────────────────────────────

@Repository
class ActividadRepositoryImpl implements ActividadRepository {

    private final JpaActividadRepository jpa;

    ActividadRepositoryImpl(JpaActividadRepository jpa) { this.jpa = jpa; }

    @Override public Actividad guardar(Actividad actividad) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(actividad)));
    }

    @Override public Optional<Actividad> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Actividad> listarTodas() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Actividad> listarActivas() {
        return jpa.findByActivaTrue().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}

// ── TrabajadorRepositoryImpl ──────────────────────────────────────────────────

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

// ── JornalRepositoryImpl ──────────────────────────────────────────────────────

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

// ── NominaRepositoryImpl ──────────────────────────────────────────────────────

@Repository
class NominaRepositoryImpl implements NominaRepository {

    private final JpaNominaRepository jpa;

    NominaRepositoryImpl(JpaNominaRepository jpa) { this.jpa = jpa; }

    @Override public Nomina guardar(Nomina nomina) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(nomina)));
    }

    @Override public Optional<Nomina> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public List<Nomina> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> buscarPorTrabajadorYEstado(Long trabajadorId, EstadoNomina estado) {
        return jpa.findByTrabajadorIdAndEstado(trabajadorId, estado).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> buscarPorPeriodo(LocalDate inicio, LocalDate fin) {
        return jpa.findByPeriodo(inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Nomina> listarTodas() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }
}

// ── PagoRepositoryImpl ────────────────────────────────────────────────────────

@Repository
class PagoRepositoryImpl implements PagoRepository {

    private final JpaPagoRepository jpa;

    PagoRepositoryImpl(JpaPagoRepository jpa) { this.jpa = jpa; }

    @Override public Pago guardar(Pago pago) {
        return WorkerMapper.toDomain(jpa.save(WorkerMapper.toEntity(pago)));
    }

    @Override public Optional<Pago> buscarPorId(Long id) {
        return jpa.findById(id).map(WorkerMapper::toDomain);
    }

    @Override public Optional<Pago> buscarPorNomina(Long nominaId) {
        return jpa.findByNominaId(nominaId).map(WorkerMapper::toDomain);
    }

    @Override public List<Pago> buscarPorTrabajador(Long trabajadorId) {
        return jpa.findByTrabajadorId(trabajadorId).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override public List<Pago> listarTodos() {
        return jpa.findAll().stream().map(WorkerMapper::toDomain).collect(Collectors.toList());
    }
}
