package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.application.ports.out.BandejaAvisosRepository;
import com.agroruta.notification.domain.AvisoBandeja;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class BandejaAvisosJdbc implements BandejaAvisosRepository {

    private static final Logger log = LoggerFactory.getLogger(BandejaAvisosJdbc.class);
    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};
    private static final int MAX_TITULO = 120;
    private static final int MAX_CUERPO = 400;
    private static final int MAX_DATOS = 1000;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public BandejaAvisosJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(AvisoBandeja.Nuevo a, Instant ahora) {
        // Una sola sentencia (MySQL y TiDB): si la etiqueta ya existe para esa persona, se actualiza y vuelve a quedar sin leer
        jdbc.update("INSERT INTO avisos_bandeja (usuario_id, tipo, titulo, cuerpo, datos, ruta, etiqueta, creado_en, leido_en) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL) "
                        + "ON DUPLICATE KEY UPDATE tipo = VALUES(tipo), titulo = VALUES(titulo), cuerpo = VALUES(cuerpo), "
                        + "datos = VALUES(datos), ruta = VALUES(ruta), creado_en = VALUES(creado_en), leido_en = NULL",
                a.usuarioId(), a.tipo().name(), recortar(a.titulo(), MAX_TITULO), recortar(a.cuerpo(), MAX_CUERPO),
                datosJson(a.datos()), a.ruta(), a.etiqueta(), Timestamp.from(ahora));
    }

    @Override
    public List<AvisoBandeja> recientes(Long usuarioId, int limite) {
        return jdbc.query("SELECT id, tipo, titulo, cuerpo, datos, ruta, creado_en, leido_en FROM avisos_bandeja "
                        + "WHERE usuario_id = ? ORDER BY creado_en DESC, id DESC LIMIT ?",
                (rs, i) -> leer(rs), usuarioId, limite);
    }

    @Override
    public int noLeidos(Long usuarioId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM avisos_bandeja WHERE usuario_id = ? AND leido_en IS NULL",
                Integer.class, usuarioId);
        return n == null ? 0 : n;
    }

    @Override
    public boolean marcarLeido(Long usuarioId, Long avisoId, Instant ahora) {
        // COALESCE: si ya estaba leído se conserva la primera fecha
        return jdbc.update("UPDATE avisos_bandeja SET leido_en = COALESCE(leido_en, ?) WHERE id = ? AND usuario_id = ?",
                Timestamp.from(ahora), avisoId, usuarioId) > 0;
    }

    @Override
    public int marcarTodosLeidos(Long usuarioId, Instant ahora) {
        return jdbc.update("UPDATE avisos_bandeja SET leido_en = ? WHERE usuario_id = ? AND leido_en IS NULL",
                Timestamp.from(ahora), usuarioId);
    }

    private AvisoBandeja leer(ResultSet rs) throws SQLException {
        Timestamp leido = rs.getTimestamp("leido_en");
        return new AvisoBandeja(rs.getLong("id"), tipo(rs.getString("tipo")), rs.getString("titulo"), rs.getString("cuerpo"),
                datos(rs.getString("datos")), rs.getString("ruta"), rs.getTimestamp("creado_en").toInstant(),
                leido == null ? null : leido.toInstant());
    }

    /** Un tipo que ya no existe en el código no rompe la bandeja: se muestra con su texto en español */
    private static AvisoBandeja.Tipo tipo(String valor) {
        try {
            return AvisoBandeja.Tipo.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private String datosJson(Map<String, Object> datos) {
        if (datos == null || datos.isEmpty()) return null;
        try {
            String s = json.writeValueAsString(datos);
            return s.length() <= MAX_DATOS ? s : null;
        } catch (JsonProcessingException e) {
            log.warn("No se pudieron guardar los datos de un aviso: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> datos(String s) {
        if (s == null || s.isBlank()) return Map.of();
        try {
            return json.readValue(s, MAPA);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private static String recortar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
