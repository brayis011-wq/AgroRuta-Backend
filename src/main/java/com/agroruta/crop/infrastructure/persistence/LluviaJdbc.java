package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.application.ports.out.LluviaRepository;
import com.agroruta.crop.domain.Lluvia;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class LluviaJdbc implements LluviaRepository {

    private final JdbcTemplate jdbc;

    public LluviaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Lluvia> deFinca(Long fincaId, LocalDate desde, LocalDate hasta) {
        return jdbc.query("SELECT fecha, milimetros FROM lluvias WHERE finca_id = ? AND fecha BETWEEN ? AND ? ORDER BY fecha",
                (rs, i) -> new Lluvia(rs.getDate("fecha").toLocalDate(), rs.getBigDecimal("milimetros")),
                fincaId, Date.valueOf(desde), Date.valueOf(hasta));
    }

    @Override
    public void anotar(Long fincaId, LocalDate fecha, BigDecimal milimetros) {
        int cambiadas = jdbc.update("UPDATE lluvias SET milimetros = ? WHERE finca_id = ? AND fecha = ?",
                milimetros, fincaId, Date.valueOf(fecha));
        if (cambiadas == 0) {
            jdbc.update("INSERT INTO lluvias (finca_id, fecha, milimetros) VALUES (?, ?, ?)",
                    fincaId, Date.valueOf(fecha), milimetros);
        }
    }

    @Override
    public void borrar(Long fincaId, LocalDate fecha) {
        jdbc.update("DELETE FROM lluvias WHERE finca_id = ? AND fecha = ?", fincaId, Date.valueOf(fecha));
    }
}
