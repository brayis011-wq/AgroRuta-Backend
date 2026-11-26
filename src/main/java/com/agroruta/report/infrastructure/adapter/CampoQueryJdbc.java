package com.agroruta.report.infrastructure.adapter;

import com.agroruta.report.application.ports.out.CampoQueryPort;
import com.agroruta.report.domain.AplicacionCampo;
import com.agroruta.report.domain.GastosGenerales;
import com.agroruta.report.domain.Periodo;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas de solo lectura para el cuaderno de campo y los gastos generales. */
@Repository
public class CampoQueryJdbc implements CampoQueryPort {

    private final JdbcTemplate jdbc;

    public CampoQueryJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AplicacionCampo> aplicaciones(Long agricultorId, Long fincaId, Periodo periodo) {
        StringBuilder sql = new StringBuilder("""
                SELECT fu.fecha, f.nombre AS finca, l.nombre AS lote,
                       CONCAT(COALESCE(s.tipo_cultivo, ''), ' ', COALESCE(s.variedad, '')) AS cultivo,
                       fu.producto, ai.tipo AS tipo_producto, fu.dosis, fu.unidad_medida, fu.area_aplicada,
                       fu.cantidad_usada, fu.unidad_cantidad, fu.dias_carencia, fu.reentrada_horas, fu.observaciones
                  FROM fumigaciones fu
                  JOIN siembras s ON s.id = fu.siembra_id
                  JOIN lotes l ON l.id = s.lote_id
                  JOIN fincas f ON f.id = l.finca_id
                  LEFT JOIN agricultural_inputs ai ON ai.id = fu.agricultural_input_id
                 WHERE 1 = 1""");
        List<Object> args = new ArrayList<>();
        if (agricultorId != null) {
            sql.append(" AND f.agricultor_id = ?");
            args.add(agricultorId);
        }
        if (fincaId != null) {
            sql.append(" AND f.id = ?");
            args.add(fincaId);
        }
        if (periodo != null && periodo.desde() != null) {
            sql.append(" AND fu.fecha >= ?");
            args.add(Date.valueOf(periodo.desde()));
        }
        if (periodo != null && periodo.hasta() != null) {
            sql.append(" AND fu.fecha <= ?");
            args.add(Date.valueOf(periodo.hasta()));
        }
        sql.append(" ORDER BY fu.fecha, f.nombre, l.nombre, fu.id LIMIT 5000");
        return jdbc.query(
                sql.toString(),
                (rs, i) -> new AplicacionCampo(
                        rs.getDate("fecha").toLocalDate(),
                        rs.getString("finca"),
                        rs.getString("lote"),
                        rs.getString("cultivo").trim(),
                        rs.getString("producto"),
                        rs.getString("tipo_producto"),
                        numero(rs.getObject("dosis")),
                        rs.getString("unidad_medida"),
                        numero(rs.getObject("area_aplicada")),
                        numero(rs.getObject("cantidad_usada")),
                        rs.getString("unidad_cantidad"),
                        entero(rs.getObject("dias_carencia")),
                        entero(rs.getObject("reentrada_horas")),
                        rs.getString("observaciones")),
                args.toArray());
    }

    @Override
    public String nombreFinca(Long fincaId) {
        if (fincaId == null) return null;
        return jdbc.query("SELECT nombre FROM fincas WHERE id = ?", (rs, i) -> rs.getString(1), fincaId).stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public GastosGenerales gastosGenerales(Long agricultorId, Periodo periodo) {
        StringBuilder sql = new StringBuilder("SELECT g.categoria, SUM(g.monto) FROM gastos_finca g "
                + "JOIN fincas f ON f.id = g.finca_id WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (agricultorId != null) {
            sql.append(" AND f.agricultor_id = ?");
            args.add(agricultorId);
        }
        if (periodo != null && periodo.desde() != null) {
            sql.append(" AND g.fecha >= ?");
            args.add(Date.valueOf(periodo.desde()));
        }
        if (periodo != null && periodo.hasta() != null) {
            sql.append(" AND g.fecha <= ?");
            args.add(Date.valueOf(periodo.hasta()));
        }
        sql.append(" GROUP BY g.categoria ORDER BY SUM(g.monto) DESC");
        List<GastosGenerales.Parte> partes = jdbc.query(
                sql.toString(),
                (rs, i) -> new GastosGenerales.Parte(rs.getString(1), pesos(rs.getBigDecimal(2))),
                args.toArray());
        double total = partes.stream().mapToDouble(GastosGenerales.Parte::total).sum();
        return new GastosGenerales(total, partes);
    }

    private static double pesos(BigDecimal v) {
        return v == null ? 0 : v.setScale(0, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private static Double numero(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private static Integer entero(Object o) {
        return o instanceof Number n ? n.intValue() : null;
    }
}
