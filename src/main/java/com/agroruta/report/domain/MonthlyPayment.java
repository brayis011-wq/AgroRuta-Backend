package com.agroruta.report.domain;

import java.math.BigDecimal;

/** Total pagado en un mes ("2026-11") y cuántos pagos fueron. */
public record MonthlyPayment(String mes, BigDecimal total, int pagos) {}
