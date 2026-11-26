package com.agroruta.finance.infrastructure.persistence;

import com.agroruta.finance.application.ports.out.GastosFincaRepository;
import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.ResumenGastos;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class GastosFincaJdbc implements GastosFincaRepository {

    private static final String SELECT = "SELECT g.id, g.finca_id, f.nombre AS finca_nombre, g.fecha, g.categoria, "
            + "g.descripcion, g.monto FROM gastos_finca g JOIN fincas f ON f.id = g.finca_id";

    private static final RowMapper<GastoFinca> FILA = (rs, i) -> new GastoFinca(
            rs.getLong("id"),
            rs.getLong("finca_id"),
            rs.getString("finca_nombre"),
            rs.getDate("fecha").toLocalDate(),
            categoria(rs.getString("categoria")),
            rs.getString("descripcion"),
            rs.getBigDecimal("monto"));

    private final JdbcTemplate jdbc;

    public GastosFincaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public GastoFinca crear(GastoFinca g) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO gastos_finca (finca_id, fecha, categoria, descripcion, "
                                    + "monto, creado_en) VALUES (?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, g.fincaId());
                    ps.setDate(2, Date.valueOf(g.fecha()));
                    ps.setString(3, g.categoria().name());
                    ps.setString(4, g.descripcion());
                    ps.setBigDecimal(5, g.monto());
                    ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
                    return ps;
                },
                clave);
        Number id = clave.getKey();
        return buscarPorId(id == null ? null : id.longValue()).orElse(g);
    }

    @Override
    public GastoFinca actualizar(GastoFinca g) {
        jdbc.update(
                "UPDATE gastos_finca SET finca_id = ?, fecha = ?, categoria = ?, descripcion = ?, monto = ? WHERE id = ?",
                g.fincaId(),
                Date.valueOf(g.fecha()),
                g.categoria().name(),
                g.descripcion(),
                g.monto(),
                g.id());
        return buscarPorId(g.id()).orElse(g);
    }

    @Override
    public void eliminar(Long id) {
        jdbc.update("DELETE FROM gastos_finca WHERE id = ?", id);
    }

    @Override
    public Optional<GastoFinca> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return jdbc.query(SELECT + " WHERE g.id = ?", FILA, id).stream().findFirst();
    }

    @Override
    public List<GastoFinca> listar(Long agricultorId, Long fincaId, LocalDate desde, LocalDate hasta) {
        List<Object> args = new ArrayList<>();
        String where = filtro(agricultorId, fincaId, desde, hasta, args);
        return jdbc.query(SELECT + where + " ORDER BY g.fecha DESC, g.id DESC LIMIT 500", FILA, args.toArray());
    }

    @Override
    public ResumenGastos resumen(Long agricultorId, LocalDate desde, LocalDate hasta) {
        List<Object> args = new ArrayList<>();
        String where = filtro(agricultorId, null, desde, hasta, args);
        List<ResumenGastos.Parte> categorias = jdbc.query(
                "SELECT g.categoria AS clave, NULL AS nombre, SUM(g.monto) AS total, "
                        + "COUNT(*) AS cantidad FROM gastos_finca g JOIN fincas f ON f.id = g.finca_id" + where
                        + " GROUP BY g.categoria ORDER BY total DESC",
                PARTE,
                args.toArray());
        List<ResumenGastos.Parte> fincas = jdbc.query(
                "SELECT CAST(g.finca_id AS CHAR) AS clave, MAX(f.nombre) AS nombre, "
                        + "SUM(g.monto) AS total, COUNT(*) AS cantidad FROM gastos_finca g JOIN fincas f ON f.id = g.finca_id"
                        + where + " GROUP BY g.finca_id ORDER BY total DESC",
                PARTE,
                args.toArray());
        BigDecimal total = categorias.stream().map(ResumenGastos.Parte::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        int cantidad =
                categorias.stream().mapToInt(ResumenGastos.Parte::cantidad).sum();
        return new ResumenGastos(total, cantidad, categorias, fincas);
    }

    private static final RowMapper<ResumenGastos.Parte> PARTE = (rs, i) -> new ResumenGastos.Parte(
            rs.getString("clave"), rs.getString("nombre"), rs.getBigDecimal("total"), rs.getInt("cantidad"));

    private static String filtro(Long agricultorId, Long fincaId, LocalDate desde, LocalDate hasta, List<Object> args) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        if (agricultorId != null) {
            w.append(" AND f.agricultor_id = ?");
            args.add(agricultorId);
        }
        if (fincaId != null) {
            w.append(" AND g.finca_id = ?");
            args.add(fincaId);
        }
        if (desde != null) {
            w.append(" AND g.fecha >= ?");
            args.add(Date.valueOf(desde));
        }
        if (hasta != null) {
            w.append(" AND g.fecha <= ?");
            args.add(Date.valueOf(hasta));
        }
        return w.toString();
    }

    private static GastoFinca.Categoria categoria(String v) {
        try {
            return GastoFinca.Categoria.valueOf(v);
        } catch (RuntimeException e) {
            return GastoFinca.Categoria.OTRO;
        }
    }
}
