package com.agroruta.configuration.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de perfil, aparte de la tabla usuarios: el usuario se busca en cada petición para
 * validar la sesión y la foto (una imagen en texto) solo hace falta en la pantalla de perfil.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fotos_perfil")
public class FotoPerfilEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String foto;
}
