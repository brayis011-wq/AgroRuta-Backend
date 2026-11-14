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
public class Cosecha {

    private Long id;
    private LocalDate fecha;
    private Double cantidadKg;
    private CalidadCosecha calidad;
    private String observaciones;
    private Long siembraId;
    /** Precio de venta por kilo (COP); null si no se registró */
    private BigDecimal precioKg;
    /** Se registró dentro del periodo de carencia de una fumigación (la persona lo confirmó) */
    private boolean enCarencia;

    public Cosecha(Long id, LocalDate fecha, Double cantidadKg,
                   CalidadCosecha calidad, String observaciones, Long siembraId) {
        this.id = id;
        this.fecha = fecha;
        this.cantidadKg = cantidadKg;
        this.calidad = calidad;
        this.observaciones = observaciones;
        this.siembraId = siembraId;
    }

    public void setPrecioKg(BigDecimal precioKg) {
        this.precioKg = Dinero.redondear(precioKg);
    }
}