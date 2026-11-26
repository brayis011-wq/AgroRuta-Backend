package com.agroruta.worker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.agroruta.worker.application.ports.out.CargoRepository;
import com.agroruta.worker.domain.Cargo;
import com.agroruta.worker.domain.ModalidadPago;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cargos típicos de una finca")
class CargosTipicosTest {

    @Mock
    CargoRepository cargos;

    @InjectMocks
    CargoService servicio;

    @Test
    void agregaSoloLosQueFaltanConSuFormaDePago() {
        when(cargos.existsByNombreAndAgricultorId(anyString(), eq(7L)))
                .thenAnswer(i -> "Fumigador".equals(i.getArgument(0)));
        when(cargos.guardar(any())).thenAnswer(i -> i.getArgument(0));

        List<Cargo> creados = servicio.crearTipicos(7L);

        assertThat(creados)
                .extracting(Cargo::getNombre)
                .doesNotContain("Fumigador")
                .contains("Jornalero", "Recolector al kilo", "Mayordomo");
        assertThat(creados).allSatisfy(c -> assertThat(c.getAgricultorId()).isEqualTo(7L));
        Cargo kilo = creados.stream()
                .filter(c -> c.getNombre().equals("Recolector al kilo"))
                .findFirst()
                .orElseThrow();
        assertThat(kilo.getModalidadPago()).isEqualTo(ModalidadPago.KILO);
        assertThat(kilo.getValorKilo()).isEqualByComparingTo("900");
    }
}
