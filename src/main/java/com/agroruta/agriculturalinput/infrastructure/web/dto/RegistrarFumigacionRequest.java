package com.agroruta.agriculturalinput.infrastructure.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO de entrada para registrar una fumigación.
 *
 * agriculturalInputId es opcional (nullable):
 *   - Si viene con valor: el producto fue seleccionado del catálogo.
 *   - Si viene null: el producto fue escrito manualmente.
 * En ambos casos `producto` (String) siempre es obligatorio, ya que es
 * el nombre que se mostrará en reportes históricos, incluso si el insumo
 * del catálogo es eliminado en el futuro.
 */
public record RegistrarFumigacionRequest(
        @NotNull(message = "La fecha es obligatoria") LocalDate fecha,

        @NotBlank(message = "El producto es obligatorio")
        @Size(max = 150, message = "El nombre del producto no puede superar 150 caracteres")
        String producto,

        // Nullable: solo presente si el usuario seleccionó del catálogo
        Long agriculturalInputId,

        @NotNull(message = "La dosis es obligatoria")
        @DecimalMin(value = "0.01", message = "La dosis debe ser mayor a 0")
        Double dosis,

        @NotBlank(message = "La unidad de medida es obligatoria")
        @Pattern(
                regexp = "LITROS|ML|GRAMOS|KG",
                message = "Unidad inválida. Valores permitidos: LITROS, ML, GRAMOS, KG")
        String unidadMedida,

        @NotNull(message = "El área aplicada es obligatoria")
        @DecimalMin(value = "0.01", message = "El área debe ser mayor a 0")
        Double areaAplicada,

        String observaciones,

        @NotNull(message = "El ID de siembra es obligatorio")
        Long siembraId) {}
