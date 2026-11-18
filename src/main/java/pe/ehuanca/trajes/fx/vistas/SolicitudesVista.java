/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerDetalleTraje;
import pe.ehuanca.trajes.modelo.SolicitudFila;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Solicitudes de clientes hechas desde el portal (estado SOLICITADO), de la más antigua a la más reciente.
 * Aprobar → RESERVADO; Rechazar → RECHAZADO con un motivo que el cliente verá.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class SolicitudesVista extends VBox {

    private final Servicios s;
    /** Avisa al panel para actualizar la insignia del menú. */
    private final Runnable alCambiar;
    private final VBox lista = new VBox(16);
    private final Label cantidad = UtilidadesInterfaz.subtitulo("");

    public SolicitudesVista(Servicios s, Runnable alCambiar) {
        this.s = s;
        this.alCambiar = alCambiar;
        getStyleClass().add("contenido");

        Button actualizar = new Button("Actualizar", UtilidadesInterfaz.icono(Feather.REFRESH_CW));
        actualizar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        HBox cabecera = new HBox(10, UtilidadesInterfaz.encabezado("Portal de clientes", "Solicitudes",
                "Reservas pedidas por los clientes desde su portal. Mientras estén pendientes, los trajes quedan "
                        + "apartados: nadie más puede pedirlos en esas fechas."), UtilidadesInterfaz.espacio(), cantidad, actualizar);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, lista);
        cargar();
    }

    private void cargar() {
        List<SolicitudFila> filas = s.alquileres().solicitudesPendientes();
        cantidad.setText(filas.size() + (filas.size() == 1 ? " pendiente" : " pendientes"));
        lista.getChildren().clear();
        if (filas.isEmpty()) {
            VBox vacio = new VBox(8, UtilidadesInterfaz.icono(Feather.CHECK_CIRCLE, 34), UtilidadesInterfaz.seccion("No hay solicitudes pendientes"),
                    UtilidadesInterfaz.subtitulo("Cuando un cliente pida una reserva desde su portal, aparecerá aquí."));
            vacio.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            lista.getChildren().add(vacio);
        }
        for (SolicitudFila f : filas) {
            lista.getChildren().add(tarjeta(f));
        }
        alCambiar.run();
    }

    private VBox tarjeta(SolicitudFila f) {
        Label cliente = new Label(f.cliente());
        cliente.getStyleClass().add("solicitud-cliente");
        Label contacto = new Label((f.telefono() == null ? "Sin celular" : f.telefono())
                + (f.conjunto() == null ? "" : "  ·  Conjunto " + f.conjunto()), UtilidadesInterfaz.icono(Feather.PHONE, 13));
        contacto.getStyleClass().add("nota");
        Label hace = new Label(antiguedad(f.creadoEn()), UtilidadesInterfaz.icono(Feather.CLOCK, 13));
        hace.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-hace");
        if (Duration.between(f.creadoEn(), LocalDateTime.now()).toHours() >= 24) {
            hace.getStyleClass().add("etiqueta-opcion-hace-viejo");   // más de un día esperando: resalta
        }
        Label numero = new Label("N° " + f.idAlquiler());
        numero.getStyleClass().add("traje-codigo");
        HBox cabecera = new HBox(12, new VBox(4, cliente, contacto), UtilidadesInterfaz.espacio(), numero, hace);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        // trajes
        VBox trajes = new VBox(4);
        for (AlquilerDetalleTraje t : s.alquileres().trajes(f.idAlquiler())) {
            Label l = new Label(t.personaje() + " · " + t.danza() + "  (talla " + t.talla() + ", " + t.codigo() + ")",
                    UtilidadesInterfaz.icono(Feather.CHECK));
            l.getStyleClass().add("pieza-item");
            trajes.getChildren().add(l);
        }
        VBox cajaTrajes = new VBox(6, titulo("Trajes (" + f.numTrajes() + ")"), trajes);
        HBox.setHgrow(cajaTrajes, Priority.ALWAYS);

        long dias = Math.max(1, ChronoUnit.DAYS.between(f.salida(), f.devolucion()));
        Label fechas = new Label(UtilidadesInterfaz.fecha(f.salida()) + "  →  " + UtilidadesInterfaz.fecha(f.devolucion()));
        fechas.getStyleClass().add("reserva-valor");
        VBox cajaFechas = new VBox(6, titulo("Fechas"), fechas, nota(dias + (dias == 1 ? " día" : " días")));
        cajaFechas.setMinWidth(220);

        Label total = new Label(UtilidadesInterfaz.soles(f.montoAlquiler()));
        total.getStyleClass().add("resumen-total-chico");
        VBox cajaTotal = new VBox(6, titulo("Total alquiler"), total,
                nota("+ garantía " + UtilidadesInterfaz.soles(f.montoGarantia())));
        cajaTotal.setMinWidth(170);

        HBox datos = new HBox(30, cajaFechas, cajaTrajes, cajaTotal);
        datos.getStyleClass().add("reserva-datos");

        Button aprobar = UtilidadesInterfaz.estilo(new Button("Aprobar", UtilidadesInterfaz.icono(Feather.CHECK)), "boton-primario");
        aprobar.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("La solicitud N° " + f.idAlquiler() + " de " + f.cliente() + " pasará a RESERVADO. ¿Aprobar?")) {
                UtilidadesInterfaz.ejecutar(() -> {
                    s.alquileres().aprobar(f.idAlquiler());
                    cargar();
                });
            }
        });
        Button rechazar = UtilidadesInterfaz.estilo(new Button("Rechazar", UtilidadesInterfaz.icono(Feather.X)), "boton-peligro");
        rechazar.setOnAction(e -> UtilidadesInterfaz.pedirTexto("Rechazar la solicitud N° " + f.idAlquiler(),
                        "El cliente verá este motivo en «Mis reservas». Los trajes quedarán libres.",
                        "MOTIVO DEL RECHAZO", "", "Ej.: el traje está en reparación para esas fechas")
                .ifPresent(motivo -> UtilidadesInterfaz.ejecutar(() -> {
                    s.alquileres().rechazar(f.idAlquiler(), motivo);
                    cargar();
                })));
        Button detalle = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalle.setOnAction(e -> {
            new AlquilerDetalleDialogo(s, f.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        HBox acciones = new HBox(10);
        if (f.observacion() != null && !f.observacion().isBlank()) {
            Label obs = new Label("“" + f.observacion() + "”", UtilidadesInterfaz.icono(Feather.MESSAGE_SQUARE, 13));
            obs.getStyleClass().add("nota");
            obs.setWrapText(true);
            obs.setMinWidth(0);
            HBox.setHgrow(obs, Priority.ALWAYS);
            obs.setMaxWidth(Double.MAX_VALUE);
            acciones.getChildren().add(obs);
        } else {
            acciones.getChildren().add(UtilidadesInterfaz.espacio());
        }
        acciones.getChildren().addAll(detalle, rechazar, aprobar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(16, cabecera, datos, acciones);
        caja.getStyleClass().add("reserva-tarjeta");
        return caja;
    }

    /** "hace 5 min", "hace 2 h", "hace 3 días". */
    static String antiguedad(LocalDateTime desde) {
        Duration d = Duration.between(desde, LocalDateTime.now());
        long min = Math.max(0, d.toMinutes());
        if (min < 1) {
            return "hace un momento";
        }
        if (min < 60) {
            return "hace " + min + " min";
        }
        long h = d.toHours();
        if (h < 24) {
            return "hace " + h + " h";
        }
        long dias = d.toDays();
        return "hace " + dias + (dias == 1 ? " día" : " días");
    }

    private static Label titulo(String t) {
        Label l = new Label(UtilidadesInterfaz.espaciado(t));
        l.getStyleClass().add("campo-titulo");
        return l;
    }

    private static Label nota(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("nota");
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }
}
