package com.agroruta.report.infrastructure.pdf;

import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.RentabilidadLote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Formato colombiano para los reportes: $1.250.000, 4 nov 2026, etapas en español. */
final class FormatoReporte {

    private static final Locale ES_CO = Locale.forLanguageTag("es-CO");
    private static final String[] MESES = {
        "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"
    };

    static final Map<String, String> ETAPAS = Map.of(
            "GERMINACION", "Germinación",
            "CRECIMIENTO", "Crecimiento",
            "PRODUCCION", "Producción",
            "COSECHA", "Cosecha",
            "FINALIZADO", "Finalizado");

    static final java.time.format.DateTimeFormatter DIA_CUADERNO =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
        return formato(decimales > 0 ? "#,##0." + "0".repeat(decimales) : "#,##0")
                .format(v);
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
        } catch (java.time.format.DateTimeParseException | NullPointerException e) {
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

    // ── Textos de los reportes (antes en PdfReportService) ──

    static String textoPeriodo(com.agroruta.report.domain.Periodo p) {
        if (p == null || p.esTodo()) return "todo el historial";
        if (p.desde() == null) return "hasta el " + p.hasta().format(DIA_CUADERNO);
        if (p.hasta() == null) return "desde el " + p.desde().format(DIA_CUADERNO);
        return p.desde().format(DIA_CUADERNO) + " a " + p.hasta().format(DIA_CUADERNO);
    }

    static String numeroCorto(double v) {
        java.text.NumberFormat f = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(2);
        return f.format(v);
    }

    static String unidad(String u) {
        if (u == null) return "";
        return switch (u) {
            case "LITROS" -> "L";
            case "ML" -> "ml";
            case "GRAMOS" -> "g";
            case "KG" -> "kg";
            default -> u.toLowerCase();
        };
    }

    static String tipoProducto(String t) {
        return capitalizar(t.replace('_', ' ').toLowerCase());
    }

    /** 420,5 o 1.240 (sin decimales cuando es exacto) */
    static String kg(Double v) {
        double x = v != null ? v : 0;
        return numero(x, x % 1 == 0 ? 0 : 1);
    }

    static double valor(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    static String pesosDe(double v) {
        return FormatoReporte.pesos(BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP));
    }

    /** "-$300.000" para una pérdida (la fuente del PDF no tiene el signo menos tipográfico) */
    static String pesosConSigno(double v) {
        return v < 0 ? "-" + pesosDe(-v) : pesosDe(v);
    }

    static String porcentaje(double v) {
        return numero(v, Math.abs(v) < 10 ? 1 : 0) + " %";
    }

    static String porcentajeDe(double parte, double total) {
        return total > 0 ? numero(parte * 100 / total, 0) + " %" : null;
    }

    static String kgTexto(double kg) {
        return numero(kg, kg % 1 == 0 ? 0 : 1) + " kg";
    }

    static String nombreLote(RentabilidadLote l) {
        return l.fincaNombre() != null ? l.loteNombre() + " · " + l.fincaNombre() : l.loteNombre();
    }

    /** "nómina, cultivos y costos" */
    static String enumerar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
    }

    static String capitalizar(String t) {
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Cultivo para agrupar: el tipo si existe, si no lo que va antes de " · " */
    static String cultivoDe(CropDetail c) {
        if (c.cultivo() != null && !c.cultivo().isBlank()) return c.cultivo();
        String v = c.variedad() != null ? c.variedad() : "—";
        int i = v.indexOf(" · ");
        return i > 0 ? v.substring(0, i) : v;
    }

    static String ubicacion(CropDetail c) {
        if (c.fincaNombre() == null && c.loteNombre() == null) return "—";
        if (c.fincaNombre() == null) return c.loteNombre();
        if (c.loteNombre() == null) return c.fincaNombre();
        return c.fincaNombre() + " · " + c.loteNombre();
    }
}
