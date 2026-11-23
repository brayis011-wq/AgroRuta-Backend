package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.PagosDelMes;
import java.time.LocalDate;
import com.agroruta.worker.domain.ResumenPagos;
import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.domain.Pago;

import java.util.List;
import java.util.Optional;

public interface PagoRepository {

    Pago guardar(Pago pago);
    Optional<Pago> buscarPorId(Long id);
    Optional<Pago> buscarPorNomina(Long nominaId);
    List<Pago> buscarPorTrabajador(Long trabajadorId);
    List<Pago> listarTodos();
    List<Pago> listarPorAgricultor(Long agricultorId);

    /** Pagos más recientes primero. agricultorId null = todos (Administrador); nominaId opcional. */
    /** trabajadorId opcional: solo los pagos de ese trabajador */
    Pagina<Pago> pagina(Long agricultorId, Long nominaId, Long trabajadorId, int pagina, int tamano);

    default Pagina<Pago> pagina(Long agricultorId, Long nominaId, int pagina, int tamano) {
        return pagina(agricultorId, nominaId, null, pagina, tamano);
    }

    /** Cantidad, total y trabajadores distintos, con el mismo filtro de la página. */
    ResumenPagos resumen(Long agricultorId, Long nominaId, Long trabajadorId);

    default ResumenPagos resumen(Long agricultorId, Long nominaId) {
        return resumen(agricultorId, nominaId, null);
    }

    /** Total y número de pagos de cada mes desde una fecha, del más antiguo al más reciente. Los meses sin pagos no salen. */
    List<PagosDelMes> totalesPorMes(Long agricultorId, LocalDate desde);
}
