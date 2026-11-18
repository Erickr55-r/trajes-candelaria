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

    private final Servicios sErht;
    private final Runnable alCambiarErht;
    private final VBox listaErht = new VBox(16);
    private final Label cantidadErht = UtilidadesInterfaz.subtitulo("");

    public SolicitudesVista(Servicios s, Runnable alCambiar) {
        this.sErht = s;
        this.alCambiarErht = alCambiar;
        getStyleClass().add("contenido");

        Button actualizarErht = new Button("Actualizar", UtilidadesInterfaz.icono(Feather.REFRESH_CW));
        actualizarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        HBox cabeceraErht = new HBox(10, UtilidadesInterfaz.encabezado("Portal de clientes", "Solicitudes",
                "Reservas pedidas por los clientes desde su portal. Mientras estén pendientes, los trajes quedan "
                        + "apartados: nadie más puede pedirlos en esas fechas."), UtilidadesInterfaz.espacio(), cantidadErht, actualizarErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, listaErht);
        cargarErht();
    }

    private void cargarErht() {
        List<SolicitudFila> filasErht = sErht.alquileres().solicitudesPendientes();
        cantidadErht.setText(filasErht.size() + (filasErht.size() == 1 ? " pendiente" : " pendientes"));
        listaErht.getChildren().clear();
        if (filasErht.isEmpty()) {
            VBox vacioErht = new VBox(8, UtilidadesInterfaz.icono(Feather.CHECK_CIRCLE, 34), UtilidadesInterfaz.seccion("No hay solicitudes pendientes"),
                    UtilidadesInterfaz.subtitulo("Cuando un cliente pida una reserva desde su portal, aparecerá aquí."));
            vacioErht.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            listaErht.getChildren().add(vacioErht);
        }
        for (SolicitudFila f : filasErht) {
            listaErht.getChildren().add(tarjetaErht(f));
        }
        alCambiarErht.run();
    }

    private VBox tarjetaErht(SolicitudFila f) {
        Label clienteErht = new Label(f.cliente());
        clienteErht.getStyleClass().add("solicitud-cliente");
        Label contactoErht = new Label((f.telefono() == null ? "Sin celular" : f.telefono())
                + (f.conjunto() == null ? "" : "  ·  Conjunto " + f.conjunto()), UtilidadesInterfaz.icono(Feather.PHONE, 13));
        contactoErht.getStyleClass().add("nota");
        Label haceErht = new Label(antiguedad(f.creadoEn()), UtilidadesInterfaz.icono(Feather.CLOCK, 13));
        haceErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-hace");
        if (Duration.between(f.creadoEn(), LocalDateTime.now()).toHours() >= 24) {
            haceErht.getStyleClass().add("etiqueta-opcion-hace-viejo");
        }
        Label numeroErht = new Label("N° " + f.idAlquiler());
        numeroErht.getStyleClass().add("traje-codigo");
        HBox cabeceraErht = new HBox(12, new VBox(4, clienteErht, contactoErht), UtilidadesInterfaz.espacio(), numeroErht, haceErht);
        cabeceraErht.setAlignment(Pos.CENTER_LEFT);

        VBox trajesErht = new VBox(4);
        for (AlquilerDetalleTraje t : sErht.alquileres().trajes(f.idAlquiler())) {
            Label l = new Label(t.personaje() + " · " + t.danza() + "  (talla " + t.talla() + ", " + t.codigo() + ")",
                    UtilidadesInterfaz.icono(Feather.CHECK));
            l.getStyleClass().add("pieza-item");
            trajesErht.getChildren().add(l);
        }
        VBox cajaTrajesErht = new VBox(6, tituloErht("Trajes (" + f.numTrajes() + ")"), trajesErht);
        HBox.setHgrow(cajaTrajesErht, Priority.ALWAYS);

        long diasErht = Math.max(1, ChronoUnit.DAYS.between(f.salida(), f.devolucion()));
        Label fechasErht = new Label(UtilidadesInterfaz.fecha(f.salida()) + "  →  " + UtilidadesInterfaz.fecha(f.devolucion()));
        fechasErht.getStyleClass().add("reserva-valor");
        VBox cajaFechasErht = new VBox(6, tituloErht("Fechas"), fechasErht, notaErht(diasErht + (diasErht == 1 ? " día" : " días")));
        cajaFechasErht.setMinWidth(220);

        Label totalErht = new Label(UtilidadesInterfaz.soles(f.montoAlquiler()));
        totalErht.getStyleClass().add("resumen-total-chico");
        VBox cajaTotalErht = new VBox(6, tituloErht("Total alquiler"), totalErht,
                notaErht("+ garantía " + UtilidadesInterfaz.soles(f.montoGarantia())));
        cajaTotalErht.setMinWidth(170);

        HBox datosErht = new HBox(30, cajaFechasErht, cajaTrajesErht, cajaTotalErht);
        datosErht.getStyleClass().add("reserva-datos");

        Button aprobarErht = UtilidadesInterfaz.estilo(new Button("Aprobar", UtilidadesInterfaz.icono(Feather.CHECK)), "boton-primario");
        aprobarErht.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("La solicitud N° " + f.idAlquiler() + " de " + f.cliente() + " pasará a RESERVADO. ¿Aprobar?")) {
                UtilidadesInterfaz.ejecutar(() -> {
                    sErht.alquileres().aprobar(f.idAlquiler());
                    cargarErht();
                });
            }
        });
        Button rechazarErht = UtilidadesInterfaz.estilo(new Button("Rechazar", UtilidadesInterfaz.icono(Feather.X)), "boton-peligro");
        rechazarErht.setOnAction(e -> UtilidadesInterfaz.pedirTexto("Rechazar la solicitud N° " + f.idAlquiler(),
                        "El cliente verá este motivo en «Mis reservas». Los trajes quedarán libres.",
                        "MOTIVO DEL RECHAZO", "", "Ej.: el traje está en reparación para esas fechas")
                .ifPresent(motivo -> UtilidadesInterfaz.ejecutar(() -> {
                    sErht.alquileres().rechazar(f.idAlquiler(), motivo);
                    cargarErht();
                })));
        Button detalleErht = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalleErht.setOnAction(e -> {
            new AlquilerDetalleDialogo(sErht, f.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox accionesErht = new HBox(10);
        if (f.observacion() != null && !f.observacion().isBlank()) {
            Label obsErht = new Label("“" + f.observacion() + "”", UtilidadesInterfaz.icono(Feather.MESSAGE_SQUARE, 13));
            obsErht.getStyleClass().add("nota");
            obsErht.setWrapText(true);
            obsErht.setMinWidth(0);
            HBox.setHgrow(obsErht, Priority.ALWAYS);
            obsErht.setMaxWidth(Double.MAX_VALUE);
            accionesErht.getChildren().add(obsErht);
        } else {
            accionesErht.getChildren().add(UtilidadesInterfaz.espacio());
        }
        accionesErht.getChildren().addAll(detalleErht, rechazarErht, aprobarErht);
        accionesErht.setAlignment(Pos.CENTER_LEFT);

        VBox cajaErht = new VBox(16, cabeceraErht, datosErht, accionesErht);
        cajaErht.getStyleClass().add("reserva-tarjeta");
        return cajaErht;
    }

    static String antiguedad(LocalDateTime desde) {
        Duration d = Duration.between(desde, LocalDateTime.now());
        long minErht = Math.max(0, d.toMinutes());
        if (minErht < 1) {
            return "hace un momento";
        }
        if (minErht < 60) {
            return "hace " + minErht + " min";
        }
        long h = d.toHours();
        if (h < 24) {
            return "hace " + h + " h";
        }
        long diasErht = d.toDays();
        return "hace " + diasErht + (diasErht == 1 ? " día" : " días");
    }

    private static Label tituloErht(String t) {
        Label l = new Label(UtilidadesInterfaz.espaciado(t));
        l.getStyleClass().add("campo-titulo");
        return l;
    }

    private static Label notaErht(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("nota");
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }
}
