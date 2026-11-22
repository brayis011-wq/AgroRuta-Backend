package com.agroruta.worker.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.EstadoInvalidoException;
import java.math.RoundingMode;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Jornal {

    private Long id;
    private LocalDate fecha;
    private Trabajador trabajador;
    private Long cultivoId;
    private String nombreCultivo;
    private List<Labor> actividades;
    private String observaciones;
    private BigDecimal valorJornal;
    private boolean liquidado;
    /** Kilos recogidos ese día (solo cuando el cargo se paga por kilo) */
    private BigDecimal kilos;
    /** Valor del kilo el día del jornal (si el cargo cambia después, este jornal no cambia) */
    private BigDecimal valorKilo;
    /** La cosecha de la que salió (recolector); null si se anotó aparte */
    private Long cosechaId;

    public Jornal() {
        this.actividades = new ArrayList<>();
        this.liquidado = false;
    }

    public Jornal(Long id, LocalDate fecha, Trabajador trabajador,
                  Long cultivoId, String nombreCultivo, String observaciones) {
        this.id = id;
        this.fecha = fecha;
        this.trabajador = trabajador;
        this.cultivoId = cultivoId;
        this.nombreCultivo = nombreCultivo;
        this.observaciones = observaciones;
        this.actividades = new ArrayList<>();
        this.liquidado = false;
        this.valorJornal = trabajador.getCargo().getValorJornal();
    }

    // ── Lógica de dominio ────────────────────────────────────────────────────

    public void agregarActividad(Labor actividad) {
        if (this.liquidado) {
            throw new EstadoInvalidoException("No se pueden modificar las labores de un jornal ya liquidado.");
        }
        if (!this.actividades.contains(actividad)) {
            this.actividades.add(actividad);
        }
    }

    public void removerActividad(Labor actividad) {
        if (this.liquidado) {
            throw new EstadoInvalidoException("No se pueden modificar las labores de un jornal ya liquidado.");
        }
        this.actividades.remove(actividad);
    }

    /**
     * Anota los kilos recogidos y calcula el valor del día: kilos × valor del kilo del cargo.
     * Si el cargo se paga por día, no se aceptan kilos.
     */
    public void registrarKilos(BigDecimal kilos) {
        Cargo cargo = trabajador == null ? null : trabajador.getCargo();
        boolean porKilo = cargo != null && cargo.pagaPorKilo();
        if (!porKilo) {
            if (kilos != null) {
                throw new DatoInvalidoException("El cargo de este trabajador se paga por día, no por kilo.");
            }
            return;
        }
        if (kilos == null || kilos.signum() <= 0) {
            throw new DatoInvalidoException("Indica cuántos kilos recogió " + trabajador.getNombreCompleto() + ".");
        }
        if (this.liquidado) {
            throw new EstadoInvalidoException("No se pueden cambiar los kilos de un jornal ya liquidado.");
        }
        this.kilos = kilos.setScale(2, RoundingMode.HALF_UP);
        this.valorKilo = cargo.getValorKilo();
        this.valorJornal = this.kilos.multiply(this.valorKilo).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Recolector de una cosecha pagado por kilo: kilos × valor del kilo. El valor sale del que se indique
     * o, si no, del cargo (aunque el cargo se pague por día: en la cosecha se decide por persona).
     */
    public void pagarPorKilo(BigDecimal kilos, BigDecimal valorKilo) {
        if (this.liquidado) {
            throw new EstadoInvalidoException("No se pueden cambiar los kilos de un jornal ya liquidado.");
        }
        String nombre = trabajador == null ? "el recolector" : trabajador.getNombreCompleto();
        if (kilos == null || kilos.signum() <= 0) {
            throw new DatoInvalidoException("Indica cuántos kilos recogió " + nombre + ".");
        }
        Cargo cargo = trabajador == null ? null : trabajador.getCargo();
        BigDecimal valor = valorKilo != null ? valorKilo : (cargo == null ? null : cargo.getValorKilo());
        if (valor == null || valor.signum() <= 0) {
            throw new DatoInvalidoException("Indica cuánto se le paga el kilo a " + nombre + ".");
        }
        this.kilos = kilos.setScale(2, RoundingMode.HALF_UP);
        this.valorKilo = valor.setScale(2, RoundingMode.HALF_UP);
        this.valorJornal = this.kilos.multiply(this.valorKilo).setScale(2, RoundingMode.HALF_UP);
    }

    /** Recolector de una cosecha pagado por día: el valor indicado o, si no, el del cargo */
    public void pagarPorDia(BigDecimal valorDia) {
        if (this.liquidado) {
            throw new EstadoInvalidoException("No se puede cambiar el valor de un jornal ya liquidado.");
        }
        Cargo cargo = trabajador == null ? null : trabajador.getCargo();
        BigDecimal valor = valorDia != null ? valorDia : (cargo == null ? null : cargo.getValorJornal());
        if (valor == null || valor.signum() <= 0) {
            throw new DatoInvalidoException("Indica cuánto se le paga el día a "
                    + (trabajador == null ? "el recolector" : trabajador.getNombreCompleto()) + ".");
        }
        this.kilos = null;
        this.valorKilo = null;
        this.valorJornal = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public void marcarComoLiquidado() {
        this.liquidado = true;
    }

    // Sobreescribe el getter de Lombok para proteger la lista
    public List<Labor> getActividades() {
        return Collections.unmodifiableList(actividades);
    }
}