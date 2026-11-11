package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.application.ports.out.ClavesPushRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class ClavesPushJdbc implements ClavesPushRepository {

    /** Siempre hay una sola fila */
    private static final int ID = 1;

    private final JdbcTemplate jdbc;

    public ClavesPushJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Guardadas> buscar() {
        return jdbc.query("SELECT publica, privada FROM claves_push WHERE id = ?",
                (rs, i) -> new Guardadas(rs.getString("publica"), rs.getString("privada")), ID).stream().findFirst();
    }

    @Override
    @Transactional
    public void guardar(String publica, String privadaCifrada) {
        jdbc.update("DELETE FROM claves_push WHERE id = ?", ID);
        jdbc.update("INSERT INTO claves_push (id, publica, privada, creada_en) VALUES (?, ?, ?, ?)",
                ID, publica, privadaCifrada, Timestamp.from(Instant.now()));
    }
}
