package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoTrabajador;
import com.agroruta.worker.domain.ResumenJornales;
import com.agroruta.worker.domain.ResumenJornalesInicio;
import java.math.BigDecimal;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import com.agroruta.shared.util.Pagina;

import com.agroruta.worker.domain.Jornal;
import com.agroruta.worker.application.ports.out.JornalRepository;
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
    @Override
    public List<Jornal> buscarDisponiblesParaNomina(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return jpa.findDisponiblesParaNomina(trabajadorId, inicio, fin)
                .stream()
                .map(WorkerMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override public void eliminar(Long id) { jpa.deleteById(id); }

    @Override
    public List<Jornal> buscarDelEquipo(Long agricultorId, LocalDate inicio, LocalDate fin) {
        return jpa.delEquipoEntre(agricultorId, inicio, fin).stream()
                .map(WorkerMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public java.util.Map<Long, com.agroruta.worker.domain.EstadoNomina> estadosDeNomina(java.util.Collection<Long> ids) {
        java.util.Map<Long, com.agroruta.worker.domain.EstadoNomina> res = new java.util.HashMap<>();
        if (ids == null || ids.isEmpty()) return res;
        for (Object[] f : jpa.estadosDeNomina(ids)) res.put((Long) f[0], (com.agroruta.worker.domain.EstadoNomina) f[1]);
        return res;
    }

    @Override
    public java.util.Set<Long> idsEnNomina(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Set.of();
        return new java.util.HashSet<>(jpa.idsEnNomina(ids));
    }

    @Override
    public Pagina<Jornal> paginaPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano) {
        var orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        return aPagina(jpa.paginaPorTrabajador(trabajadorId, inicio, fin, PageRequest.of(pagina, tamano, orden)),
                WorkerMapper::toDomain);
    }

    @Override
    public Pagina<Jornal> paginaDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin, int pagina, int tamano) {
        var orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        return aPagina(jpa.paginaDelEquipo(agricultorId, trabajadorId, inicio, fin, PageRequest.of(pagina, tamano, orden)),
                WorkerMapper::toDomain);
    }

    @Override
    public ResumenJornales resumenDelEquipo(Long agricultorId, Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return resumenDe(jpa.resumenDelEquipo(agricultorId, trabajadorId, inicio, fin));
    }

    @Override
    public ResumenJornales resumenPorTrabajador(Long trabajadorId, LocalDate inicio, LocalDate fin) {
        return resumenDe(jpa.resumenPorTrabajador(trabajadorId, inicio, fin));
    }

    private ResumenJornales resumenDe(List<Object[]> filas) {
        Object[] f = filas.isEmpty() ? new Object[6] : filas.get(0);
        return new ResumenJornales(
                f[0] == null ? 0 : ((Number) f[0]).longValue(), decimal(f[1]),
                f[2] == null ? 0 : ((Number) f[2]).longValue(), decimal(f[3]),
                (LocalDate) f[4], (LocalDate) f[5]);
    }

    @Override
    public ResumenJornalesInicio resumenInicio(Long agricultorId, LocalDate hoy) {
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        List<Object[]> filas = jpa.conteoPorMes(agricultorId, EstadoTrabajador.ACTIVO, inicioMes.minusMonths(1), inicioMes, hoy);
        // Sin jornales en el rango, SUM devuelve null
        Object[] f = filas.isEmpty() || filas.get(0) == null ? new Object[2] : filas.get(0);
        return new ResumenJornalesInicio(
                f[0] == null ? 0 : ((Number) f[0]).longValue(),
                f[1] == null ? 0 : ((Number) f[1]).longValue(),
                jpa.trabajadoresConJornal(agricultorId, EstadoTrabajador.ACTIVO, hoy));
    }

    private static <E, D> Pagina<D> aPagina(Page<E> p, java.util.function.Function<E, D> f) {
        return new Pagina<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }

    private static BigDecimal decimal(Object o) {
        return o == null ? BigDecimal.ZERO : new BigDecimal(o.toString());
    }
}
