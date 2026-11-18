package com.agroruta.report.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkerPayrollDetail(
        Long workerId,
        String workerName,
        Integer totalPagos,
        BigDecimal totalPaid,
        LocalDate periodoInicio,
        LocalDate periodoFin,
        Integer totalJornales,
        /** Primera letra del nombre y del apellido, igual que en la lista del equipo */
        String workerInitials
) {
    /** Sin iniciales calculadas: se toman la primera y la última palabra del nombre completo. */
    public WorkerPayrollDetail(Long workerId, String workerName, Integer totalPagos, BigDecimal totalPaid,
                               LocalDate periodoInicio, LocalDate periodoFin, Integer totalJornales) {
        this(workerId, workerName, totalPagos, totalPaid, periodoInicio, periodoFin, totalJornales, desdeNombre(workerName));
    }

    private static String desdeNombre(String nombre) {
        String[] p = nombre == null ? new String[0] : nombre.strip().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return "?";
        String r = p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "");
        return r.toUpperCase(java.util.Locale.ROOT);
    }
}
