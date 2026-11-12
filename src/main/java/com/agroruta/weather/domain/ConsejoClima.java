package com.agroruta.weather.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Algo que el clima cambia en lo que el agricultor tiene programado: "no fumigues hoy en el lote X,
 * llueve a las 3 p. m.". La pantalla y el correo arman el texto con estos datos.
 *
 * @param lote          lote de la tarea (null en los avisos de toda la finca: helada, lluvia fuerte)
 * @param dia           día de la tarea
 * @param motivo        LLUVIA o VIENTO en los consejos de fumigación; null en los demás
 * @param problemaDesde cuándo empieza la lluvia, el viento o la helada
 * @param ventanaInicio mejor rato para fumigar (null si el pronóstico no muestra ninguno)
 * @param valor         milímetros de lluvia (NO_REGAR, LLUVIA_FUERTE) o temperatura mínima (HELADA)
 */
public record ConsejoClima(Tipo tipo, Nivel nivel, Long fincaId, String finca, String lote, String cultivo,
                           LocalDate dia, String motivo, LocalDateTime problemaDesde,
                           LocalDateTime ventanaInicio, LocalDateTime ventanaFin, Double valor) {

    public enum Tipo { NO_FUMIGAR, FUMIGAR_EN_VENTANA, FUMIGAR_BIEN, NO_REGAR, HELADA, LLUVIA_FUERTE }

    public enum Nivel { ALERTA, PRECAUCION, BIEN }
}
