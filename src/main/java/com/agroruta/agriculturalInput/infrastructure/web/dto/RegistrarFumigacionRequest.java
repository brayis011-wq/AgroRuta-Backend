package com.agroruta.agriculturalInput.infrastructure.web.dto;

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
public class RegistrarFumigacionRequest {

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "El producto es obligatorio")
    @Size(max = 150, message = "El nombre del producto no puede superar 150 caracteres")
    private String producto;

    // Nullable: solo presente si el usuario seleccionó del catálogo
    private Long agriculturalInputId;

    @NotNull(message = "La dosis es obligatoria")
    @DecimalMin(value = "0.01", message = "La dosis debe ser mayor a 0")
    private Double dosis;

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Pattern(
            regexp  = "LITROS|ML|GRAMOS|KG",
            message = "Unidad inválida. Valores permitidos: LITROS, ML, GRAMOS, KG"
    )
    private String unidadMedida;

    @NotNull(message = "El área aplicada es obligatoria")
    @DecimalMin(value = "0.01", message = "El área debe ser mayor a 0")
    private Double areaAplicada;

    private String observaciones;

    @NotNull(message = "El ID de siembra es obligatorio")
    private Long siembraId;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public LocalDate getFecha()                         { return fecha; }
    public void setFecha(LocalDate fecha)               { this.fecha = fecha; }

    public String getProducto()                         { return producto; }
    public void setProducto(String producto)            { this.producto = producto; }

    public Long getAgriculturalInputId()                { return agriculturalInputId; }
    public void setAgriculturalInputId(Long id)         { this.agriculturalInputId = id; }

    public Double getDosis()                            { return dosis; }
    public void setDosis(Double dosis)                  { this.dosis = dosis; }

    public String getUnidadMedida()                     { return unidadMedida; }
    public void setUnidadMedida(String u)               { this.unidadMedida = u; }

    public Double getAreaAplicada()                     { return areaAplicada; }
    public void setAreaAplicada(Double a)               { this.areaAplicada = a; }

    public String getObservaciones()                    { return observaciones; }
    public void setObservaciones(String o)              { this.observaciones = o; }

    public Long getSiembraId()                          { return siembraId; }
    public void setSiembraId(Long id)                   { this.siembraId = id; }
}