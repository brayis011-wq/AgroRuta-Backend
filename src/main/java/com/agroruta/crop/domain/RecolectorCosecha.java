package com.agroruta.crop.domain;

import java.math.BigDecimal;

/**
 * Un recolector ya anotado en una cosecha: su jornal y si todavía se puede cambiar.
 *
 * @param estadoNomina null (sin nómina), PENDIENTE, APROBADA o PAGADA
 * @param editable     false si el jornal está en una nómina aprobada o pagada
 */
public record RecolectorCosecha(Long jornalId, Long trabajadorId, String nombre, String modalidad,
                                BigDecimal kilos, BigDecimal valorKilo, BigDecimal valor,
                                String estadoNomina, boolean editable) {}
