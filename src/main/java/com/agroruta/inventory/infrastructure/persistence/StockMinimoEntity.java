package com.agroruta.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Cantidad (en ml o g) a partir de la cual se avisa que un insumo se está acabando. */
@Entity
@Table(name = "stock_minimo_insumo")
@Getter
@Setter
public class StockMinimoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agricultor_id", nullable = false)
    private Long agricultorId;

    @Column(name = "agricultural_input_id", nullable = false)
    private Long insumoId;

    @Column(nullable = false)
    private double minimo;
}
