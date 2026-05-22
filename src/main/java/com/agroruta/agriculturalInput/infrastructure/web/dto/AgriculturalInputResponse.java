package com.agroruta.agriculturalInput.infrastructure.web.dto;

import java.time.LocalDateTime;

/**
 * DTO de salida.
 * El cliente Angular recibe exactamente esta estructura.
 * displayName expone el nombre legible del enum (ej: "Fertilizante foliar").
 */
public class AgriculturalInputResponse {

    private Long id;
    private String nombre;
    private String tipo;            // Enum name: FUNGICIDA, INSECTICIDA…
    private String tipoDisplay;     // Nombre legible: "Fungicida", "Insecticida"…
    private String unidadSugerida;
    private Double dosisSugerida;
    private Integer reentradaHoras;
    private Boolean activo;
    private LocalDateTime creadoEn;

    // ── Builder ───────────────────────────────────────────────────────────────

    private AgriculturalInputResponse() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AgriculturalInputResponse dto = new AgriculturalInputResponse();

        public Builder id(Long id)                      { dto.id = id; return this; }
        public Builder nombre(String nombre)            { dto.nombre = nombre; return this; }
        public Builder tipo(String tipo)                { dto.tipo = tipo; return this; }
        public Builder tipoDisplay(String td)           { dto.tipoDisplay = td; return this; }
        public Builder unidadSugerida(String u)         { dto.unidadSugerida = u; return this; }
        public Builder dosisSugerida(Double d)          { dto.dosisSugerida = d; return this; }
        public Builder reentradaHoras(Integer r)        { dto.reentradaHoras = r; return this; }
        public Builder activo(Boolean a)                { dto.activo = a; return this; }
        public Builder creadoEn(LocalDateTime t)        { dto.creadoEn = t; return this; }

        public AgriculturalInputResponse build()        { return dto; }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getNombre()            { return nombre; }
    public String getTipo()              { return tipo; }
    public String getTipoDisplay()       { return tipoDisplay; }
    public String getUnidadSugerida()    { return unidadSugerida; }
    public Double getDosisSugerida()     { return dosisSugerida; }
    public Integer getReentradaHoras()   { return reentradaHoras; }
    public Boolean getActivo()           { return activo; }
    public LocalDateTime getCreadoEn()   { return creadoEn; }
}