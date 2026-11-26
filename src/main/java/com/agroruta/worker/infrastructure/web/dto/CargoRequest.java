package com.agroruta.worker.infrastructure.web.dto;

import com.agroruta.worker.domain.ModalidadPago;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CargoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    public String nombre;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    public String descripcion;

    /** JORNAL (por día, si no viene) o KILO */
    public ModalidadPago modalidadPago;

    /** Obligatorio si se paga por día (lo valida el dominio) */
    @Positive(message = "El valor del jornal debe ser mayor a 0")
    @DecimalMax(value = "100000000", message = "El valor del jornal es demasiado alto")
    @Digits(integer = 10, fraction = 2, message = "El valor del jornal admite máximo 2 decimales")
    public BigDecimal valorJornal;

    /** Obligatorio si se paga por kilo */
    @Positive(message = "El valor del kilo debe ser mayor a 0")
    @DecimalMax(value = "10000000", message = "El valor del kilo es demasiado alto")
    @Digits(integer = 8, fraction = 2, message = "El valor del kilo admite máximo 2 decimales")
    public BigDecimal valorKilo;
}
