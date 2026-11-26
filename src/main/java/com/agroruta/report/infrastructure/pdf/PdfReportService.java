package com.agroruta.report.infrastructure.pdf;

import com.agroruta.shared.exception.FalloInternoException;
import com.agroruta.report.domain.CostosTemporada;
import com.agroruta.report.domain.CropDetail;
import com.agroruta.report.domain.CropReport;
import com.agroruta.report.domain.MonthlyPayment;
import com.agroruta.report.domain.PayrollReport;
import com.agroruta.report.domain.RentabilidadLote;
import com.agroruta.report.domain.ReporteFinanciero;
import com.agroruta.report.domain.TemporadaFinanzas;
import com.agroruta.report.domain.WorkerPayrollDetail;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Barra;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Celda;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Columna;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Segmento;
import com.agroruta.report.infrastructure.pdf.PdfLienzo.Tarjeta;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.agroruta.report.infrastructure.pdf.FormatoReporte.*;
import static com.agroruta.report.infrastructure.pdf.PdfLienzo.*;

/**
 * PDF de los reportes de nómina, cultivos, costos y rentabilidad (cada uno con resumen, gráficas y tabla
 * de detalle), por separado o juntos en un solo archivo.
 */
@Service
public class PdfReportService {

    /** Trabajadores que se muestran en la gráfica (la tabla los muestra todos) */
    private static final int MAX_BARRAS = 10;

    private static final Map<String, Color> COLOR_ETAPA = new LinkedHashMap<>();
    static {
        COLOR_ETAPA.put("GERMINACION", new Color(0xA5, 0xD6, 0xA7));
        COLOR_ETAPA.put("CRECIMIENTO", new Color(0x66, 0xBB, 0x6A));
        COLOR_ETAPA.put("PRODUCCION", new Color(0x2E, 0x7D, 0x32));
        COLOR_ETAPA.put("COSECHA", new Color(0xF9, 0xA8, 0x25));
        COLOR_ETAPA.put("FINALIZADO", new Color(0xB0, 0xB8, 0xB2));
    }

    public byte[] generatePayrollPdf(PayrollReport report) {
        return generatePayrollPdf(report, null);
    }

    public byte[] generateCropPdf(CropReport report) {
        return generateCropPdf(report, null);
    }

    // ══ Nómina ═════════════════════════════════════════════════════════════

    public byte[] generatePayrollPdf(PayrollReport r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Nómina", SUB_NOMINA)) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);
            nomina(pdf, r);
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando PDF de nómina", e);
        }
    }

    private static final String SUB_NOMINA = "Pagos realizados a tus trabajadores y lo que queda por pagar";
    private static final String SUB_CULTIVOS = "Siembras, etapas y cosechas de tus fincas";
    private static final String SUB_COSTOS = "Lo que costó y lo que dejó cada temporada";
    private static final String SUB_RENTABILIDAD = "Cuánto dejó cada lote y cómo le fue a cada temporada frente a la anterior";

    private void nomina(PdfLienzo pdf, PayrollReport r) throws IOException {
            List<WorkerPayrollDetail> ws = r.workerDetails();
            int jornales = ws.stream().map(WorkerPayrollDetail::totalJornales).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            int pagos = ws.stream().map(WorkerPayrollDetail::totalPagos).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            BigDecimal promedio = ws.isEmpty() ? BigDecimal.ZERO
                    : r.totalAccumulated().divide(BigDecimal.valueOf(ws.size()), 0, RoundingMode.HALF_UP);

            pdf.tarjetas(List.of(
                    new Tarjeta("Total pagado", pesos(r.totalAccumulated()), pagos + (pagos == 1 ? " pago" : " pagos"), VERDE),
                    new Tarjeta("Pendiente por pagar", pesos(r.totalPendiente()),
                            r.nominasPendientes() + (r.nominasPendientes() == 1 ? " nómina" : " nóminas"), AMBAR),
                    new Tarjeta("Trabajadores pagados", String.valueOf(ws.size()), "Promedio " + pesos(promedio), new Color(0x1F, 0x5F, 0xA8)),
                    new Tarjeta("Jornales liquidados", numero(jornales, 0), "en las nóminas pagadas", new Color(0x81, 0xC7, 0x84))));

            if (!r.pagosPorMes().isEmpty()) {
                pdf.seccion("Pagos por mes");
                List<MonthlyPayment> meses = r.pagosPorMes();
                List<Barra> barras = new ArrayList<>();
                for (int i = 0; i < meses.size(); i++) {
                    MonthlyPayment m = meses.get(i);
                    boolean cambiaAnio = i > 0 && !m.mes().substring(0, 4).equals(meses.get(i - 1).mes().substring(0, 4));
                    barras.add(new Barra(mesCorto(m.mes(), cambiaAnio), m.total().doubleValue(), pesosCorto(m.total()), null));
                }
                if (barras.stream().allMatch(b -> b.valor() == 0)) pdf.nota("No hubo pagos en los últimos " + meses.size() + " meses.");
                else pdf.barrasVerticales(barras, 120);
            }

            pdf.seccion("Pagado por trabajador");
            if (ws.isEmpty()) {
                pdf.nota("Todavía no hay pagos registrados.");
            } else {
                double total = r.totalAccumulated().doubleValue();
                pdf.barrasHorizontales(ws.stream().limit(MAX_BARRAS).map(w -> new Barra(
                        w.workerName(), valor(w.totalPaid()), pesos(w.totalPaid()),
                        total > 0 ? numero(valor(w.totalPaid()) * 100 / total, 0) + " %" : null)).toList());
                if (ws.size() > MAX_BARRAS) pdf.nota("Y " + (ws.size() - MAX_BARRAS) + " trabajadores más en la tabla.");

                pdf.seccion("Detalle por trabajador");
                pdf.tabla(List.of(
                        new Columna("Trabajador", 165, false),
                        new Columna("Período pagado", 150, false),
                        new Columna("Jornales", 60, true),
                        new Columna("Pagos", 50, true),
                        new Columna("Total pagado", 90, true)),
                        ws.stream().map(w -> List.of(
                                Celda.negrita(w.workerName()),
                                Celda.de(periodo(w.periodoInicio(), w.periodoFin())),
                                Celda.de(numero(w.totalJornales() != null ? w.totalJornales() : 0, 0)),
                                Celda.de(String.valueOf(w.totalPagos() != null ? w.totalPagos() : 0)),
                                new Celda(pesos(w.totalPaid()), null, VERDE_OSCURO, true))).toList());
            }
    }

    // ══ Cultivos ═══════════════════════════════════════════════════════════

    public byte[] generateCropPdf(CropReport r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Cultivos", SUB_CULTIVOS)) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);
            cultivos(pdf, r);
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando PDF de cultivos", e);
        }
    }

    private void cultivos(PdfLienzo pdf, CropReport r) throws IOException {
            List<CropDetail> cs = r.cropDetails();
            long activas = cs.stream().filter(c -> !"FINALIZADO".equals(c.estado())).count();
            double kg = cs.stream().mapToDouble(c -> c.totalKgCosechado() != null ? c.totalKgCosechado() : 0).sum();
            int cosechas = cs.stream().mapToInt(CropDetail::totalCosechas).sum();
            long tipos = cs.stream().map(PdfReportService::cultivoDe).distinct().count();

            pdf.tarjetas(List.of(
                    new Tarjeta("Siembras", String.valueOf(cs.size()), tipos + (tipos == 1 ? " cultivo" : " cultivos"), VERDE),
                    new Tarjeta("Activas", String.valueOf(activas), (cs.size() - activas) + " finalizadas", new Color(0x66, 0xBB, 0x6A)),
                    new Tarjeta("Kg cosechados", numero(kg, kg % 1 == 0 ? 0 : 1) + " kg", "en total", AMBAR),
                    new Tarjeta("Cosechas", String.valueOf(cosechas),
                            cs.isEmpty() ? "—" : numero(kg / Math.max(1, cosechas), 1) + " kg por cosecha", new Color(0x1F, 0x5F, 0xA8))));

            if (cs.isEmpty()) {
                pdf.seccion("Siembras");
                pdf.nota("Todavía no hay siembras registradas.");
                return;
            }

            pdf.seccion("Siembras por etapa");
            Map<String, Integer> porEtapa = new LinkedHashMap<>();
            COLOR_ETAPA.keySet().forEach(k -> porEtapa.put(k, 0));
            cs.forEach(c -> porEtapa.merge(c.estado(), 1, Integer::sum));
            pdf.barraApilada(porEtapa.entrySet().stream()
                    .map(e -> new Segmento(etapa(e.getKey()), e.getValue(), COLOR_ETAPA.getOrDefault(e.getKey(), TINTA_3)))
                    .toList());

            Map<String, Double> kgPorCultivo = new LinkedHashMap<>();
            cs.forEach(c -> kgPorCultivo.merge(cultivoDe(c), c.totalKgCosechado() != null ? c.totalKgCosechado() : 0, Double::sum));
            if (kgPorCultivo.values().stream().anyMatch(v -> v > 0)) {
                pdf.seccion("Kg cosechados por cultivo");
                pdf.barrasHorizontales(kgPorCultivo.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(MAX_BARRAS)
                        .map(e -> new Barra(e.getKey(), e.getValue(), numero(e.getValue(), e.getValue() % 1 == 0 ? 0 : 1) + " kg",
                                kg > 0 ? numero(e.getValue() * 100 / kg, 0) + " %" : null))
                        .toList());
            }

            pdf.seccion("Detalle de siembras");
            pdf.tabla(List.of(
                    new Columna("Cultivo · variedad", 128, false),
                    new Columna("Finca · lote", 108, false),
                    new Columna("Etapa", 76, false),
                    new Columna("Sembrado", 72, false),
                    new Columna("Días", 34, true),
                    new Columna("Kg", 44, true),
                    new Columna("Cosechas", 53, true)),
                    cs.stream().map(c -> List.of(
                            Celda.negrita(c.variedad()),
                            Celda.de(ubicacion(c)),
                            new Celda(etapa(c.estado()), suave(COLOR_ETAPA.get(c.estado())), oscuro(c.estado()), true),
                            Celda.de(fecha(c.fechaSiembra())),
                            Celda.de(numero(c.diasDesdeSiembra(), 0)),
                            Celda.de(kg(c.totalKgCosechado())),
                            Celda.de(String.valueOf(c.totalCosechas())))).toList());
    }

    // ══ Costos ═════════════════════════════════════════════════════════════

    public byte[] generateCostsPdf(ReporteFinanciero r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Costos", SUB_COSTOS)) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);
            costos(pdf, r);
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando PDF de costos", e);
        }
    }

    private void costos(PdfLienzo pdf, ReporteFinanciero r) throws IOException {
        List<TemporadaFinanzas> con = r.temporadas().stream().filter(t -> t.costos() != null).toList();
        double costo = con.stream().mapToDouble(t -> t.costos().costoTotal()).sum();
        double ingresos = con.stream().mapToDouble(t -> t.costos().ingresos()).sum();
        double kg = con.stream().mapToDouble(t -> t.costos().kgTotal()).sum();
        boolean conPrecio = con.stream().anyMatch(t -> t.costos().kgConPrecio() > 0);
        double manoObra = con.stream().mapToDouble(t -> t.costos().manoObra()).sum();
        double insumos = con.stream().mapToDouble(t -> t.costos().insumos()).sum();
        double otros = con.stream().mapToDouble(t -> t.costos().otros()).sum();
        int jornales = con.stream().mapToInt(t -> t.costos().jornales()).sum();
        int n = r.temporadas().size();

        pdf.tarjetas(List.of(
                new Tarjeta("Costo total", pesosDe(costo), n + (n == 1 ? " temporada" : " temporadas"), AMBAR),
                new Tarjeta("Ingresos", pesosDe(ingresos), kgTexto(kg) + " cosechados", VERDE),
                new Tarjeta("Ganancia", conPrecio ? pesosConSigno(ingresos - costo) : "Sin precios",
                        conPrecio ? "ingresos menos costos" : "registra el precio de tus cosechas",
                        conPrecio && ingresos - costo < 0 ? ROJO : VERDE_OSCURO),
                new Tarjeta("Costo por kilo", kg > 0 ? pesosDe(costo / kg) : "—", kg > 0 ? "de lo cosechado" : "sin cosechas",
                        new Color(0x1F, 0x5F, 0xA8))));

        if (con.isEmpty()) {
            pdf.seccion("Temporadas");
            pdf.nota(n == 0 ? "Todavía no hay siembras registradas." : "No se pudieron calcular los costos de las temporadas.");
            return;
        }

        if (costo > 0) {
            pdf.seccion("En qué se fue la plata");
            List<Barra> partes = new ArrayList<>();
            partes.add(new Barra("Mano de obra", manoObra, pesosDe(manoObra), porcentajeDe(manoObra, costo)));
            partes.add(new Barra("Insumos (fumigaciones)", insumos, pesosDe(insumos), porcentajeDe(insumos, costo)));
            partes.add(new Barra("Otros (materiales)", otros, pesosDe(otros), porcentajeDe(otros, costo)));
            pdf.barrasHorizontales(partes);
            if (jornales > 0) pdf.nota(numero(jornales, 0) + (jornales == 1 ? " jornal" : " jornales") + " cargados a las temporadas.");

            pdf.seccion("Costo por temporada");
            List<TemporadaFinanzas> porCosto = con.stream()
                    .filter(t -> t.costos().costoTotal() > 0)
                    .sorted(Comparator.comparingDouble((TemporadaFinanzas t) -> t.costos().costoTotal()).reversed())
                    .toList();
            pdf.barrasHorizontales(porCosto.stream().limit(MAX_BARRAS).map(t -> new Barra(
                    // Con el lote, para distinguir dos temporadas del mismo cultivo
                    t.siembra().loteNombre() != null ? t.siembra().variedad() + " · " + t.siembra().loteNombre() : t.siembra().variedad(),
                    t.costos().costoTotal(), pesosDe(t.costos().costoTotal()),
                    porcentajeDe(t.costos().costoTotal(), costo))).toList());
            if (porCosto.size() > MAX_BARRAS) pdf.nota("Y " + (porCosto.size() - MAX_BARRAS) + " temporadas más en la tabla.");
        }

        pdf.seccion("Detalle por temporada");
        List<TemporadaFinanzas> recientes = r.temporadas().stream()
                .sorted(Comparator.comparing((TemporadaFinanzas t) -> t.siembra().fechaSiembra(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        pdf.tabla(List.of(
                new Columna("Temporada", 104, false),
                new Columna("Finca · lote", 124, false),
                new Columna("Costo", 72, true),
                new Columna("Ingresos", 72, true),
                new Columna("Ganancia", 80, true),
                new Columna("Costo/kg", 63, true)),
                recientes.stream().map(t -> {
                    CostosTemporada c = t.costos();
                    String nombre = t.siembra().variedad() + (t.siembra().eliminada() ? " (eliminada)" : "");
                    if (c == null) {
                        return List.of(Celda.negrita(nombre), Celda.de(ubicacion(t.siembra())),
                                Celda.de("—"), Celda.de("—"), Celda.de("—"), Celda.de("—"));
                    }
                    return List.of(
                            Celda.negrita(nombre),
                            Celda.de(ubicacion(t.siembra())),
                            Celda.de(pesosDe(c.costoTotal())),
                            Celda.de(pesosDe(c.ingresos())),
                            celdaGanancia(c.ganancia()),
                            Celda.de(c.costoPorKg() != null ? pesosDe(c.costoPorKg()) : "—"));
                }).toList());
        long sinPrecio = con.stream().filter(t -> t.costos().ganancia() == null && t.costos().kgTotal() > 0).count();
        if (sinPrecio > 0) {
            pdf.nota(sinPrecio + (sinPrecio == 1 ? " temporada tiene" : " temporadas tienen")
                    + " cosechas sin precio de venta: su ganancia no se puede calcular.");
        }
    }

    // ══ Rentabilidad ═══════════════════════════════════════════════════════

    public byte[] generateProfitabilityPdf(ReporteFinanciero r, String preparadoPara) {
        try (PdfLienzo pdf = new PdfLienzo("Reporte de Rentabilidad", SUB_RENTABILIDAD)) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(r.generatedAt()),
                    preparadoPara != null ? "Para " + preparadoPara : null);
            rentabilidad(pdf, r);
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando PDF de rentabilidad", e);
        }
    }

    private void rentabilidad(PdfLienzo pdf, ReporteFinanciero r) throws IOException {
        List<RentabilidadLote> lotes = RentabilidadLote.porLote(r.temporadas());
        List<RentabilidadLote> con = lotes.stream().filter(l -> l.ganancia() != null).toList();
        double ingresos = con.stream().mapToDouble(RentabilidadLote::ingresos).sum();
        Double ganancia = con.isEmpty() ? null : con.stream().mapToDouble(RentabilidadLote::ganancia).sum();
        RentabilidadLote mejor = con.isEmpty() ? null : con.get(0);

        pdf.tarjetas(List.of(
                new Tarjeta("Lotes", String.valueOf(lotes.size()), con.size() + " con ventas", new Color(0x1F, 0x5F, 0xA8)),
                new Tarjeta("Ganancia total", ganancia != null ? pesosConSigno(ganancia) : "Sin ventas", "de todos los lotes",
                        ganancia != null && ganancia < 0 ? ROJO : VERDE_OSCURO),
                new Tarjeta("Margen", ganancia != null && ingresos > 0 ? porcentaje(ganancia / ingresos * 100) : "—",
                        "de cada peso vendido", VERDE),
                new Tarjeta("Mejor lote", mejor != null ? mejor.loteNombre() : "—",
                        mejor != null ? pesosConSigno(mejor.ganancia()) : "sin ventas todavía", AMBAR)));

        if (lotes.isEmpty()) {
            pdf.seccion("Lotes");
            pdf.nota("Todavía no hay temporadas con costos para comparar.");
            return;
        }

        if (!con.isEmpty()) {
            pdf.seccion("Ganancia por lote");
            pdf.barrasHorizontales(con.stream().limit(MAX_BARRAS).map(l -> new Barra(
                    nombreLote(l), Math.abs(l.ganancia()), pesosConSigno(l.ganancia()),
                    l.margen() != null ? porcentaje(l.margen()) : null,
                    l.ganancia() < 0 ? ROJO : null)).toList());
            if (con.size() > MAX_BARRAS) pdf.nota("Y " + (con.size() - MAX_BARRAS) + " lotes más en la tabla.");
        }

        pdf.seccion("Temporadas de cada lote");
        List<List<Celda>> filas = new ArrayList<>();
        for (RentabilidadLote l : lotes) {
            boolean primera = true;
            for (RentabilidadLote.Temporada t : l.temporadas()) {
                filas.add(List.of(
                        primera ? Celda.negrita(l.loteNombre()) : Celda.de(""),
                        Celda.de(t.nombre() + (t.enCurso() ? " (en curso)" : "")),
                        Celda.de(pesosDe(t.costo())),
                        Celda.de(pesosDe(t.ingresos())),
                        celdaGanancia(t.ganancia()),
                        t.cambio() == null ? Celda.de("—")
                                : new Celda(pesosConSigno(t.cambio()), null, t.cambio() < 0 ? ROJO : VERDE_OSCURO, false)));
                primera = false;
            }
        }
        pdf.tabla(List.of(
                new Columna("Lote", 80, false),
                new Columna("Temporada", 135, false),
                new Columna("Costo", 70, true),
                new Columna("Ingresos", 70, true),
                new Columna("Ganancia", 80, true),
                new Columna("Vs. anterior", 80, true)), filas);
        pdf.nota("Vs. anterior: cuánto ganó de más (o de menos) frente a la temporada anterior del mismo lote.");
        gastosGenerales(pdf, r.gastos(), ganancia);
    }

    /** Gastos de las fincas que no son de una siembra: se restan para ver la ganancia real */
    private void gastosGenerales(PdfLienzo pdf, com.agroruta.report.domain.GastosGenerales g, Double ganancia) throws IOException {
        if (g == null || g.total() <= 0) return;
        pdf.seccion("Gastos generales de las fincas");
        double real = (ganancia == null ? 0 : ganancia) - g.total();
        pdf.tarjetas(List.of(
                new Tarjeta("Gastos generales", pesosDe(g.total()), "transporte, herramientas, servicios…", AMBAR),
                new Tarjeta("Ganancia real", pesosConSigno(real), "lotes menos gastos generales",
                        real < 0 ? ROJO : VERDE_OSCURO)));
        List<List<Celda>> filas = new ArrayList<>();
        for (var p : g.porCategoria()) {
            filas.add(List.of(Celda.de(CATEGORIAS_GASTO.getOrDefault(p.categoria(), capitalizar(p.categoria()))),
                    Celda.de(pesosDe(p.total()))));
        }
        pdf.tabla(List.of(new Columna("Categoría", 300, false), new Columna("Total", 215, true)), filas);
    }

    private static final Map<String, String> CATEGORIAS_GASTO = Map.of(
            "TRANSPORTE", "Transporte", "HERRAMIENTAS", "Herramientas", "SERVICIOS", "Servicios",
            "MANTENIMIENTO", "Mantenimiento", "ADMINISTRACION", "Administración", "IMPUESTOS", "Impuestos",
            "OTRO", "Otros");

    // ══ Cuaderno de campo ══════════════════════════════════════════════════

    private static final java.time.format.DateTimeFormatter DIA_CUADERNO = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Cuaderno de campo: todas las aplicaciones de producto del periodo (fecha, lugar, producto, dosis,
     * carencia y reentrada), como lo piden las certificaciones y algunos compradores.
     */
    public byte[] generateFieldBookPdf(List<com.agroruta.report.domain.AplicacionCampo> aplicaciones,
                                       com.agroruta.report.domain.Periodo periodo, String finca, String preparadoPara) {
        String sub = "Registro de aplicaciones de productos" + (finca != null ? " · " + finca : "")
                + " · " + textoPeriodo(periodo);
        try (PdfLienzo pdf = new PdfLienzo("Cuaderno de campo", sub)) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(LocalDateTime.now()),
                    preparadoPara != null ? "Productor: " + preparadoPara : null);
            long productos = aplicaciones.stream().map(a -> a.producto() == null ? "" : a.producto().toLowerCase())
                    .distinct().count();
            long lotes = aplicaciones.stream().map(a -> a.finca() + "|" + a.lote()).distinct().count();
            pdf.tarjetas(List.of(
                    new Tarjeta("Aplicaciones", String.valueOf(aplicaciones.size()), "en el periodo", VERDE_OSCURO),
                    new Tarjeta("Productos", String.valueOf(productos), "distintos", new Color(0x1F, 0x5F, 0xA8)),
                    new Tarjeta("Lotes", String.valueOf(lotes), "con aplicaciones", AMBAR)));
            pdf.seccion("Aplicaciones");
            if (aplicaciones.isEmpty()) {
                pdf.nota("No hay fumigaciones ni fertilizaciones registradas en este periodo.");
            } else {
                List<List<PdfLienzo.Celda2>> filas = new ArrayList<>();
                for (var a : aplicaciones) {
                    String dosis = a.dosis() == null ? "—" : numeroCorto(a.dosis()) + " " + unidad(a.unidadDosis());
                    List<String> detalleDosis = new ArrayList<>();
                    if (a.areaAplicada() != null) detalleDosis.add(numeroCorto(a.areaAplicada()) + " ha");
                    if (a.cantidadUsada() != null) detalleDosis.add("gastado " + numeroCorto(a.cantidadUsada()) + " " + unidad(a.unidadCantidad()));
                    boolean conCarencia = a.finCarencia() != null;
                    filas.add(List.of(
                            new PdfLienzo.Celda2(a.fecha().format(DIA_CUADERNO), null, false),
                            new PdfLienzo.Celda2(a.finca() + " / " + a.lote(), a.cultivo(), false),
                            new PdfLienzo.Celda2(a.producto(), a.tipoProducto() != null ? tipoProducto(a.tipoProducto()) : null, true),
                            new PdfLienzo.Celda2(dosis, detalleDosis.isEmpty() ? null : String.join(" · ", detalleDosis), false),
                            new PdfLienzo.Celda2(conCarencia ? a.diasCarencia() + " días" : "—",
                                    conCarencia ? "desde " + a.finCarencia().format(DIA_CUADERNO) : null, false),
                            new PdfLienzo.Celda2(a.reentradaHoras() == null || a.reentradaHoras() <= 0 ? "—" : a.reentradaHoras() + " h", null, false)));
                }
                pdf.tablaDosLineas(List.of(
                        new Columna("Fecha", 60, false),
                        new Columna("Finca / lote y cultivo", 120, false),
                        new Columna("Producto", 128, false),
                        new Columna("Dosis", 90, false),
                        new Columna("Carencia", 62, false),
                        new Columna("Reentrada", 55, true)), filas);
                pdf.notaLarga("Carencia: días sin cosechar después de aplicar; debajo, el primer día en que se puede cosechar. "
                        + "Reentrada: horas sin entrar al lote. En la dosis, el área aplicada y el producto gastado en total.");
            }
            pdf.asegurar(70);
            pdf.y -= 30;
            pdf.linea(MARGEN, pdf.y, MARGEN + 220, pdf.y, TINTA_3, 0.6f);
            pdf.texto("Firma del responsable técnico", NORMAL, 8.5f, MARGEN, pdf.y - 12, TINTA_2);
            pdf.linea(MARGEN + 280, pdf.y, MARGEN + CONTENIDO, pdf.y, TINTA_3, 0.6f);
            pdf.texto("Firma del productor", NORMAL, 8.5f, MARGEN + 280, pdf.y - 12, TINTA_2);
            pdf.y -= 24;
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando el cuaderno de campo", e);
        }
    }

    private static String textoPeriodo(com.agroruta.report.domain.Periodo p) {
        if (p == null || p.esTodo()) return "todo el historial";
        if (p.desde() == null) return "hasta el " + p.hasta().format(DIA_CUADERNO);
        if (p.hasta() == null) return "desde el " + p.desde().format(DIA_CUADERNO);
        return p.desde().format(DIA_CUADERNO) + " a " + p.hasta().format(DIA_CUADERNO);
    }

    private static String numeroCorto(double v) {
        java.text.NumberFormat f = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(2);
        return f.format(v);
    }

    private static String unidad(String u) {
        if (u == null) return "";
        return switch (u) {
            case "LITROS" -> "L";
            case "ML" -> "ml";
            case "GRAMOS" -> "g";
            case "KG" -> "kg";
            default -> u.toLowerCase();
        };
    }

    private static String tipoProducto(String t) {
        return capitalizar(t.replace('_', ' ').toLowerCase());
    }

    // ══ Reporte completo ═══════════════════════════════════════════════════

    /**
     * Las partes elegidas en un solo PDF, cada una desde una página nueva. Una parte en null no se incluye
     * (al menos una debe venir).
     */
    public byte[] generateCompletePdf(PayrollReport nomina, CropReport cultivos, ReporteFinanciero finanzas,
                                      boolean conCostos, boolean conRentabilidad, String preparadoPara) {
        List<String> nombres = new ArrayList<>();
        if (nomina != null) nombres.add("nómina");
        if (cultivos != null) nombres.add("cultivos");
        if (finanzas != null && conCostos) nombres.add("costos");
        if (finanzas != null && conRentabilidad) nombres.add("rentabilidad");
        if (nombres.isEmpty()) throw new IllegalArgumentException("El reporte completo necesita al menos una parte");

        try (PdfLienzo pdf = new PdfLienzo("Reporte de la finca", capitalizar(enumerar(nombres)))) {
            pdf.encabezadoDerecha("Generado el " + fechaHora(LocalDateTime.now()),
                    preparadoPara != null ? "Para " + preparadoPara : null);
            boolean primera = true;
            if (nomina != null) {
                pdf.parte("Nómina", SUB_NOMINA, primera);
                nomina(pdf, nomina);
                primera = false;
            }
            if (cultivos != null) {
                pdf.parte("Cultivos", SUB_CULTIVOS, primera);
                cultivos(pdf, cultivos);
                primera = false;
            }
            if (finanzas != null && conCostos) {
                pdf.parte("Costos", SUB_COSTOS, primera);
                costos(pdf, finanzas);
                primera = false;
            }
            if (finanzas != null && conRentabilidad) {
                pdf.parte("Rentabilidad", SUB_RENTABILIDAD, primera);
                rentabilidad(pdf, finanzas);
            }
            return pdf.terminar();
        } catch (IOException e) {
            throw new FalloInternoException("Error generando el reporte completo", e);
        }
    }

    // ── Apoyo ──────────────────────────────────────────────────────────────

    /** 420,5 o 1.240 (sin decimales cuando es exacto) */
    private static String kg(Double v) {
        double x = v != null ? v : 0;
        return numero(x, x % 1 == 0 ? 0 : 1);
    }

    private static double valor(BigDecimal v) {
        return v != null ? v.doubleValue() : 0;
    }

    private static String pesosDe(double v) {
        return FormatoReporte.pesos(BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP));
    }

    /** "-$300.000" para una pérdida (la fuente del PDF no tiene el signo menos tipográfico) */
    private static String pesosConSigno(double v) {
        return v < 0 ? "-" + pesosDe(-v) : pesosDe(v);
    }

    private static String porcentaje(double v) {
        return numero(v, Math.abs(v) < 10 ? 1 : 0) + " %";
    }

    private static String porcentajeDe(double parte, double total) {
        return total > 0 ? numero(parte * 100 / total, 0) + " %" : null;
    }

    private static String kgTexto(double kg) {
        return numero(kg, kg % 1 == 0 ? 0 : 1) + " kg";
    }

    /** Ganancia en verde, pérdida en rojo, y "Sin precio" cuando no hay ventas con precio */
    private static Celda celdaGanancia(Double g) {
        if (g == null) return new Celda("Sin precio", null, TINTA_3, false);
        return new Celda(pesosConSigno(g), null, g < 0 ? ROJO : VERDE_OSCURO, true);
    }

    private static String nombreLote(RentabilidadLote l) {
        return l.fincaNombre() != null ? l.loteNombre() + " · " + l.fincaNombre() : l.loteNombre();
    }

    /** "nómina, cultivos y costos" */
    private static String enumerar(List<String> partes) {
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
    }

    private static String capitalizar(String t) {
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Cultivo para agrupar: el tipo si existe, si no lo que va antes de " · " */
    private static String cultivoDe(CropDetail c) {
        if (c.cultivo() != null && !c.cultivo().isBlank()) return c.cultivo();
        String v = c.variedad() != null ? c.variedad() : "—";
        int i = v.indexOf(" · ");
        return i > 0 ? v.substring(0, i) : v;
    }

    private static String ubicacion(CropDetail c) {
        if (c.fincaNombre() == null && c.loteNombre() == null) return "—";
        if (c.fincaNombre() == null) return c.loteNombre();
        if (c.loteNombre() == null) return c.fincaNombre();
        return c.fincaNombre() + " · " + c.loteNombre();
    }

    /** Versión clara del color de una etapa, para el fondo de la píldora */
    private static Color suave(Color c) {
        if (c == null) return VERDE_SUAVE;
        return new Color((c.getRed() + 255 * 3) / 4, (c.getGreen() + 255 * 3) / 4, (c.getBlue() + 255 * 3) / 4);
    }

    private static Color oscuro(String etapa) {
        return switch (etapa == null ? "" : etapa) {
            case "COSECHA" -> new Color(0x8A, 0x5A, 0x00);
            case "FINALIZADO" -> TINTA_3;
            default -> VERDE_OSCURO;
        };
    }
}
