package com.agroruta.crop.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.agroruta.shared.util.Dinero;
import java.time.LocalDate;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ActividadCultivo {

    private Long id;
    private TipoActividad tipo;
    private String descripcion;
    private LocalDate fecha;
    private Long siembraId;
    /** Costo de materiales o servicios de la actividad (COP), aparte de los jornales */
    private BigDecimal costo;

    public ActividadCultivo(Long id, TipoActividad tipo, String descripcion,
                            LocalDate fecha, Long siembraId) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.siembraId = siembraId;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = Dinero.redondear(costo);
    }
}