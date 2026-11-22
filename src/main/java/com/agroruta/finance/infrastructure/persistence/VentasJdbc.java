package com.agroruta.finance.infrastructure.persistence;

import com.agroruta.finance.application.ports.out.VentasRepository;
import com.agroruta.finance.domain.Venta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VentasJdbc implements VentasRepository {

    /** Con lo abonado y el nombre de la siembra (cultivo y lote) */
    private static final String SELECT = """
            SELECT v.id, v.siembra_id, v.comprador, v.fecha, v.producto, v.cantidad_kg, v.precio_kg, v.total, v.vence,
                   v.observaciones,
                   (SELECT COALESCE(SUM(a.monto), 0) FROM abonos_venta a WHERE a.venta_id = v.id) AS abonado,
                   CASE WHEN s.id IS NULL THEN NULL
                        ELSE CONCAT(COALESCE(s.tipo_cultivo, ''), ' ', COALESCE(s.variedad, ''), ' · ', l.nombre) END AS siembra_nombre
              FROM ventas v
              LEFT JOIN siembras s ON s.id = v.siembra_id
              LEFT JOIN lotes l ON l.id = s.lote_id""";

    private static final RowMapper<Venta> FILA = (rs, i) -> {
        Date vence = rs.getDate("vence");
        long siembra = rs.getLong("siembra_id");
        return new Venta(rs.getLong("id"), rs.wasNull() ? null : siembra, rs.getString("siembra_nombre"),
                rs.getString("comprador"), rs.getDate("fecha").toLocalDate(), rs.getString("producto"),
                rs.getBigDecimal("cantidad_kg"), rs.getBigDecimal("precio_kg"), rs.getBigDecimal("total"),
                vence == null ? null : vence.toLocalDate(), rs.getString("observaciones"), rs.getBigDecimal("abonado"),
                List.of());
    };

    private static final RowMapper<Venta.Abono> ABONO = (rs, i) -> new Venta.Abono(rs.getLong("id"),
            rs.getLong("venta_id"), rs.getDate("fecha").toLocalDate(), rs.getBigDecimal("monto"),
            rs.getString("metodo"), rs.getString("observaciones"));

    private final JdbcTemplate jdbc;

    public VentasJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Venta crear(Long agricultorId, Venta v) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO ventas (agricultor_id, siembra_id, comprador, fecha, "
                    + "producto, cantidad_kg, precio_kg, total, vence, observaciones, creado_en) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, agricultorId);
            if (v.siembraId() == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, v.siembraId());
            ps.setString(3, v.comprador());
            ps.setDate(4, Date.valueOf(v.fecha()));
            ps.setString(5, v.producto());
            ps.setBigDecimal(6, v.cantidadKg());
            ps.setBigDecimal(7, v.precioKg());
            ps.setBigDecimal(8, v.total());
            if (v.vence() == null) ps.setNull(9, Types.DATE); else ps.setDate(9, Date.valueOf(v.vence()));
            ps.setString(10, v.observaciones());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, clave);
        Number id = clave.getKey();
        return buscarPorId(id == null ? null : id.longValue()).orElse(v);
    }

    @Override
    public void actualizar(Venta v) {
        jdbc.update("UPDATE ventas SET siembra_id = ?, comprador = ?, fecha = ?, producto = ?, cantidad_kg = ?, "
                        + "precio_kg = ?, total = ?, vence = ?, observaciones = ? WHERE id = ?",
                v.siembraId(), v.comprador(), Date.valueOf(v.fecha()), v.producto(), v.cantidadKg(), v.precioKg(),
                v.total(), v.vence() == null ? null : Date.valueOf(v.vence()), v.observaciones(), v.id());
    }

    @Override
    public void eliminar(Long id) {
        jdbc.update("DELETE FROM abonos_venta WHERE venta_id = ?", id);
        jdbc.update("DELETE FROM ventas WHERE id = ?", id);
    }

    @Override
    public Optional<Venta> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.query(SELECT + " WHERE v.id = ?", FILA, id).stream().findFirst().map(v -> new Venta(v.id(),
                v.siembraId(), v.siembraNombre(), v.comprador(), v.fecha(), v.producto(), v.cantidadKg(), v.precioKg(),
                v.total(), v.vence(), v.observaciones(), v.abonado(),
                jdbc.query("SELECT id, venta_id, fecha, monto, metodo, observaciones FROM abonos_venta "
                        + "WHERE venta_id = ? ORDER BY fecha, id", ABONO, id)));
    }

    @Override
    public List<Venta> listarPorSiembra(Long siembraId) {
        return jdbc.query(SELECT + " WHERE v.siembra_id = ? ORDER BY v.fecha, v.id", FILA, siembraId);
    }

    @Override
    public List<com.agroruta.finance.domain.KilosEnBodega> kilosEnBodega(Long agricultorId) {
        return agricultorId == null ? bodega("", " LIMIT 300") : bodega(" AND f.agricultor_id = ?", " LIMIT 300", agricultorId);
    }

    @Override
    public java.util.Optional<com.agroruta.finance.domain.KilosEnBodega> kilosDeSiembra(Long siembraId) {
        return bodega(" AND s.id = ?", "", siembraId).stream().findFirst();
    }

    /** Siembras con cosechas: kilos recogidos y vendidos (filtro: condición extra del WHERE) */
    private List<com.agroruta.finance.domain.KilosEnBodega> bodega(String filtro, String limite, Object... args) {
        String sql = """
                SELECT s.id, s.tipo_cultivo, s.variedad, l.nombre AS lote, f.nombre AS finca, s.estado_cultivo,
                       c.kg AS cosechados, c.ultima, COALESCE(v.kg, 0) AS vendidos
                  FROM siembras s
                  JOIN lotes l ON l.id = s.lote_id
                  JOIN fincas f ON f.id = l.finca_id
                  JOIN (SELECT siembra_id, SUM(cantidad_kg) AS kg, MAX(fecha) AS ultima FROM cosechas GROUP BY siembra_id) c
                    ON c.siembra_id = s.id
                  LEFT JOIN (SELECT siembra_id, SUM(cantidad_kg) AS kg FROM ventas
                              WHERE siembra_id IS NOT NULL AND cantidad_kg IS NOT NULL GROUP BY siembra_id) v
                    ON v.siembra_id = s.id
                 WHERE s.eliminado_en IS NULL""";
        return jdbc.query(sql + filtro + " ORDER BY c.ultima DESC, s.id DESC" + limite, (rs, i) -> new com.agroruta.finance.domain.KilosEnBodega(
                rs.getLong("id"), rs.getString("tipo_cultivo"), rs.getString("variedad"), rs.getString("lote"),
                rs.getString("finca"), BigDecimal.valueOf(rs.getDouble("cosechados")).setScale(2, java.math.RoundingMode.HALF_UP),
                rs.getBigDecimal("vendidos"), rs.getDate("ultima").toLocalDate(),
                "FINALIZADO".equals(rs.getString("estado_cultivo"))), args);
    }

    @Override
    public List<Venta> listar(Long agricultorId, LocalDate desde, LocalDate hasta) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (agricultorId != null) { w.append(" AND v.agricultor_id = ?"); args.add(agricultorId); }
        if (desde != null) { w.append(" AND v.fecha >= ?"); args.add(Date.valueOf(desde)); }
        if (hasta != null) { w.append(" AND v.fecha <= ?"); args.add(Date.valueOf(hasta)); }
        return jdbc.query(SELECT + w + " ORDER BY v.fecha DESC, v.id DESC LIMIT 1000", FILA, args.toArray());
    }

    @Override
    public Venta.Abono agregarAbono(Venta.Abono a) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO abonos_venta (venta_id, fecha, monto, metodo, observaciones) "
                    + "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.ventaId());
            ps.setDate(2, Date.valueOf(a.fecha()));
            ps.setBigDecimal(3, a.monto());
            ps.setString(4, a.metodo());
            ps.setString(5, a.observaciones());
            return ps;
        }, clave);
        Number id = clave.getKey();
        return new Venta.Abono(id == null ? null : id.longValue(), a.ventaId(), a.fecha(), a.monto(), a.metodo(), a.observaciones());
    }

    @Override
    public void eliminarAbono(Long ventaId, Long abonoId) {
        jdbc.update("DELETE FROM abonos_venta WHERE id = ? AND venta_id = ?", abonoId, ventaId);
    }

    @Override
    public BigDecimal cobradoEntre(Long agricultorId, LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(a.monto), 0) FROM abonos_venta a "
                + "JOIN ventas v ON v.id = a.venta_id WHERE a.fecha BETWEEN ? AND ?");
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(desde), Date.valueOf(hasta)));
        if (agricultorId != null) { sql.append(" AND v.agricultor_id = ?"); args.add(agricultorId); }
        return jdbc.queryForObject(sql.toString(), BigDecimal.class, args.toArray());
    }

    @Override
    public List<String> compradores(Long agricultorId) {
        if (agricultorId == null) return List.of();
        return jdbc.query("SELECT comprador FROM ventas WHERE agricultor_id = ? GROUP BY comprador "
                + "ORDER BY MAX(fecha) DESC LIMIT 50", (rs, i) -> rs.getString(1), agricultorId);
    }
}
