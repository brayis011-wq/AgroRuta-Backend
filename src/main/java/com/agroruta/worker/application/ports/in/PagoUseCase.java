package com.agroruta.worker.application.ports.in;

import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.domain.MetodoPago;
import com.agroruta.worker.domain.Pago;
import com.agroruta.worker.domain.PagosDelMes;
import com.agroruta.worker.domain.ResumenPagos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoUseCase {

    Pago registrarPago(
            Long nominaId, BigDecimal monto, MetodoPago metodoPago, String comprobante, String observaciones);

    /**
     * Paga varias nóminas juntas, cada una por su valor total y con el mismo método. Con
     * aprobarPendientes, las que siguen pendientes se aprueban antes. Todo o nada.
     */
    List<Pago> pagarVarias(
            Long agricultorId,
            List<Long> nominaIds,
            MetodoPago metodoPago,
            String observaciones,
            boolean aprobarPendientes);

    Pago buscarPorId(Long id);

    List<Pago> historialPorTrabajador(Long trabajadorId);

    List<Pago> listarTodos();

    List<Pago> listarTodosDe(Long agricultorId);

    Pagina<Pago> pagina(Long agricultorId, Long nominaId, Long trabajadorId, int pagina, int tamano);

    default Pagina<Pago> pagina(Long agricultorId, Long nominaId, int pagina, int tamano) {
        return pagina(agricultorId, nominaId, null, pagina, tamano);
    }

    ResumenPagos resumen(Long agricultorId, Long nominaId, Long trabajadorId);

    default ResumenPagos resumen(Long agricultorId, Long nominaId) {
        return resumen(agricultorId, nominaId, null);
    }

    /** Lo pagado cada mes desde una fecha. agricultorId null = todos (Administrador). */
    List<PagosDelMes> totalesPorMes(Long agricultorId, LocalDate desde);
}
