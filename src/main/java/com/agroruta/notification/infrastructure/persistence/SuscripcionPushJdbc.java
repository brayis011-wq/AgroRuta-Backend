package com.agroruta.notification.infrastructure.persistence;

import com.agroruta.notification.application.ports.out.SuscripcionPushRepository;
import com.agroruta.notification.domain.SuscripcionPush;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Repository
public class SuscripcionPushJdbc implements SuscripcionPushRepository {

    private final JdbcTemplate jdbc;

    public SuscripcionPushJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void guardar(Long usuarioId, String endpoint, String p256dh, String auth, String dispositivo) {
        // El mismo navegador se suscribe una sola vez: si entra otra cuenta, los avisos pasan a ser de ella
        jdbc.update("DELETE FROM suscripciones_push WHERE endpoint_hash = ?", huella(endpoint));
        jdbc.update("INSERT INTO suscripciones_push (usuario_id, endpoint, endpoint_hash, p256dh, auth, dispositivo, creada_en) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                usuarioId, endpoint, huella(endpoint), p256dh, auth, dispositivo, Timestamp.from(Instant.now()));
    }

    @Override
    public List<SuscripcionPush> deUsuario(Long usuarioId) {
        return jdbc.query("SELECT id, usuario_id, endpoint, p256dh, auth, dispositivo FROM suscripciones_push "
                        + "WHERE usuario_id = ? ORDER BY creada_en DESC, id DESC",
                (rs, i) -> new SuscripcionPush(rs.getLong("id"), rs.getLong("usuario_id"), rs.getString("endpoint"),
                        rs.getString("p256dh"), rs.getString("auth"), rs.getString("dispositivo")),
                usuarioId);
    }

    @Override
    public boolean existe(Long usuarioId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM suscripciones_push WHERE usuario_id = ?", Integer.class, usuarioId);
        return n != null && n > 0;
    }

    @Override
    public void eliminar(Long id) {
        jdbc.update("DELETE FROM suscripciones_push WHERE id = ?", id);
    }

    @Override
    public void eliminarPorEndpoint(Long usuarioId, String endpoint) {
        jdbc.update("DELETE FROM suscripciones_push WHERE usuario_id = ? AND endpoint_hash = ?", usuarioId, huella(endpoint));
    }

    @Override
    public void eliminarTodas() {
        jdbc.update("DELETE FROM suscripciones_push");
    }

    private static String huella(String endpoint) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
