package com.agroruta.crop.application;

import com.agroruta.crop.application.ports.out.PresupuestoRepository;
import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.crop.domain.PresupuestoSiembra;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.util.Dinero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Presupuesto por temporada: lo que se esperaba gastar frente a lo que se lleva. */
@Service
@Transactional
public class PresupuestoService {

    private static final Logger log = LoggerFactory.getLogger(PresupuestoService.class);

    private final PresupuestoRepository repositorio;
    private final CostosCalendarioService costos;

    public PresupuestoService(PresupuestoRepository repositorio, CostosCalendarioService costos) {
        this.repositorio = repositorio;
        this.costos = costos;
    }

    /** Vacío si la siembra no tiene presupuesto */
    @Transactional(readOnly = true)
    public Optional<PresupuestoSiembra.Comparacion> comparacion(Long siembraId) {
        return repositorio.deSiembra(siembraId).map(p -> p.comparar(costos.costos(siembraId)));
    }

    public PresupuestoSiembra.Comparacion guardar(Long siembraId, BigDecimal manoObra, BigDecimal insumos, BigDecimal otros) {
        for (BigDecimal v : new BigDecimal[]{manoObra, insumos, otros}) {
            if (v != null && v.signum() < 0) throw new DatoInvalidoException("Los valores del presupuesto no pueden ser negativos.");
        }
        PresupuestoSiembra p = new PresupuestoSiembra(siembraId, Dinero.redondear(manoObra), Dinero.redondear(insumos),
                Dinero.redondear(otros), 0);
        if (p.total().signum() <= 0) throw new DatoInvalidoException("Escribe cuánto esperas gastar en al menos una parte.");
        costos.costos(siembraId); // la siembra existe
        repositorio.guardar(p);
        return p.comparar(costos.costos(siembraId));
    }

    public void borrar(Long siembraId) {
        repositorio.borrar(siembraId);
    }

    /**
     * Avisos para el resumen del día: las temporadas que acaban de llegar al 90 % o de pasarse del presupuesto.
     * Cada nivel se avisa una sola vez (queda marcado); si se cambia el presupuesto, vuelve a empezar.
     */
    public List<AlertaCultivo> avisosNuevos(Long agricultorId, LocalDate hoy) {
        List<AlertaCultivo> res = new ArrayList<>();
        if (agricultorId == null) return res;
        for (PresupuestoRepository.ConLugar c : repositorio.activosDe(agricultorId)) {
            try {
                PresupuestoSiembra p = c.presupuesto();
                PresupuestoSiembra.Comparacion comp = p.comparar(costos.costos(p.siembraId()));
                int nivel = p.nivelPorAvisar(comp);
                if (nivel == 0) continue;
                AlertaCultivo.Tipo tipo = nivel == PresupuestoSiembra.SUPERADO
                        ? AlertaCultivo.Tipo.PRESUPUESTO_SUPERADO : AlertaCultivo.Tipo.PRESUPUESTO_CERCA;
                String detalle = pesos(comp.gastadoTotal()) + " de " + pesos(comp.total());
                res.add(new AlertaCultivo(tipo, c.finca(), c.lote(), c.cultivo(),
                        String.valueOf(Math.round(comp.porcentaje())), detalle, hoy));
                repositorio.marcarAvisado(p.siembraId(), nivel);
            } catch (RuntimeException e) {
                log.warn("No se pudo revisar el presupuesto de la siembra {}: {}", c.presupuesto().siembraId(), e.getMessage());
            }
        }
        return res;
    }

    private static String pesos(double v) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return f.format(v);
    }
}
