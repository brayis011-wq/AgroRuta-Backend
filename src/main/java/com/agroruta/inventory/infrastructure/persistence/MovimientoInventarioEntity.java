package com.agroruta.inventory.infrastructure.persistence;

import com.agroruta.inventory.domain.TipoMovimiento;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
public class MovimientoInventarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agricultor_id", nullable = false)
    private Long agricultorId;

    @Column(name = "agricultural_input_id", nullable = false)
    private Long insumoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private double cantidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "costo_total", precision = 14, scale = 2)
    private BigDecimal costoTotal;

    @Column(length = 150)
    private String proveedor;

    @Column(name = "fumigacion_id")
    private Long fumigacionId;

    @Column(name = "actividad_id")
    private Long actividadId;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
