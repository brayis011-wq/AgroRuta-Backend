package com.agroruta.shared.util;

import com.agroruta.shared.exception.DatoInvalidoException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Convierte un texto en un valor de enum con un mensaje claro si no coincide.
 * Evita que Enum.valueOf devuelva "No enum constant com.agroruta..." al cliente.
 */
public final class EnumParser {

    private EnumParser() {}

    public static <E extends Enum<E>> E parse(Class<E> type, String value, String campo) {
        if (value == null || value.isBlank()) {
            throw new DatoInvalidoException("Falta el valor de " + campo + ".");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            String opciones = Arrays.stream(type.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new DatoInvalidoException(
                    "Valor no válido para " + campo + ": '" + value + "'. Opciones: " + opciones);
        }
    }
}
