package com.agroruta.user.infrastructure.persistence;
import com.agroruta.user.domain.Rol;
import jakarta.persistence.*;
import java.time.LocalDateTime;
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
    private String telefono;                    // NUEVO

    @Column(name = "foto_perfil", length = 500)
    private String fotoPerfil;                  // NUEVO

    public UsuarioEntity() {}

    public UsuarioEntity(Long id, String nombre, String email, String password,
                         Rol rol, boolean activo, LocalDateTime fechaCreacion,
                         String telefono, String fotoPerfil) {  // NUEVO constructor
        this.id            = id;
        this.nombre        = nombre;
        this.email         = email;
        this.password      = password;
        this.rol           = rol;
        this.activo        = activo;
        this.fechaCreacion = fechaCreacion;
        this.telefono      = telefono;
        this.fotoPerfil    = fotoPerfil;
    }

    public Long getId()                           { return id; }
    public void setId(Long id)                    { this.id = id; }
    public String getNombre()                     { return nombre; }
    public void setNombre(String nombre)          { this.nombre = nombre; }
    public String getEmail()                      { return email; }
    public void setEmail(String email)            { this.email = email; }
    public String getPassword()                   { return password; }
    public void setPassword(String password)      { this.password = password; }
    public Rol getRol()                           { return rol; }
    public void setRol(Rol rol)                   { this.rol = rol; }
    public boolean isActivo()                     { return activo; }
    public void setActivo(boolean activo)         { this.activo = activo; }
    public LocalDateTime getFechaCreacion()       { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime f) { this.fechaCreacion = f; }
    public String getTelefono()                   { return telefono; }
    public void setTelefono(String telefono)      { this.telefono = telefono; }
    public String getFotoPerfil()                 { return fotoPerfil; }
    public void setFotoPerfil(String fotoPerfil)  { this.fotoPerfil = fotoPerfil; }
}