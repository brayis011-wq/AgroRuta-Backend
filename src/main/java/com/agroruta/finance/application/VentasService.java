package com.agroruta.finance.application;

import com.agroruta.finance.application.ports.in.VentasUseCase;
import com.agroruta.finance.application.ports.out.VentasRepository;
import com.agroruta.finance.domain.ResumenVentas;
import com.agroruta.finance.domain.Venta;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.Dinero;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class VentasService implements VentasUseCase {

    static final Set<String> METODOS = Set.of("EFECTIVO", "TRANSFERENCIA", "CHEQUE");

    private final VentasRepository repositorio;
    private final Clock reloj;

    @org.springframework.beans.factory.annotation.Autowired
    public VentasService(VentasRepository repositorio) {
        this(repositorio, Clock.system(ZoneId.of("America/Bogota")));
    }

    VentasService(VentasRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Override
    public Venta registrar(Long agricultorId, DatosVenta d, boolean pagadoDeContado, String metodo) {
        Venta v = repositorio.crear(agricultorId, Venta.nueva(null, d.siembraId(), d.comprador(), d.fecha(), d.producto(),
                d.cantidadKg(), d.precioKg(), Dinero.redondear(d.total()), d.vence(), d.observaciones()));
        if (pagadoDeContado) {
            repositorio.agregarAbono(new Venta.Abono(null, v.id(), v.fecha(), v.total(), metodo(metodo), "De contado"));
        }
        return buscarPorId(v.id());
    }

    @Override
    public Venta actualizar(Long id, DatosVenta d) {
        Venta actual = buscarPorId(id);
        Venta v = Venta.nueva(id, d.siembraId(), d.comprador(), d.fecha(), d.producto(), d.cantidadKg(), d.precioKg(),
                Dinero.redondear(d.total()), d.vence(), d.observaciones());
        if (v.total().compareTo(actual.abonado()) < 0) {
            throw new DatoInvalidoException("El total no puede quedar por debajo de lo que ya te abonaron ("
                    + pesos(actual.abonado()) + ").");
        }
        repositorio.actualizar(v);
        return buscarPorId(id);
    }

    @Override
    public void eliminar(Long id) {
        buscarPorId(id);
        repositorio.eliminar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Venta buscarPorId(Long id) {
        return repositorio.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Venta", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> listar(Long agricultorId, Venta.Estado estado, String comprador, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(reloj);
        String quien = comprador == null ? "" : comprador.trim().toLowerCase(Locale.ROOT);
        return repositorio.listar(agricultorId, desde, hasta).stream()
                .filter(v -> estado == null || v.estado(hoy) == estado)
                .filter(v -> quien.isEmpty() || v.comprador().toLowerCase(Locale.ROOT).contains(quien))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenVentas resumen(Long agricultorId) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        BigDecimal porCobrar = BigDecimal.ZERO, vencido = BigDecimal.ZERO, vendidoMes = BigDecimal.ZERO;
        int pendientes = 0, vencidas = 0;
        Map<String, BigDecimal> saldos = new LinkedHashMap<>();
        Map<String, Integer> ventas = new LinkedHashMap<>();
        Map<String, Boolean> conVencidas = new LinkedHashMap<>();
        Map<String, String> nombre = new LinkedHashMap<>();
        for (Venta v : repositorio.listar(agricultorId, null, null)) {
            if (!v.fecha().isBefore(inicioMes) && !v.fecha().isAfter(hoy)) vendidoMes = vendidoMes.add(v.total());
            Venta.Estado e = v.estado(hoy);
            if (e == Venta.Estado.PAGADA) continue;
            BigDecimal saldo = v.saldo();
            porCobrar = porCobrar.add(saldo);
            if (e == Venta.Estado.VENCIDA) { vencido = vencido.add(saldo); vencidas++; } else pendientes++;
            String clave = v.comprador().trim().toLowerCase(Locale.ROOT);
            nombre.putIfAbsent(clave, v.comprador().trim());
            saldos.merge(clave, saldo, BigDecimal::add);
            ventas.merge(clave, 1, Integer::sum);
            conVencidas.merge(clave, e == Venta.Estado.VENCIDA, Boolean::logicalOr);
        }
        List<ResumenVentas.Deudor> deudores = new ArrayList<>();
        saldos.forEach((k, s) -> deudores.add(new ResumenVentas.Deudor(nombre.get(k), s, ventas.get(k), conVencidas.get(k))));
        deudores.sort(Comparator.comparing(ResumenVentas.Deudor::saldo).reversed());
        BigDecimal cobradoMes = repositorio.cobradoEntre(agricultorId, inicioMes, hoy);
        return new ResumenVentas(porCobrar, vencido, pendientes, vencidas, vendidoMes,
                cobradoMes == null ? BigDecimal.ZERO : cobradoMes, deudores);
    }

    @Override
    public Venta abonar(Long ventaId, LocalDate fecha, BigDecimal monto, String metodo, String observaciones) {
        Venta v = buscarPorId(ventaId);
        if (fecha == null) throw new DatoInvalidoException("Indica la fecha del pago.");
        if (fecha.isBefore(v.fecha())) throw new DatoInvalidoException("El pago no puede ser anterior a la venta.");
        if (v.saldo().signum() == 0) throw new DatoInvalidoException("Esta venta ya está pagada.");
        if (monto == null || monto.signum() <= 0) throw new DatoInvalidoException("El valor del pago debe ser mayor a 0.");
        BigDecimal m = Dinero.redondear(monto);
        if (m.compareTo(v.saldo()) > 0) {
            throw new DatoInvalidoException("El pago es mayor que lo que te deben (" + pesos(v.saldo()) + ").");
        }
        String obs = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        repositorio.agregarAbono(new Venta.Abono(null, ventaId, fecha, m, metodo(metodo), obs));
        return buscarPorId(ventaId);
    }

    @Override
    public Venta quitarAbono(Long ventaId, Long abonoId) {
        Venta v = buscarPorId(ventaId);
        if (v.abonos().stream().noneMatch(a -> a.id().equals(abonoId))) {
            throw new ResourceNotFoundException("Pago", abonoId);
        }
        repositorio.eliminarAbono(ventaId, abonoId);
        return buscarPorId(ventaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> compradores(Long agricultorId) {
        return repositorio.compradores(agricultorId);
    }

    private static String pesos(BigDecimal v) {
        java.text.NumberFormat f = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return f.format(v);
    }

    private static String metodo(String m) {
        String v = m == null ? "EFECTIVO" : m.trim().toUpperCase(Locale.ROOT);
        if (!METODOS.contains(v)) throw new DatoInvalidoException("Método de pago no válido.");
        return v;
    }
}
