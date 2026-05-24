package com.agroruta.worker.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "actividades", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_actividad_nombre",
                columnNames = {"nombre"}
        )
})
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
}