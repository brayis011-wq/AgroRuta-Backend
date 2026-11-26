package com.agroruta.finance.infrastructure.persistence;

import com.agroruta.finance.application.ports.out.PreciosReferenciaRepository;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PreciosReferenciaJdbc implements PreciosReferenciaRepository {

    private static final RowMapper<PrecioReferencia> PRECIO = (rs, i) -> new PrecioReferencia(
            rs.getString("producto"),
            rs.getDate("semana").toLocalDate(),
            rs.getBigDecimal("precio_kg"),
            rs.getBigDecimal("minimo_kg"),
            rs.getBigDecimal("maximo_kg"),
            rs.getInt("mercados"));

    private static final RowMapper<PreferenciaPrecio> PREFERENCIA = (rs, i) ->
            new PreferenciaPrecio(rs.getString("cultivo"), rs.getString("producto"), rs.getBigDecimal("precio_kg"));

    private final JdbcTemplate jdbc;

    public PreciosReferenciaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void guardar(List<PrecioReferencia> precios, LocalDateTime cuando) {
        Set<LocalDate> semanas = new TreeSet<>();
        precios.forEach(p -> semanas.add(p.semana()));
        for (LocalDate s : semanas) {
            jdbc.update("DELETE FROM precios_referencia WHERE semana = ?", Date.valueOf(s));
        }
        Timestamp ts = Timestamp.valueOf(cuando);
        jdbc.batchUpdate(
                "INSERT INTO precios_referencia (producto, semana, precio_kg, minimo_kg, maximo_kg, mercados, "
                        + "actualizado_en) VALUES (?, ?, ?, ?, ?, ?, ?)",
                precios,
                500,
                (ps, p) -> {
                    ps.setString(1, p.producto().length() > 120 ? p.producto().substring(0, 120) : p.producto());
                    ps.setDate(2, Date.valueOf(p.semana()));
                    ps.setBigDecimal(3, p.precioKg());
                    ps.setBigDecimal(4, p.minimoKg());
                    ps.setBigDecimal(5, p.maximoKg());
                    ps.setInt(6, p.mercados());
                    ps.setTimestamp(7, ts);
                });
        // Solo se guardan las últimas semanas: las más viejas ya no sirven de referencia
        if (!semanas.isEmpty()) {
            LocalDate primera = semanas.iterator().next();
            jdbc.update("DELETE FROM precios_referencia WHERE semana < ?", Date.valueOf(primera.minusWeeks(8)));
        }
    }

    @Override
    public List<PrecioReferencia> ultimaSemana() {
        return jdbc.query(
                "SELECT producto, semana, precio_kg, minimo_kg, maximo_kg, mercados FROM precios_referencia "
                        + "WHERE semana = (SELECT MAX(semana) FROM precios_referencia) ORDER BY producto",
                PRECIO);
    }

    @Override
    public Optional<LocalDateTime> ultimaActualizacion() {
        Timestamp t = jdbc.queryForObject("SELECT MAX(actualizado_en) FROM precios_referencia", Timestamp.class);
        return Optional.ofNullable(t).map(Timestamp::toLocalDateTime);
    }

    @Override
    public Optional<PreferenciaPrecio> preferencia(Long agricultorId, String cultivo) {
        return jdbc
                .query(
                        "SELECT cultivo, producto, precio_kg FROM precios_propios WHERE agricultor_id = ? AND cultivo = ?",
                        PREFERENCIA,
                        agricultorId,
                        cultivo)
                .stream()
                .findFirst();
    }

    @Override
    public List<PreferenciaPrecio> preferencias(Long agricultorId) {
        return jdbc.query(
                "SELECT cultivo, producto, precio_kg FROM precios_propios WHERE agricultor_id = ? ORDER BY cultivo",
                PREFERENCIA,
                agricultorId);
    }

    @Override
    public void guardarPreferencia(Long agricultorId, PreferenciaPrecio p, LocalDateTime cuando) {
        int cambiadas = jdbc.update(
                "UPDATE precios_propios SET producto = ?, precio_kg = ?, actualizado_en = ? "
                        + "WHERE agricultor_id = ? AND cultivo = ?",
                p.producto(),
                p.precioKg(),
                Timestamp.valueOf(cuando),
                agricultorId,
                p.cultivo());
        if (cambiadas == 0) {
            jdbc.update(
                    "INSERT INTO precios_propios (agricultor_id, cultivo, producto, precio_kg, actualizado_en) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    agricultorId,
                    p.cultivo(),
                    p.producto(),
                    p.precioKg(),
                    Timestamp.valueOf(cuando));
        }
    }

    @Override
    public void borrarPreferencia(Long agricultorId, String cultivo) {
        jdbc.update("DELETE FROM precios_propios WHERE agricultor_id = ? AND cultivo = ?", agricultorId, cultivo);
    }
}
