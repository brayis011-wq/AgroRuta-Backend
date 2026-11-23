package com.agroruta.worker.domain;

import com.agroruta.shared.exception.DatoInvalidoException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class Cargo {

    private Long id;
    private String nombre;
    private String descripcion;
    /** Valor del día; en los cargos que se pagan por kilo es 0 */
    private BigDecimal valorJornal;
    private boolean activo;
    private ModalidadPago modalidadPago = ModalidadPago.JORNAL;
    /** Pesos por kilo recogido; solo en los cargos que se pagan por kilo */
    private BigDecimal valorKilo;

    /** Id del usuario (agricultor) dueño de este cargo. */
    private Long agricultorId;

    public Cargo(Long id, String nombre, String descripcion, BigDecimal valorJornal, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
        this.activo = activo;
    }

    public void actualizar(String nombre, String descripcion, BigDecimal valorJornal) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.valorJornal = valorJornal;
    }

    /**
     * Fija cómo se paga el cargo. Por día: el valor del jornal es obligatorio.
     * Por kilo: el valor del kilo es obligatorio y el del día queda en 0.
     */
    public void fijarPago(ModalidadPago modalidad, BigDecimal valorJornal, BigDecimal valorKilo) {
        ModalidadPago m = modalidad == null ? ModalidadPago.JORNAL : modalidad;
        if (m == ModalidadPago.KILO) {
            if (valorKilo == null || valorKilo.signum() <= 0) {
                throw new DatoInvalidoException("Indica cuánto se paga por kilo.");
            }
            this.valorKilo = valorKilo;
            this.valorJornal = BigDecimal.ZERO;
        } else {
            if (valorJornal == null || valorJornal.signum() <= 0) {
                throw new DatoInvalidoException("El valor del jornal debe ser mayor a 0.");
            }
            this.valorJornal = valorJornal;
            this.valorKilo = null;
        }
        this.modalidadPago = m;
    }

    public boolean pagaPorKilo() {
        return modalidadPago == ModalidadPago.KILO;
    }

    public void desactivar() { this.activo = false; }
    public void activar()    { this.activo = true; }
}
