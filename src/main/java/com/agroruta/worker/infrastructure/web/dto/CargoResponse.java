package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.ModalidadPago;
import java.math.BigDecimal;

public class CargoResponse {
    public Long id;
    public String nombre;
    public String descripcion;
    public BigDecimal valorJornal;
    public boolean activo;
    public ModalidadPago modalidadPago;
    public BigDecimal valorKilo;

    public static CargoResponse from(Cargo c) {
        CargoResponse r = new CargoResponse();
        r.id = c.getId();
        r.nombre = c.getNombre();
        r.descripcion = c.getDescripcion();
        r.valorJornal = c.getValorJornal();
        r.activo = c.isActivo();
        r.modalidadPago = c.getModalidadPago() == null ? ModalidadPago.JORNAL : c.getModalidadPago();
        r.valorKilo = c.getValorKilo();
        return r;
    }
}
