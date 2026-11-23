package com.agroruta.finance.application.ports.in;

import com.agroruta.finance.domain.PrecioCultivo;
import com.agroruta.finance.domain.PrecioReferencia;
import com.agroruta.finance.domain.PreferenciaPrecio;

import java.math.BigDecimal;
import java.util.List;

/** Precio de referencia de lo que se cosecha, para estimar lo que vale una cosecha sin precio. */
public interface PreciosReferenciaUseCase {

    /** El precio del cultivo: el propio del agricultor, el producto que eligió o el que más se parece en SIPSA. */
    PrecioCultivo precioDe(Long agricultorId, String cultivo, String variedad);

    /** Productos de la última semana de SIPSA que contienen el texto (para elegir a mano). */
    List<PrecioReferencia> buscar(String texto);

    List<PreferenciaPrecio> preferencias(Long agricultorId);

    /** Fija para un cultivo un producto de SIPSA o un precio propio (uno de los dos). */
    PreferenciaPrecio elegir(Long agricultorId, String cultivo, String producto, BigDecimal precioKg);

    void quitarPreferencia(Long agricultorId, String cultivo);

    /** Trae los precios de SIPSA ahora (tarda: el servicio del DANE responde en unos 20 s). */
    int actualizar();

    /** Si los precios tienen más de una semana, los trae en segundo plano (no espera). */
    void actualizarSiHaceFalta();
}
