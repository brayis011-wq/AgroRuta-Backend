package com.agroruta.crop.infrastructure.persistence;

import com.agroruta.crop.application.ports.out.DiagnosticoPlagaRepository;
import com.agroruta.crop.domain.AnalisisFoto;
import com.agroruta.crop.domain.ConfianzaDiagnostico;
import com.agroruta.crop.domain.DiagnosticoPlaga;
import com.agroruta.crop.domain.TipoProblema;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class DiagnosticoPlagaJdbc implements DiagnosticoPlagaRepository {

    private static final String COLUMNAS = "id, siembra_id, creado_en, nota, problema, tipo, confianza, sintomas, "
            + "recomendaciones, otras_posibilidades, advertencia";

    private static final RowMapper<DiagnosticoPlaga> FILA = (rs, i) -> new DiagnosticoPlaga(
            rs.getLong("id"), rs.getLong("siembra_id"), rs.getTimestamp("creado_en").toLocalDateTime(),
            rs.getString("nota"), rs.getString("problema"),
            enumerado(TipoProblema.class, rs.getString("tipo"), TipoProblema.INDETERMINADO),
            enumerado(ConfianzaDiagnostico.class, rs.getString("confianza"), ConfianzaDiagnostico.BAJA),
            rs.getString("sintomas"), lineas(rs.getString("recomendaciones")), lineas(rs.getString("otras_posibilidades")),
            rs.getString("advertencia"));

    private final JdbcTemplate jdbc;

    public DiagnosticoPlagaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DiagnosticoPlaga guardar(Long siembraId, Long usuarioId, LocalDateTime fecha, byte[] foto, String nota, AnalisisFoto a) {
        KeyHolder clave = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO diagnosticos_plaga (siembra_id, usuario_id, creado_en, foto, nota, problema, tipo, confianza, "
                            + "sintomas, recomendaciones, otras_posibilidades, advertencia, modelo) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, siembraId);
            ps.setLong(2, usuarioId);
            ps.setTimestamp(3, Timestamp.valueOf(fecha));
            ps.setBytes(4, foto);
            ps.setString(5, nota);
            ps.setString(6, a.problema());
            ps.setString(7, a.tipo().name());
            ps.setString(8, a.confianza().name());
            ps.setString(9, a.sintomas());
            ps.setString(10, unir(a.recomendaciones()));
            ps.setString(11, unir(a.otrasPosibilidades()));
            ps.setString(12, a.advertencia());
            ps.setString(13, a.modelo());
            return ps;
        }, clave);
        Number id = clave.getKey();
        return new DiagnosticoPlaga(id == null ? null : id.longValue(), siembraId, fecha, nota, a.problema(), a.tipo(),
                a.confianza(), a.sintomas(), a.recomendaciones(), a.otrasPosibilidades(), a.advertencia());
    }

    @Override
    public List<DiagnosticoPlaga> deSiembra(Long siembraId, int limite) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM diagnosticos_plaga WHERE siembra_id = ? "
                + "ORDER BY creado_en DESC, id DESC LIMIT ?", FILA, siembraId, limite);
    }

    @Override
    public Optional<byte[]> foto(Long siembraId, Long diagnosticoId) {
        return jdbc.query("SELECT foto FROM diagnosticos_plaga WHERE id = ? AND siembra_id = ?",
                (rs, i) -> rs.getBytes("foto"), diagnosticoId, siembraId).stream().findFirst();
    }

    @Override
    public boolean eliminar(Long siembraId, Long diagnosticoId) {
        return jdbc.update("DELETE FROM diagnosticos_plaga WHERE id = ? AND siembra_id = ?", diagnosticoId, siembraId) > 0;
    }

    @Override
    public void anotarUso(Long usuarioId, LocalDate dia) {
        jdbc.update("INSERT INTO diagnosticos_uso (usuario_id, dia, cantidad) VALUES (?, ?, 1) "
                + "ON DUPLICATE KEY UPDATE cantidad = cantidad + 1", usuarioId, Date.valueOf(dia));
    }

    @Override
    public void devolverUso(Long usuarioId, LocalDate dia) {
        jdbc.update("UPDATE diagnosticos_uso SET cantidad = cantidad - 1 WHERE usuario_id = ? AND dia = ? AND cantidad > 0",
                usuarioId, Date.valueOf(dia));
    }

    @Override
    public int usoDe(Long usuarioId, LocalDate dia) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(cantidad), 0) FROM diagnosticos_uso WHERE usuario_id = ? AND dia = ?",
                Integer.class, usuarioId, Date.valueOf(dia));
        return n == null ? 0 : n;
    }

    @Override
    public int usoTotal(LocalDate dia) {
        Integer n = jdbc.queryForObject("SELECT COALESCE(SUM(cantidad), 0) FROM diagnosticos_uso WHERE dia = ?",
                Integer.class, Date.valueOf(dia));
        return n == null ? 0 : n;
    }

    private static String unir(List<String> lineas) {
        return lineas == null || lineas.isEmpty() ? null : String.join("\n", lineas);
    }

    private static List<String> lineas(String texto) {
        if (texto == null || texto.isBlank()) return List.of();
        return Arrays.stream(texto.split("\n")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor, E porDefecto) {
        try {
            return valor == null ? porDefecto : Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
