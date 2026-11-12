package com.agroruta.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "segundo_factor")
@Getter
@Setter
@NoArgsConstructor
public class SegundoFactorEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "secreto", nullable = false, length = 255)
    private String secreto;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "respaldo", length = 700)
    private String respaldo;

    @Column(name = "ultimo_paso")
    private Long ultimoPaso;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "activado_en")
    private LocalDateTime activadoEn;
}
