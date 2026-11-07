package com.agroruta.admin.infrastructure.persistence;

import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.infrastructure.persistence.FincaEntity;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.EstadoTrabajador;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Consultas de solo lectura para el panel de administración.
 * Spring Data valida estas consultas al arrancar la aplicación.
 */
public interface AdminEstadisticasJpaRepository extends Repository<FincaEntity, Long> {

    // Fincas, lotes y siembras eliminadas (borrado lógico) no cuentan en el panel

    @Query("SELECT COUNT(f) FROM FincaEntity f WHERE f.eliminadoEn IS NULL")
    long contarFincas();

    @Query("SELECT COALESCE(SUM(f.hectareas), 0.0) FROM FincaEntity f WHERE f.eliminadoEn IS NULL")
    Double sumarHectareas();

    @Query("SELECT COUNT(l) FROM LoteEntity l WHERE l.eliminadoEn IS NULL")
    long contarLotes();

    @Query("SELECT COUNT(s) FROM SiembraEntity s WHERE s.estadoCultivo <> :finalizado AND s.eliminadoEn IS NULL")
    long contarSiembrasActivas(@Param("finalizado") EstadoCultivo finalizado);

    @Query("SELECT COALESCE(SUM(c.cantidadKg), 0.0) FROM CosechaEntity c")
    Double sumarCosechaKg();

    @Query("SELECT COUNT(t) FROM TrabajadorEntity t WHERE t.estado = :estado")
    long contarTrabajadores(@Param("estado") EstadoTrabajador estado);

    @Query("SELECT COUNT(n) FROM NominaEntity n WHERE n.estado IN :estados")
    long contarNominas(@Param("estados") Collection<EstadoNomina> estados);

    @Query("SELECT SUM(p.monto) FROM PagoEntity p")
    java.math.BigDecimal sumarPagos();

    // ── Agrupados por agricultor: cada fila es [agricultorId, valor(es)] ──

    @Query("SELECT f.agricultorId, COUNT(f), COALESCE(SUM(f.hectareas), 0.0) "
            + "FROM FincaEntity f WHERE f.eliminadoEn IS NULL GROUP BY f.agricultorId")
    List<Object[]> fincasPorAgricultor();

    @Query("SELECT t.agricultorId, COUNT(t) FROM TrabajadorEntity t "
            + "WHERE t.estado = :estado GROUP BY t.agricultorId")
    List<Object[]> trabajadoresPorAgricultor(@Param("estado") EstadoTrabajador estado);

    @Query("SELECT n.trabajador.agricultorId, COUNT(n) FROM NominaEntity n "
            + "GROUP BY n.trabajador.agricultorId")
    List<Object[]> nominasPorAgricultor();

    @Query("SELECT p.nomina.trabajador.agricultorId, SUM(p.monto) FROM PagoEntity p "
            + "GROUP BY p.nomina.trabajador.agricultorId")
    List<Object[]> pagosPorAgricultor();
}
