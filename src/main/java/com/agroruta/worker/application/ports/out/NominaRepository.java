package com.agroruta.worker.application.ports.out;

import com.agroruta.worker.domain.ResumenNominas;
import com.agroruta.shared.util.Pagina;
import com.agroruta.worker.domain.EstadoNomina;
import com.agroruta.worker.domain.Nomina;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NominaRepository {

    Nomina guardar(Nomina nomina);
    Optional<Nomina> buscarPorId(Long id);
    List<Nomina> buscarPorTrabajador(Long trabajadorId);
    List<Nomina> buscarPorTrabajadorYEstado(Long trabajadorId, EstadoNomina estado);
    List<Nomina> buscarPorPeriodo(LocalDate inicio, LocalDate fin);
    List<Nomina> listarPorAgricultor(Long agricultorId);
    List<Nomina> listarTodas();
    void eliminar(Long id);

    /** Nóminas más recientes primero. agricultorId null = todas (Administrador); los demás filtros son opcionales. */
    Pagina<Nomina> pagina(Long agricultorId, Long trabajadorId, EstadoNomina estado, int pagina, int tamano);

    /** Cantidad y total por estado, con los mismos filtros de la página. */
    List<ResumenNominas> resumenPorEstado(Long agricultorId, Long trabajadorId);
}
