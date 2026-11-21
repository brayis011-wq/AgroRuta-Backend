package com.agroruta.finance.domain;

import java.math.BigDecimal;

/**
 * Lo que el agricultor eligió para un cultivo: un producto de SIPSA (si la búsqueda automática no
 * acertó) o un precio propio (para lo que SIPSA no publica, como el café pergamino). El precio propio manda.
 */
public record PreferenciaPrecio(String cultivo, String producto, BigDecimal precioKg) {}
