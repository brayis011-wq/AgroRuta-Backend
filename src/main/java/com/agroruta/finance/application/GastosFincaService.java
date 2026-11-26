package com.agroruta.finance.application;

import com.agroruta.finance.application.ports.in.GastosFincaUseCase;
import com.agroruta.finance.application.ports.out.GastosFincaRepository;
import com.agroruta.finance.domain.GastoFinca;
import com.agroruta.finance.domain.ResumenGastos;
import com.agroruta.shared.exception.DatoInvalidoException;
import com.agroruta.shared.exception.ResourceNotFoundException;
import com.agroruta.shared.util.Dinero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GastosFincaService implements GastosFincaUseCase {

    private final GastosFincaRepository repositorio;

    public GastosFincaService(GastosFincaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public GastoFinca registrar(
            Long fincaId, LocalDate fecha, GastoFinca.Categoria categoria, String descripcion, BigDecimal monto) {
        return repositorio.crear(valido(null, fincaId, fecha, categoria, descripcion, monto));
    }

    @Override
    public GastoFinca actualizar(
            Long id,
            Long fincaId,
            LocalDate fecha,
            GastoFinca.Categoria categoria,
            String descripcion,
            BigDecimal monto) {
        repositorio.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Gasto", id));
        return repositorio.actualizar(valido(id, fincaId, fecha, categoria, descripcion, monto));
    }

    @Override
    public void eliminar(Long id) {
        repositorio.buscarPorId(id).orElseThrow(() -> new ResourceNotFoundException("Gasto", id));
        repositorio.eliminar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GastoFinca> listar(Long agricultorId, Long fincaId, LocalDate desde, LocalDate hasta) {
        return repositorio.listar(agricultorId, fincaId, desde, hasta);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenGastos resumen(Long agricultorId, LocalDate desde, LocalDate hasta) {
        return repositorio.resumen(agricultorId, desde, hasta);
    }

    private static GastoFinca valido(
            Long id,
            Long fincaId,
            LocalDate fecha,
            GastoFinca.Categoria categoria,
            String descripcion,
            BigDecimal monto) {
        if (fincaId == null) throw new DatoInvalidoException("Elige la finca.");
        if (fecha == null) throw new DatoInvalidoException("Indica la fecha del gasto.");
        if (monto == null || monto.signum() <= 0) throw new DatoInvalidoException("El valor debe ser mayor a 0.");
        String texto = descripcion == null ? "" : descripcion.trim();
        if (texto.isEmpty()) throw new DatoInvalidoException("Escribe en qué se gastó.");
        return new GastoFinca(
                id,
                fincaId,
                null,
                fecha,
                categoria == null ? GastoFinca.Categoria.OTRO : categoria,
                texto,
                Dinero.redondear(monto));
    }
}
