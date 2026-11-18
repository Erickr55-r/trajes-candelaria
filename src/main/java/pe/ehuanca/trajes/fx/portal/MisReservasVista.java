/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerDetalleTraje;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.feather.Feather;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Mis reservas: una tarjeta por reserva con su línea de tiempo (Solicitado → Reservado → Entregado → Devuelto).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class MisReservasVista extends VBox {

    private static final List<String> PASOS = List.of("SOLICITADO", "RESERVADO", "ENTREGADO", "DEVUELTO");
    private static final List<String> NOMBRES = List.of("Solicitado", "Reservado", "Entregado", "Devuelto");

    private final Servicios sErht;
    private final VistaPortalCliente portalErht;

    public MisReservasVista(Servicios s, VistaPortalCliente portal) {
        this.sErht = s;
        this.portalErht = portal;
        getStyleClass().add("contenido");
        int idClienteErht = Sesion.actual().idCliente();

        List<AlquilerResumen> listaErht = s.alquileres().delCliente(idClienteErht);
        Map<Integer, String> motivosErht = s.alquileres().motivosRechazo(idClienteErht);

        getChildren().add(UtilidadesInterfaz.encabezado("Mi cuenta", "Mis reservas",
                "Sigue cada reserva paso a paso. Las solicitudes quedan en revisión hasta que la tienda las confirme."));
        if (listaErht.isEmpty()) {
            Button ir = UtilidadesInterfaz.estilo(new Button("Ver trajes", UtilidadesInterfaz.icono(Feather.ARROW_RIGHT)), "boton-primario");
            ir.setOnAction(e -> portal.ir(VistaPortalCliente.CATALOGO));
            VBox vacioErht = new VBox(10, UtilidadesInterfaz.seccion("Todavía no tienes reservas"),
                    UtilidadesInterfaz.subtitulo("Elige un traje del catálogo y solicita tu reserva."), ir);
            vacioErht.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            getChildren().add(vacioErht);
            return;
        }
        for (AlquilerResumen a : listaErht) {
            getChildren().add(tarjetaErht(a, motivosErht.get(a.idAlquiler()), idClienteErht));
        }
    }

    private VBox tarjetaErht(AlquilerResumen a, String motivo, int idCliente) {
        Label tituloErht = new Label("Reserva N° " + a.idAlquiler());
        tituloErht.getStyleClass().add("reserva-titulo");
        Label insigniaErht = UtilidadesInterfaz.insigniaEstado(a.estado());
        HBox cabeceraErht = new HBox(12, tituloErht, insigniaErht, UtilidadesInterfaz.espacio());
        cabeceraErht.setAlignment(Pos.CENTER_LEFT);
        if ("SOLICITADO".equals(a.estado())) {
            Button cancelarErht = UtilidadesInterfaz.estilo(new Button("Cancelar solicitud", UtilidadesInterfaz.icono(Feather.X_CIRCLE)), "boton-peligro");
            cancelarErht.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿Cancelar tu solicitud N° " + a.idAlquiler() + "? Los trajes quedarán libres.")) {
                    UtilidadesInterfaz.ejecutar(() -> {
                        sErht.alquileres().cancelarSolicitud(a.idAlquiler(), idCliente);
                        portalErht.ir(VistaPortalCliente.MIS_RESERVAS);
                    });
                }
            });
            cabeceraErht.getChildren().add(cancelarErht);
        }

        VBox cajaErht = new VBox(18, cabeceraErht, lineaDeTiempoErht(a.estado()));

        if ("RECHAZADO".equals(a.estado())) {
            Label m = new Label("La tienda no pudo aceptar esta solicitud: " + (motivo == null ? "—" : motivo),
                    UtilidadesInterfaz.icono(Feather.INFO));
            m.getStyleClass().add("error-suave");
            m.setWrapText(true);
            m.setMaxWidth(Double.MAX_VALUE);
            cajaErht.getChildren().add(m);
        } else if ("ANULADO".equals(a.estado())) {
            Label m = new Label("Esta reserva fue cancelada.", UtilidadesInterfaz.icono(Feather.INFO));
            m.getStyleClass().add("aviso-gris");
            cajaErht.getChildren().add(m);
        }

        long diasErht = Math.max(1, ChronoUnit.DAYS.between(a.fechaSalida(), a.fechaDevolucionPrevista()));
        VBox fechasErht = datoErht("Fechas", UtilidadesInterfaz.fecha(a.fechaSalida()) + "  →  " + UtilidadesInterfaz.fecha(a.fechaDevolucionPrevista()),
                diasErht + (diasErht == 1 ? " día" : " días") + (a.conjunto() != null && !"PARTICULAR".equals(a.conjunto())
                        ? " · " + a.conjunto() : ""));
        List<AlquilerDetalleTraje> trajesErht = sErht.alquileres().trajesDelCliente(a.idAlquiler(), idCliente);
        VBox listaTrajesErht = new VBox(4);
        for (AlquilerDetalleTraje t : trajesErht) {
            Label l = new Label(t.personaje() + " · " + t.danza() + "  (talla " + t.talla() + ", " + t.codigo() + ")",
                    UtilidadesInterfaz.icono(Feather.CHECK));
            l.getStyleClass().add("pieza-item");
            listaTrajesErht.getChildren().add(l);
        }
        Label tTrajesErht = new Label(UtilidadesInterfaz.espaciado("Trajes (" + trajesErht.size() + ")"));
        tTrajesErht.getStyleClass().add("campo-titulo");
        VBox cajaTrajesErht = new VBox(6, tTrajesErht, listaTrajesErht);
        HBox.setHgrow(cajaTrajesErht, Priority.ALWAYS);

        boolean cuentaErht = !List.of("RECHAZADO", "ANULADO").contains(a.estado());
        VBox montosErht = new VBox(6,
                montoErht("Total alquiler", UtilidadesInterfaz.soles(a.montoAlquiler()), false),
                montoErht("Garantía (reembolsable)", UtilidadesInterfaz.soles(a.montoGarantia()), false),
                montoErht("Pagado", UtilidadesInterfaz.soles(a.pagadoAlquiler()), false),
                montoErht("Saldo", cuentaErht ? UtilidadesInterfaz.soles(a.saldo()) : "—", true));
        montosErht.setMinWidth(260);

        HBox datosErht = new HBox(30, fechasErht, cajaTrajesErht, montosErht);
        datosErht.getStyleClass().add("reserva-datos");
        cajaErht.getChildren().add(datosErht);
        cajaErht.getStyleClass().add("reserva-tarjeta");
        return cajaErht;
    }

    private static HBox lineaDeTiempoErht(String estado) {
        int actualErht = PASOS.indexOf(estado);
        boolean corteErht = "RECHAZADO".equals(estado) || "ANULADO".equals(estado);
        if (corteErht) {
            actualErht = 0;
        }
        HBox lineaErht = new HBox();
        lineaErht.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < PASOS.size(); i++) {
            String claseErht;
            if (corteErht && i == 0) {
                claseErht = "paso-cortado";
            } else if (corteErht) {
                claseErht = "paso-pendiente";
            } else if (i < actualErht) {
                claseErht = "paso-hecho";
            } else if (i == actualErht) {
                claseErht = "paso-actual";
            } else {
                claseErht = "paso-pendiente";
            }
            Circle c = new Circle(14);
            c.getStyleClass().addAll("paso-circulo", claseErht);
            StackPane puntoErht = new StackPane(c);
            if ("paso-hecho".equals(claseErht)) {
                puntoErht.getChildren().add(UtilidadesInterfaz.icono(Feather.CHECK, 14));
            } else if ("paso-cortado".equals(claseErht)) {
                puntoErht.getChildren().add(UtilidadesInterfaz.icono(Feather.X, 14));
            } else {
                Label n = new Label(String.valueOf(i + 1));
                n.getStyleClass().add("paso-numero");
                puntoErht.getChildren().add(n);
            }
            puntoErht.getStyleClass().add(claseErht);
            String textoErht = corteErht && i == 0 ? ("RECHAZADO".equals(estado) ? "Rechazado" : "Cancelado") : NOMBRES.get(i);
            Label nombreErht = new Label(textoErht);
            nombreErht.getStyleClass().addAll("paso-nombre", claseErht);
            VBox pasoErht = new VBox(6, puntoErht, nombreErht);
            pasoErht.setAlignment(Pos.CENTER);
            pasoErht.setMinWidth(90);
            lineaErht.getChildren().add(pasoErht);
            if (i < PASOS.size() - 1) {
                Region r = new Region();
                r.getStyleClass().addAll("paso-linea", !corteErht && i < actualErht ? "paso-hecho" : "paso-pendiente");
                HBox.setHgrow(r, Priority.ALWAYS);
                HBox.setMargin(r, new Insets(0, -20, 22, -20));
                lineaErht.getChildren().add(r);
            }
        }
        return lineaErht;
    }

    private static VBox datoErht(String titulo, String valor, String nota) {
        Label t = new Label(UtilidadesInterfaz.espaciado(titulo));
        t.getStyleClass().add("campo-titulo");
        Label v = new Label(valor);
        v.getStyleClass().add("reserva-valor");
        Label n = new Label(nota);
        n.getStyleClass().add("nota");
        VBox c = new VBox(6, t, v, n);
        c.setMinWidth(230);
        return c;
    }

    private static HBox montoErht(String concepto, String valor, boolean fuerte) {
        Label c = new Label(concepto);
        c.getStyleClass().add("resumen-concepto");
        Label v = new Label(valor);
        v.getStyleClass().add(fuerte ? "resumen-total-chico" : "resumen-monto");
        HBox h = new HBox(10, c, UtilidadesInterfaz.espacio(), v);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
