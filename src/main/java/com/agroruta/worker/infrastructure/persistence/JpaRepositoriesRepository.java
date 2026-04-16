package com.agroruta.worker.infrastructure.persistence;

import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.EstadoTrabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// ── Cargo ────────────────────────────────────────────────────────────────────

interface JpaCargoRepository extends JpaRepository<CargoEntity, Long> {
    List<CargoEntity> findByActivoTrue();
}

// ── Actividad ─────────────────────────────────────────────────────────────────

interface JpaActividadRepository extends JpaRepository<ActividadEntity, Long> {
    List<ActividadEntity> findByActivaTrue();
}

// ── Trabajador ────────────────────────────────────────────────────────────────

interface JpaTrabajadorRepository extends JpaRepository<TrabajadorEntity, Long> {
    Optional<TrabajadorEntity> findByCedula(String cedula);
    boolean existsByCedula(String cedula);
    List<TrabajadorEntity> findByEstado(EstadoTrabajador estado);
}

// ── Jornal ────────────────────────────────────────────────────────────────────

interface JpaJornalRepository extends JpaRepository<JornalEntity, Long> {

    List<JornalEntity> findByTrabajadorId(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByCultivoId(Long cultivoId);

    List<JornalEntity> findByCultivoIdAndFechaBetween(
            Long cultivoId, LocalDate inicio, LocalDate fin);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalse(Long trabajadorId);

    List<JornalEntity> findByTrabajadorIdAndLiquidadoFalseAndFechaBetween(
            Long trabajadorId, LocalDate inicio, LocalDate fin);
}

// ── Nomina ────────────────────────────────────────────────────────────────────

interface JpaNominaRepository extends JpaRepository<NominaEntity, Long> {

    List<NominaEntity> findByTrabajadorId(Long trabajadorId);

    List<NominaEntity> findByTrabajadorIdAndEstado(Long trabajadorId, EstadoNomina estado);

    @Query("SELECT n FROM NominaEntity n WHERE n.periodoInicio >= :inicio AND n.periodoFin <= :fin")
    List<NominaEntity> findByPeriodo(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}

// ── Pago ──────────────────────────────────────────────────────────────────────

interface JpaPagoRepository extends JpaRepository<PagoEntity, Long> {
    Optional<PagoEntity> findByNominaId(Long nominaId);

    @Query("SELECT p FROM PagoEntity p WHERE p.nomina.trabajador.id = :trabajadorId")
    List<PagoEntity> findByTrabajadorId(@Param("trabajadorId") Long trabajadorId);
}
