package com.agroruta.shared.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.Map;

/**
 * Una sola consulta por recurso: sube con JOIN hasta el dueño (cosecha -> siembra -> lote -> finca,
 * pago -> nómina -> trabajador) en vez de traer cada entidad completa, una por una.
 * Igual que antes, no mira el borrado lógico: una finca eliminada sigue siendo de su agricultor.
 */
@Repository
public class ConsultaPropiedadJdbc implements ConsultaPropiedad {

    private static final String HASTA_FINCA =
            " JOIN siembras s ON s.id = x.siembra_id"
          + " JOIN lotes l ON l.id = s.lote_id"
          + " JOIN fincas f ON f.id = l.finca_id"
          + " WHERE x.id = ? AND f.agricultor_id = ?";

    private static final String HASTA_TRABAJADOR =
            " JOIN trabajadores t ON t.id = x.trabajador_id"
          + " WHERE x.id = ? AND t.agricultor_id = ?";

    private static final Map<Recurso, String> CONSULTAS = new EnumMap<>(Recurso.class);

    static {
        CONSULTAS.put(Recurso.FINCA,
                "SELECT COUNT(*) FROM fincas f WHERE f.id = ? AND f.agricultor_id = ?");
        CONSULTAS.put(Recurso.LOTE,
                "SELECT COUNT(*) FROM lotes l JOIN fincas f ON f.id = l.finca_id"
              + " WHERE l.id = ? AND f.agricultor_id = ?");
        CONSULTAS.put(Recurso.SIEMBRA,
                "SELECT COUNT(*) FROM siembras s JOIN lotes l ON l.id = s.lote_id"
              + " JOIN fincas f ON f.id = l.finca_id WHERE s.id = ? AND f.agricultor_id = ?");
        CONSULTAS.put(Recurso.COSECHA, "SELECT COUNT(*) FROM cosechas x" + HASTA_FINCA);
        CONSULTAS.put(Recurso.FUMIGACION, "SELECT COUNT(*) FROM fumigaciones x" + HASTA_FINCA);
        CONSULTAS.put(Recurso.ACTIVIDAD_CULTIVO, "SELECT COUNT(*) FROM actividades_cultivo x" + HASTA_FINCA);
        // Las labores comunes (sin agricultor) no son de nadie: solo las cambia el Administrador
        CONSULTAS.put(Recurso.LABOR,
                "SELECT COUNT(*) FROM actividades a WHERE a.id = ? AND a.agricultor_id = ?");
        CONSULTAS.put(Recurso.TRABAJADOR,
                "SELECT COUNT(*) FROM trabajadores t WHERE t.id = ? AND t.agricultor_id = ?");
        CONSULTAS.put(Recurso.CARGO,
                "SELECT COUNT(*) FROM cargos c WHERE c.id = ? AND c.agricultor_id = ?");
        CONSULTAS.put(Recurso.JORNAL, "SELECT COUNT(*) FROM jornales x" + HASTA_TRABAJADOR);
        CONSULTAS.put(Recurso.NOMINA, "SELECT COUNT(*) FROM nominas x" + HASTA_TRABAJADOR);
        CONSULTAS.put(Recurso.PAGO,
                "SELECT COUNT(*) FROM pagos p JOIN nominas n ON n.id = p.nomina_id"
              + " JOIN trabajadores t ON t.id = n.trabajador_id WHERE p.id = ? AND t.agricultor_id = ?");
    }

    private final JdbcTemplate jdbc;

    public ConsultaPropiedadJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean esDueno(Recurso recurso, Long recursoId, Long agricultorId) {
        if (recurso == null || recursoId == null || agricultorId == null) return false;
        Integer filas = jdbc.queryForObject(CONSULTAS.get(recurso), Integer.class, recursoId, agricultorId);
        return filas != null && filas > 0;
    }

    /** Para las pruebas: la consulta de cada recurso. */
    static String consulta(Recurso recurso) {
        return CONSULTAS.get(recurso);
    }
}
