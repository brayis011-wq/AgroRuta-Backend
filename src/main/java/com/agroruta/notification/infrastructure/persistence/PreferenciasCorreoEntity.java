package com.agroruta.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "preferencias_correo")
@Getter
@Setter
@NoArgsConstructor
public class PreferenciasCorreoEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "alertas_cultivo", nullable = false)
    private boolean alertasCultivo;

    @Column(name = "avisos_trabajadores", nullable = false)
    private boolean avisosTrabajadores;
}
