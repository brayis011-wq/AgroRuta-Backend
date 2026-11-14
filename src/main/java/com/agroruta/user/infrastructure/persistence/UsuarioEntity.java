package com.agroruta.user.infrastructure.persistence;

import com.agroruta.user.domain.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(length = 20)
    private String telefono;


    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = true;

    public UsuarioEntity(Long id, String nombre, String email, String password,
                         Rol rol, boolean activo, LocalDateTime fechaCreacion,
                         String telefono) {
        this.id            = id;
        this.nombre        = nombre;
        this.email         = email;
        this.password      = password;
        this.rol           = rol;
        this.activo        = activo;
        this.fechaCreacion = fechaCreacion;
        this.telefono      = telefono;
    }
}