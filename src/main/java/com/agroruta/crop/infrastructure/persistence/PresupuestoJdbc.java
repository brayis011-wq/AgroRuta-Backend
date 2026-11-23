package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.application.ports.out.PresupuestoRepository;
import com.agroruta.crop.domain.PresupuestoSiembra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PresupuestoJdbc implements PresupuestoRepository {

    private static final RowMapper<PresupuestoSiembra> FILA = (rs, i) -> new PresupuestoSiembra(
            rs.getLong("siembra_id"), rs.getBigDecimal("mano_obra"), rs.getBigDecimal("insumos"),
            rs.getBigDecimal("otros"), rs.getInt("avisado"));

    private final JdbcTemplate jdbc;

    public PresupuestoJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PresupuestoSiembra> deSiembra(Long siembraId) {
        return jdbc.query("SELECT siembra_id, mano_obra, insumos, otros, avisado FROM presupuestos_siembra WHERE siembra_id = ?",
                FILA, siembraId).stream().findFirst();
    }

    @Override
    public void guardar(PresupuestoSiembra p) {
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
        int cambiadas = jdbc.update("UPDATE presupuestos_siembra SET mano_obra = ?, insumos = ?, otros = ?, avisado = 0, "
                + "actualizado_en = ? WHERE siembra_id = ?", p.manoObra(), p.insumos(), p.otros(), ahora, p.siembraId());
        if (cambiadas == 0) {
            jdbc.update("INSERT INTO presupuestos_siembra (siembra_id, mano_obra, insumos, otros, avisado, actualizado_en) "
                    + "VALUES (?, ?, ?, ?, 0, ?)", p.siembraId(), p.manoObra(), p.insumos(), p.otros(), ahora);
        }
    }

    @Override
    public void borrar(Long siembraId) {
        jdbc.update("DELETE FROM presupuestos_siembra WHERE siembra_id = ?", siembraId);
    }

    @Override
    public void marcarAvisado(Long siembraId, int nivel) {
        jdbc.update("UPDATE presupuestos_siembra SET avisado = ? WHERE siembra_id = ? AND avisado < ?", nivel, siembraId, nivel);
    }

    @Override
    public List<ConLugar> activosDe(Long agricultorId) {
        return jdbc.query("""
                SELECT p.siembra_id, p.mano_obra, p.insumos, p.otros, p.avisado, f.nombre AS finca, l.nombre AS lote,
                       CONCAT(COALESCE(s.tipo_cultivo, ''), ' ', COALESCE(s.variedad, '')) AS cultivo
                  FROM presupuestos_siembra p
                  JOIN siembras s ON s.id = p.siembra_id
                  JOIN lotes l ON l.id = s.lote_id
                  JOIN fincas f ON f.id = l.finca_id
                 WHERE f.agricultor_id = ? AND s.estado_cultivo <> 'FINALIZADO' AND s.eliminado_en IS NULL
                """, (rs, i) -> new ConLugar(FILA.mapRow(rs, i), rs.getString("finca"), rs.getString("lote"),
                rs.getString("cultivo").trim()), agricultorId);
    }
}
