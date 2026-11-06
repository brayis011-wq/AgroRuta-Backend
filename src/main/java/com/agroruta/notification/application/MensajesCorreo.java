package com.agroruta.notification.application;

import com.agroruta.crop.domain.AlertaCultivo;
import com.agroruta.notification.domain.Correo;
import com.agroruta.notification.domain.PlantillaCorreo;
import com.agroruta.weather.domain.Pronostico;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Los textos de cada correo. Todos en español, como el resto de los mensajes del backend. */
@Component
public class MensajesCorreo {

    static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("d 'de' MMMM 'a las' h:mm a", ES);

    private static final String PIE_CUENTA = "Recibes este correo porque tienes una cuenta en AgroRuta.";
    private static final String PIE_NO_FUISTE = "Si no fuiste tú, cambia tu contraseña lo antes posible.";

    static final Map<String, String> LABORES = Map.of(
            "DESHIERBE", "Deshierbe", "PODA", "Poda", "REVISION", "Revisión", "RIEGO", "Riego",
            "TUTOREO", "Tutoreo", "OTRO", "Labor");

    private final String frontend;

    public MensajesCorreo(@Value("${app.frontend-url:http://localhost:4200}") String frontend) {
        this.frontend = frontend.endsWith("/") ? frontend.substring(0, frontend.length() - 1) : frontend;
    }

    String url(String ruta) {
        return frontend + ruta;
    }

    // ── Cuenta y seguridad ───────────────────────────────────────────

    public Correo recuperarContrasena(String nombre, String email, String token, long minutos) {
        return PlantillaCorreo.con("Crea una contraseña nueva")
                .resumen("Usa este enlace en los próximos " + minutos + " minutos.")
                .parrafo("Hola " + primerNombre(nombre) + ", recibimos una solicitud para cambiar la contraseña de tu cuenta.")
                .boton("Crear contraseña nueva", url("/auth/restablecer?token=" + token))
                .parrafo("El enlace sirve una sola vez y vence en " + minutos + " minutos.")
                .parrafo("Si no pediste el cambio, ignora este correo: tu contraseña sigue igual.")
                .pie(PIE_CUENTA)
                .para(email, "Recupera tu contraseña de AgroRuta");
    }

    public Correo verificarCorreo(String nombre, String email, String token, long horas) {
        return PlantillaCorreo.con("Confirma tu correo")
                .resumen("Un paso más para empezar a usar AgroRuta.")
                .parrafo("Hola " + primerNombre(nombre) + ", gracias por registrarte en AgroRuta.")
                .parrafo("Confirma que este correo es tuyo para activar tu cuenta:")
                .boton("Confirmar mi correo", url("/auth/verificar?token=" + token))
                .parrafo("El enlace vence en " + horas + " horas. Si no creaste esta cuenta, ignora este correo.")
                .pie(PIE_CUENTA)
                .para(email, "Confirma tu correo en AgroRuta");
    }

    public Correo contrasenaCambiada(String nombre, String email, LocalDateTime cuando) {
        return PlantillaCorreo.con("Tu contraseña cambió")
                .resumen("Se cerraron las sesiones abiertas en otros equipos.")
                .parrafo("Hola " + primerNombre(nombre) + ", la contraseña de tu cuenta se cambió el "
                        + cuando.format(HORA) + ".")
                .parrafo("Por seguridad cerramos las sesiones abiertas en otros equipos.")
                .aviso(PIE_NO_FUISTE + " Puedes crear una nueva desde el enlace de abajo.")
                .boton("Recuperar mi cuenta", url("/auth/recuperar"))
                .pie(PIE_CUENTA)
                .para(email, "Tu contraseña de AgroRuta cambió");
    }

    public Correo cuentaBloqueada(String nombre, String email, long minutos) {
        return PlantillaCorreo.con("Bloqueamos el inicio de sesión por " + minutos + " minutos")
                .resumen("Hubo varios intentos con una contraseña equivocada.")
                .parrafo("Hola " + primerNombre(nombre) + ", alguien intentó entrar a tu cuenta varias veces con una "
                        + "contraseña equivocada, así que pausamos el inicio de sesión " + minutos + " minutos.")
                .parrafo("Si fuiste tú y no recuerdas la contraseña, crea una nueva:")
                .boton("Crear contraseña nueva", url("/auth/recuperar"))
                .aviso("Si no fuiste tú, tu contraseña sigue protegida. Te recomendamos cambiarla por una que no uses en otros sitios.")
                .pie(PIE_CUENTA)
                .para(email, "Intentos fallidos en tu cuenta de AgroRuta");
    }

    // ── Trabajadores y nómina ────────────────────────────────────────

    public Correo solicitudAcceso(String agricultor, String email, String trabajador, String cuenta, String cuentaEmail) {
        return PlantillaCorreo.con(trabajador + " pidió acceso al portal")
                .resumen("Apruébalo para que vea sus jornales, nóminas y pagos.")
                .parrafo("Hola " + primerNombre(agricultor) + ", la cuenta " + nombreYCorreo(cuenta, cuentaEmail)
                        + " se registró con la cédula de " + trabajador + ".")
                .parrafo("Si la apruebas, podrá ver sus jornales, nóminas y pagos (solo los suyos).")
                .boton("Revisar la solicitud", url("/worker"))
                .pie(PIE_CUENTA)
                .pie("Puedes dejar de recibir estos avisos en Configuración: " + url("/configuracion"))
                .para(email, "Solicitud de acceso de " + trabajador);
    }

    public Correo accesoResuelto(String nombre, String email, boolean aprobado, boolean retirado, String empleador) {
        String quien = empleador == null || empleador.isBlank() ? "Tu empleador" : empleador;
        if (!aprobado && retirado) {
            return PlantillaCorreo.con("Tu acceso al portal fue retirado")
                    .resumen("Ya no puedes ver los datos de este trabajador.")
                    .parrafo("Hola " + primerNombre(nombre) + ", " + quien + " retiró tu acceso al portal del trabajador.")
                    .parrafo("Si crees que es un error, habla con tu empleador.")
                    .pie(PIE_CUENTA)
                    .para(email, "Tu acceso a AgroRuta fue retirado");
        }
        PlantillaCorreo p = aprobado
                ? PlantillaCorreo.con("Ya puedes entrar a tu portal")
                    .resumen(quien + " aprobó tu acceso.")
                    .parrafo("Hola " + primerNombre(nombre) + ", " + quien + " aprobó tu acceso.")
                    .parrafo("Desde ahora ves tus jornales, nóminas y pagos en AgroRuta.")
                    .boton("Entrar al portal", url("/portal/inicio"))
                : PlantillaCorreo.con("Tu solicitud de acceso no fue aprobada")
                    .resumen("Revisa la cédula o habla con tu empleador.")
                    .parrafo("Hola " + primerNombre(nombre) + ", " + quien + " no aprobó tu solicitud de acceso al portal.")
                    .parrafo("Si crees que es un error, revisa que la cédula sea la correcta y vuelve a enviarla, "
                            + "o habla con tu empleador.")
                    .boton("Ver mi solicitud", url("/portal/inicio"));
        return p.pie(PIE_CUENTA).para(email, aprobado ? "Tu acceso a AgroRuta fue aprobado" : "Tu solicitud de acceso en AgroRuta");
    }

    public Correo pagoRegistrado(String nombre, String email, LocalDate desde, LocalDate hasta,
                                 BigDecimal monto, String metodo, LocalDate fechaPago) {
        String valor = pesos(monto);
        return PlantillaCorreo.con("Te pagaron " + valor)
                .resumen("Nómina del " + desde.format(DIA_CORTO) + " al " + hasta.format(DIA_CORTO) + ".")
                .parrafo("Hola " + primerNombre(nombre) + ", se registró el pago de tu nómina.")
                .lista(List.of(
                        new String[]{"Periodo:", desde.format(DIA_CORTO) + " al " + hasta.format(DIA_CORTO)},
                        new String[]{"Valor:", valor},
                        new String[]{"Forma de pago:", metodoPago(metodo)},
                        new String[]{"Fecha:", fechaPago.format(DIA_CORTO)}))
                .boton("Ver mis pagos", url("/portal/pagos"))
                .pie(PIE_CUENTA)
                .para(email, "Pago de nómina: " + valor);
    }

    // ── Resumen diario del cultivo ───────────────────────────────────

    /** Clima de una finca para las próximas 24 horas */
    public record ClimaFinca(String finca, Pronostico pronostico) {}

    public Correo resumenDiario(String nombre, String email, LocalDate hoy,
                                List<AlertaCultivo> alertas, List<ClimaFinca> clima) {
        List<String[]> fumigar = new ArrayList<>(), labores = new ArrayList<>(),
                restricciones = new ArrayList<>(), tiempo = new ArrayList<>();

        for (AlertaCultivo a : alertas) {
            String donde = donde(a);
            switch (a.tipo()) {
                case FUMIGACION_ATRASADA -> fumigar.add(new String[]{donde, "· fumigación atrasada "
                        + dias(a.detalle()) + " (tocaba el " + a.fecha().format(DIA_CORTO) + ")."});
                case FUMIGACION_HOY -> fumigar.add(new String[]{donde, "· hoy toca fumigar."});
                case FUMIGACION_MANANA -> fumigar.add(new String[]{donde, "· mañana toca fumigar."});
                case LABOR_HOY -> labores.add(new String[]{donde, "· hoy: " + labor(a)});
                case LABOR_MANANA -> labores.add(new String[]{donde, "· mañana: " + labor(a)});
                case REENTRADA_EN_CURSO -> restricciones.add(new String[]{donde, "· no entrar hasta el "
                        + a.fecha().format(DIA_CORTO) + " (se aplicó " + a.titulo() + ")."});
                case REENTRADA_LIBRE -> restricciones.add(new String[]{donde, "· desde hoy ya se puede entrar ("
                        + a.titulo() + ")."});
                case CARENCIA_LIBRE -> restricciones.add(new String[]{donde, "· desde hoy ya se puede cosechar (terminó la carencia de "
                        + a.titulo() + ")."});
            }
        }
        boolean hayFumigacion = alertas.stream().anyMatch(AlertaCultivo::esFumigacionPendiente);
        for (ClimaFinca c : clima) {
            Pronostico p = c.pronostico();
            List<String> partes = new ArrayList<>();
            if (p.riesgoHelada()) partes.add("riesgo de helada (mínima de " + grados(p.tempMin()) + ")");
            if (p.lluviaFuerte()) partes.add("lluvia fuerte (" + mm(p.lluviaMm()) + ")");
            else if (hayFumigacion && p.lluviaLavaProducto()) partes.add("lluvia (" + mm(p.lluviaMm())
                    + "): si fumigas, hazlo cuando no vaya a llover para que el producto no se lave");
            if (!partes.isEmpty()) tiempo.add(new String[]{c.finca(), "· " + String.join("; ", partes) + "."});
        }

        int total = fumigar.size() + labores.size() + restricciones.size() + tiempo.size();
        PlantillaCorreo p = PlantillaCorreo.con("Tu cultivo hoy, " + hoy.format(DIA))
                .resumen(total == 1 ? "Tienes 1 aviso para hoy." : "Tienes " + total + " avisos para hoy.")
                .parrafo("Hola " + primerNombre(nombre) + ", esto es lo que conviene tener en cuenta hoy:");
        if (!tiempo.isEmpty()) p.seccion("Clima (próximas 24 horas)").lista(tiempo);
        if (!fumigar.isEmpty()) p.seccion("Fumigaciones").lista(fumigar);
        if (!labores.isEmpty()) p.seccion("Labores programadas").lista(labores);
        if (!restricciones.isEmpty()) p.seccion("Reentrada y cosecha").lista(restricciones);
        return p.boton("Abrir el calendario", url("/cultivo/calendario"))
                .pie(PIE_CUENTA)
                .pie("Puedes dejar de recibir este resumen en Configuración: " + url("/configuracion"))
                .para(email, "AgroRuta: " + (total == 1 ? "1 aviso" : total + " avisos") + " para hoy");
    }

    // ── auxiliares ───────────────────────────────────────────────────

    private static String donde(AlertaCultivo a) {
        String lote = a.lote() == null ? "" : a.lote();
        String finca = a.finca() == null ? "" : a.finca();
        String base = finca.isEmpty() ? lote : lote.isEmpty() ? finca : finca + " / " + lote;
        return a.cultivo() == null || a.cultivo().isBlank() ? base : base + " (" + a.cultivo() + ")";
    }

    private static String labor(AlertaCultivo a) {
        String nombre = LABORES.getOrDefault(a.titulo(), "Labor");
        return a.detalle() == null || a.detalle().isBlank() ? nombre + "." : nombre + " — " + a.detalle() + ".";
    }

    private static String dias(String n) {
        return "1".equals(n) ? "1 día" : n + " días";
    }

    static String primerNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        return nombre.trim().split("\\s+")[0];
    }

    private static String nombreYCorreo(String nombre, String email) {
        if (nombre == null || nombre.isBlank()) return email;
        return email == null ? nombre : nombre + " (" + email + ")";
    }

    static String pesos(BigDecimal monto) {
        NumberFormat f = NumberFormat.getCurrencyInstance(ES);
        f.setMaximumFractionDigits(0);
        return f.format(monto == null ? BigDecimal.ZERO : monto);
    }

    private static String metodoPago(String metodo) {
        if (metodo == null) return "—";
        return switch (metodo) {
            case "EFECTIVO" -> "Efectivo";
            case "TRANSFERENCIA" -> "Transferencia";
            case "CHEQUE" -> "Cheque";
            default -> metodo;
        };
    }

    private static String grados(double t) {
        return String.format(ES, "%.0f °C", t);
    }

    private static String mm(double v) {
        return String.format(ES, "%.0f mm", v);
    }
}
