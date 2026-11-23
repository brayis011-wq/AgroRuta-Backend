package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.application.ports.out.ActividadProgramadaRepository;
import com.agroruta.crop.domain.ActividadProgramada;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ActividadProgramadaJdbc implements ActividadProgramadaRepository {

    private static final String SELECT = "SELECT id, siembra_id, tipo, descripcion, cada_dias, desde, hasta "
            + "FROM actividades_programadas";

    private static final RowMapper<ActividadProgramada> FILA = (rs, i) -> {
        Date hasta = rs.getDate("hasta");
        return new ActividadProgramada(rs.getLong("id"), rs.getLong("siembra_id"), rs.getString("tipo"),
                rs.getString("descripcion"), rs.getInt("cada_dias"), rs.getDate("desde").toLocalDate(),
                hasta == null ? null : hasta.toLocalDate());
    };

    private final JdbcTemplate jdbc;

    public ActividadProgramadaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ActividadProgramada guardar(ActividadProgramada p) {
        if (p.id() != null) {
            jdbc.update("UPDATE actividades_programadas SET tipo = ?, descripcion = ?, cada_dias = ?, desde = ?, hasta = ? "
                            + "WHERE id = ?", p.tipo(), p.descripcion(), p.cadaDias(), Date.valueOf(p.desde()),
                    p.hasta() == null ? null : Date.valueOf(p.hasta()), p.id());
            return buscarPorId(p.id()).orElse(p);
        }
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("INSERT INTO actividades_programadas (siembra_id, tipo, descripcion, "
                    + "cada_dias, desde, hasta, creado_en) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, p.siembraId());
            ps.setString(2, p.tipo());
            ps.setString(3, p.descripcion());
            ps.setInt(4, p.cadaDias());
            ps.setDate(5, Date.valueOf(p.desde()));
            if (p.hasta() == null) ps.setNull(6, Types.DATE); else ps.setDate(6, Date.valueOf(p.hasta()));
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps;
        }, clave);
        Number id = clave.getKey();
        return new ActividadProgramada(id == null ? null : id.longValue(), p.siembraId(), p.tipo(), p.descripcion(),
                p.cadaDias(), p.desde(), p.hasta());
    }

    @Override
    public Optional<ActividadProgramada> buscarPorId(Long id) {
        return jdbc.query(SELECT + " WHERE id = ?", FILA, id).stream().findFirst();
    }

    @Override
    public List<ActividadProgramada> deSiembra(Long siembraId) {
        return jdbc.query(SELECT + " WHERE siembra_id = ? ORDER BY desde, id", FILA, siembraId);
    }

    @Override
    public void eliminar(Long id) {
        jdbc.update("DELETE FROM actividades_programadas WHERE id = ?", id);
    }

    @Override
    public Map<String, List<LocalDate>> fechasHechas(Long siembraId) {
        Map<String, List<LocalDate>> res = new HashMap<>();
        // El abonado cumple también lo programado como FERTILIZACION
        jdbc.query("SELECT CASE WHEN tipo = 'ABONADO' THEN 'FERTILIZACION' ELSE tipo END, fecha FROM actividades_cultivo WHERE siembra_id = ? "
                        + "UNION ALL "
                        + "SELECT CASE WHEN ai.tipo LIKE 'FERTILIZANTE%' THEN 'FERTILIZACION' ELSE 'FUMIGACION' END, f.fecha "
                        + "FROM fumigaciones f LEFT JOIN agricultural_inputs ai ON ai.id = f.agricultural_input_id "
                        + "WHERE f.siembra_id = ?",
                rs -> {
                    res.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getDate(2).toLocalDate());
                }, siembraId, siembraId);
        return res;
    }
}
