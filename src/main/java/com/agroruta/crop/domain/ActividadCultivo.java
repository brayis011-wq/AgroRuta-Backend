package com.agroruta.crop.domain;

import com.agroruta.shared.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    /** Insumo que se gastó (abonado...): nombre, producto de la bodega y cantidad total usada */
    private String producto;

    private Long agriculturalInputId;
    private Double cantidadUsada;
    private String unidadCantidad;

    public ActividadCultivo(Long id, TipoActividad tipo, String descripcion, LocalDate fecha, Long siembraId) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.siembraId = siembraId;
    }

    /** Deja el insumo de la actividad como viene (null = sin insumo) */
    public void usarInsumo(UsoInsumo uso) {
        boolean hay = uso != null && ((uso.producto() != null && !uso.producto().isBlank()) || uso.insumoId() != null);
        this.producto = hay && uso.producto() != null ? uso.producto().trim() : null;
        this.agriculturalInputId = hay ? uso.insumoId() : null;
        this.cantidadUsada = hay ? uso.cantidad() : null;
        this.unidadCantidad = hay && uso.cantidad() != null ? uso.unidad() : null;
    }

    public UsoInsumo usoInsumo() {
        return producto == null && agriculturalInputId == null
                ? null
                : new UsoInsumo(agriculturalInputId, producto, cantidadUsada, unidadCantidad);
    }

    public void setCosto(BigDecimal costo) {
        this.costo = Dinero.redondear(costo);
    }
}
