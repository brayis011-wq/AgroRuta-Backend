package com.agroruta.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompraRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Double cantidad;

    // LITROS, ML, KG o GRAMOS (lo valida InventarioService)
    @NotBlank(message = "La unidad es obligatoria")
    private String unidad;

    // Opcional: lo que costó la compra completa (COP); sin él no se puede calcular el costo de las fumigaciones
    @PositiveOrZero(message = "El costo no puede ser negativo")
    private Double costoTotal;

    @PastOrPresent(message = "La fecha de compra no puede ser futura")
    private LocalDate fecha;

    @Size(max = 150, message = "El proveedor no puede superar 150 caracteres")
    private String proveedor;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;
}
