package com.agroruta.report.infrastructure.adapter;

import com.agroruta.crop.domain.EstadoCultivo;
import com.agroruta.crop.infrastructure.persistence.FincaEntity;
import com.agroruta.crop.infrastructure.persistence.JpaCosechaRepository;
import com.agroruta.crop.infrastructure.persistence.JpaFincaRepository;
import com.agroruta.crop.infrastructure.persistence.JpaLoteRepository;
import com.agroruta.crop.infrastructure.persistence.JpaSiembraRepository;
import com.agroruta.crop.infrastructure.persistence.LoteEntity;
import com.agroruta.crop.infrastructure.persistence.SiembraEntity;
import com.agroruta.report.domain.CropDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CropQueryAdapter - siembras para los reportes")
class CropQueryAdapterTest {

    @Mock private JpaSiembraRepository jpaSiembraRepository;
    @Mock private JpaCosechaRepository jpaCosechaRepository;
    @Mock private JpaLoteRepository jpaLoteRepository;
    @Mock private JpaFincaRepository jpaFincaRepository;
    @InjectMocks private CropQueryAdapter adapter;

    private static SiembraEntity siembra(long id, Long loteId, String tipo, String variedad) {
        SiembraEntity s = new SiembraEntity();
        s.setId(id);
        s.setLoteId(loteId);
        s.setTipoCultivo(tipo);
        s.setVariedad(variedad);
        s.setEstadoCultivo(EstadoCultivo.PRODUCCION);
        s.setFechaSiembra(LocalDate.now().minusDays(40));
        return s;
    }

    private static LoteEntity lote(long id, long fincaId, String nombre, double area) {
        LoteEntity l = new LoteEntity();
        l.setId(id);
        l.setFincaId(fincaId);
        l.setNombre(nombre);
        l.setArea(area);
        return l;
    }

    private static FincaEntity finca(long id, String nombre) {
        FincaEntity f = new FincaEntity();
        f.setId(id);
        f.setNombre(nombre);
        return f;
    }

    @Test
    @DisplayName("arma el detalle con lote, finca y cosechas sin consultar siembra por siembra")
    void armaElDetalleEnBloque() {
        when(jpaSiembraRepository.findByAgricultorId(7L)).thenReturn(List.of(
                siembra(1L, 10L, "Café", "Castillo"),
                siembra(2L, 10L, null, "Hass"),
                siembra(3L, 11L, "Plátano", "Hartón")));
        when(jpaLoteRepository.findAllById(any())).thenReturn(List.of(
                lote(10L, 100L, "Lote alto", 1.5), lote(11L, 100L, "Lote bajo", 2.0)));
        when(jpaFincaRepository.findAllById(any())).thenReturn(List.of(finca(100L, "La Esperanza")));
        when(jpaCosechaRepository.totalesPorSiembra(7L)).thenReturn(List.of(
                new Object[]{1L, 120.5, 3L},
                new Object[]{3L, 40.0, 1L}));

        List<CropDetail> detalle = adapter.getCropDetailsByAgricultor(7L);

        assertThat(detalle).extracting(CropDetail::siembraId).containsExactly(1L, 2L, 3L);
        assertThat(detalle.get(0).variedad()).isEqualTo("Café · Castillo");
        assertThat(detalle.get(0).totalKgCosechado()).isEqualTo(120.5);
        assertThat(detalle.get(0).totalCosechas()).isEqualTo(3);
        assertThat(detalle.get(0).loteNombre()).isEqualTo("Lote alto");
        assertThat(detalle.get(0).fincaNombre()).isEqualTo("La Esperanza");
        assertThat(detalle.get(0).loteAreaHa()).isEqualTo(1.5);
        assertThat(detalle.get(0).eliminada()).isFalse();
        // siembra antigua (solo variedad) y sin cosechas
        assertThat(detalle.get(1).variedad()).isEqualTo("Hass");
        assertThat(detalle.get(1).totalKgCosechado()).isNull();
        assertThat(detalle.get(1).totalCosechas()).isZero();
        assertThat(detalle.get(2).loteNombre()).isEqualTo("Lote bajo");

        // una consulta por tabla, no una por siembra
        verify(jpaLoteRepository, times(1)).findAllById(any());
        verify(jpaFincaRepository, times(1)).findAllById(any());
        verify(jpaCosechaRepository, times(1)).totalesPorSiembra(7L);
        verify(jpaLoteRepository, never()).findById(anyLong());
        verify(jpaFincaRepository, never()).findById(anyLong());
        verify(jpaCosechaRepository, never()).sumCantidadKgBySiembraId(anyLong());
        verify(jpaCosechaRepository, never()).countBySiembraId(anyLong());
    }

    @Test
    @DisplayName("marca como eliminada la siembra cuyo lote o finca se eliminó")
    void marcaEliminadas() {
        SiembraEntity borrada = siembra(1L, 10L, "Café", "Castillo");
        borrada.setEliminadoEn(LocalDateTime.now());
        LoteEntity loteBorrado = lote(11L, 100L, "Lote viejo", 1.0);
        loteBorrado.setEliminadoEn(LocalDateTime.now());
        FincaEntity fincaBorrada = finca(101L, "Finca vieja");
        fincaBorrada.setEliminadoEn(LocalDateTime.now());

        when(jpaSiembraRepository.findAll()).thenReturn(List.of(
                borrada,
                siembra(2L, 11L, "Café", "Castillo"),
                siembra(3L, 12L, "Café", "Castillo"),
                siembra(4L, 10L, "Café", "Castillo")));
        when(jpaLoteRepository.findAllById(any())).thenReturn(List.of(
                lote(10L, 100L, "Lote alto", 1.5), loteBorrado, lote(12L, 101L, "Lote de finca vieja", 1.0)));
        when(jpaFincaRepository.findAllById(any())).thenReturn(List.of(finca(100L, "La Esperanza"), fincaBorrada));
        when(jpaCosechaRepository.totalesPorSiembra(null)).thenReturn(List.of());

        List<CropDetail> detalle = adapter.getAllCropDetails();

        assertThat(detalle).extracting(CropDetail::eliminada).containsExactly(true, true, true, false);
    }

    @Test
    @DisplayName("la siembra sin lote (dato viejo) sale sin lote ni finca")
    void siembraSinLote() {
        when(jpaSiembraRepository.findAll()).thenReturn(List.of(siembra(1L, 99L, "Café", "Castillo")));
        when(jpaLoteRepository.findAllById(any())).thenReturn(List.of());
        when(jpaFincaRepository.findAllById(any())).thenReturn(List.of());
        when(jpaCosechaRepository.totalesPorSiembra(null)).thenReturn(List.of());

        CropDetail d = adapter.getAllCropDetails().get(0);

        assertThat(d.loteNombre()).isNull();
        assertThat(d.fincaNombre()).isNull();
        assertThat(d.loteAreaHa()).isNull();
        assertThat(d.eliminada()).isFalse();
    }

    @Test
    @DisplayName("sin siembras no consulta nada más")
    void sinSiembras() {
        when(jpaSiembraRepository.findByAgricultorId(7L)).thenReturn(List.of());

        assertThat(adapter.getCropDetailsByAgricultor(7L)).isEmpty();

        verifyNoInteractions(jpaLoteRepository, jpaFincaRepository, jpaCosechaRepository);
    }
}
