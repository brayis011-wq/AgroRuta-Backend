package com.agroruta.configuration.domain;

import java.util.regex.Pattern;

/**
 * Reglas de la foto de perfil. La foto llega desde la app ya recortada y reducida,
 * como imagen embebida (data:image/jpeg;base64,...). También se aceptan enlaces cortos,
 * que es como se guardaban antes.
 */
public final class FotoPerfil {

    /** Una foto de 320x320 en JPEG ocupa unos 20-40 KB; se deja margen para fotos con mucho detalle */
    public static final int MAX_EMBEBIDA = 350_000;
    public static final int MAX_ENLACE = 500;

    private static final Pattern EMBEBIDA =
            Pattern.compile("^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+={0,2}$");

    private FotoPerfil() {
    }

    /** Devuelve la foto tal cual si es válida; si no, lanza IllegalArgumentException con el motivo */
    public static String validar(String foto) {
        if (foto == null || foto.isEmpty()) {
            return foto;
        }
        if (foto.startsWith("data:")) {
            if (foto.length() > MAX_EMBEBIDA) {
                throw new IllegalArgumentException("La foto es muy pesada. Elige una imagen más pequeña.");
            }
            if (!EMBEBIDA.matcher(foto).matches()) {
                throw new IllegalArgumentException("La foto debe ser una imagen JPG, PNG o WebP.");
            }
            return foto;
        }
        if (foto.length() > MAX_ENLACE) {
            throw new IllegalArgumentException("El enlace de la foto no puede superar 500 caracteres.");
        }
        return foto;
    }
}
