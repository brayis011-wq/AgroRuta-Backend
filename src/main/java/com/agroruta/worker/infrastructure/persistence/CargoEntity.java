package com.agroruta.worker.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cargos", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_cargo_nombre_agricultor",
                columnNames = {"nombre", "agricultor_id"}
        )
})
public class CargoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "valor_jornal", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorJornal;

    @Column(nullable = false)
    private boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_pago", nullable = false, length = 10)
    private com.agroruta.worker.domain.ModalidadPago modalidadPago = com.agroruta.worker.domain.ModalidadPago.JORNAL;

    @Column(name = "valor_kilo", precision = 10, scale = 2)
    private BigDecimal valorKilo;

    // Nullable a propósito: ddl-auto=update puede agregar la columna sobre filas
    // existentes. Se rellena con el SQL de migración.
    @Column(name = "agricultor_id")
    private Long agricultorId;
}