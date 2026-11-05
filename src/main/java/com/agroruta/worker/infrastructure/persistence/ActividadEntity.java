package com.agroruta.worker.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
// El nombre ya no es único en toda la tabla: cada agricultor tiene sus labores (se valida en ActividadService)
@Table(name = "actividades")
public class ActividadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activa;

    /** null = labor común del sistema */
    @Column(name = "agricultor_id")
    private Long agricultorId;
}