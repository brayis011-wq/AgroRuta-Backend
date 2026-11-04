package com.agroruta.report.infrastructure.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;

/** Formato colombiano para los reportes: $1.250.000, 4 nov 2026, etapas en español. */
final class FormatoReporte {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};

    static final Map<String, String> ETAPAS = Map.of(
            "GERMINACION", "Germinación",
            "CRECIMIENTO", "Crecimiento",
            "PRODUCCION", "Producción",
            "COSECHA", "Cosecha",
            "FINALIZADO", "Finalizado");

    private FormatoReporte() {}

    private static DecimalFormat formato(String patron) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(ES_CO);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        DecimalFormat f = new DecimalFormat(patron, s);
        f.setRoundingMode(RoundingMode.HALF_UP);
        return f;
    }

    /** $1.250.000 — con centavos solo si los hay: $1.200,50 */
    static String pesos(BigDecimal v) {
        BigDecimal x = v != null ? v : BigDecimal.ZERO;
        boolean centavos = x.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0;
        return "$" + formato(centavos ? "#,##0.00" : "#,##0").format(x);
    }

    /** $1,2 M — para etiquetas cortas en gráficas */
    static String pesosCorto(BigDecimal v) {
        double x = v != null ? v.doubleValue() : 0;
        if (x >= 1_000_000) return "$" + formato("#,##0.#").format(x / 1_000_000) + " M";
        if (x >= 1_000) return "$" + formato("#,##0").format(x / 1_000) + " mil";
        return pesos(v);
    }

    static String numero(double v, int decimales) {
        return formato(decimales > 0 ? "#,##0." + "0".repeat(decimales) : "#,##0").format(v);
    }

    static String fecha(LocalDate d) {
        return d == null ? "—" : d.getDayOfMonth() + " " + MESES[d.getMonthValue() - 1] + " " + d.getYear();
    }

    static String fechaHora(LocalDateTime t) {
        if (t == null) return "—";
        // Se arma a mano: el formato de Java usa espacios especiales que la fuente del PDF no tiene
        int h = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        return fecha(t.toLocalDate()) + ", " + h + ":" + String.format("%02d", t.getMinute())
                + (t.getHour() < 12 ? " a. m." : " p. m.");
    }

    /** "2026-11" → "nov" (y el año si cambia de año: "ene 27") */
    static String mesCorto(String mes, boolean conAnio) {
        try {
            YearMonth m = YearMonth.parse(mes);
            String base = MESES[m.getMonthValue() - 1];
            return conAnio ? base + " " + String.valueOf(m.getYear()).substring(2) : base;
        } catch (Exception e) {
            return mes;
        }
    }

    static String etapa(String codigo) {
        return codigo == null ? "—" : ETAPAS.getOrDefault(codigo, codigo);
    }

    static String periodo(LocalDate a, LocalDate b) {
        if (a == null && b == null) return "—";
        return fecha(a) + " – " + fecha(b);
    }
}
