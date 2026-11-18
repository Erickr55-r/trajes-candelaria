package com.trajes.fx.portal;

import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.modelo.AlquilerDetalleTraje;
import com.trajes.modelo.AlquilerResumen;
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

public class MisReservasVista extends VBox {

    private static final List<String> PASOS = List.of("SOLICITADO", "RESERVADO", "ENTREGADO", "DEVUELTO");
    private static final List<String> NOMBRES = List.of("Solicitado", "Reservado", "Entregado", "Devuelto");

    private final Servicios s;
    private final PortalClienteView portal;

    public MisReservasVista(Servicios s, PortalClienteView portal) {
        this.s = s;
        this.portal = portal;
        getStyleClass().add("contenido");
        int idCliente = Sesion.get().getCliente();   

        List<AlquilerResumen> lista = s.alquileres().delCliente(idCliente);
        Map<Integer, String> motivos = s.alquileres().motivosRechazo(idCliente);

        getChildren().add(Ui.encabezado("Mi cuenta", "Mis reservas",
                "Sigue cada reserva paso a paso. Las solicitudes quedan en revisión hasta que la tienda las confirme."));
        if (lista.isEmpty()) {
            Button ir = Ui.estilo(new Button("Ver trajes", Ui.icono(Feather.ARROW_RIGHT)), "boton-primario");
            ir.setOnAction(e -> portal.ir(PortalClienteView.CATALOGO));
            VBox vacio = new VBox(10, Ui.seccion("Todavía no tienes reservas"),
                    Ui.subtitulo("Elige un traje del catálogo y solicita tu reserva."), ir);
            vacio.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            getChildren().add(vacio);
            return;
        }
        for (AlquilerResumen a : lista) {
            getChildren().add(tarjeta(a, motivos.get(a.idAlquiler()), idCliente));
        }
    }

    private VBox tarjeta(AlquilerResumen a, String motivo, int idCliente) {
        Label titulo = new Label("Reserva N° " + a.idAlquiler());
        titulo.getStyleClass().add("reserva-titulo");
        Label badge = Ui.badge(a.estado());
        HBox cabecera = new HBox(12, titulo, badge, Ui.espacio());
        cabecera.setAlignment(Pos.CENTER_LEFT);
        if ("SOLICITADO".equals(a.estado())) {
            Button cancelar = Ui.estilo(new Button("Cancelar solicitud", Ui.icono(Feather.X_CIRCLE)), "boton-peligro");
            cancelar.setOnAction(e -> {
                if (Ui.confirmar("¿Cancelar tu solicitud N° " + a.idAlquiler() + "? Los trajes quedarán libres.")) {
                    Ui.ejecutar(() -> {
                        s.alquileres().cancelarSolicitud(a.idAlquiler(), idCliente);
                        portal.ir(PortalClienteView.MIS_RESERVAS);
                    });
                }
            });
            cabecera.getChildren().add(cancelar);
        }

        VBox caja = new VBox(18, cabecera, lineaDeTiempo(a.estado()));

        if ("RECHAZADO".equals(a.estado())) {
            Label m = new Label("La tienda no pudo aceptar esta solicitud: " + (motivo == null ? "—" : motivo),
                    Ui.icono(Feather.INFO));
            m.getStyleClass().add("error-suave");
            m.setWrapText(true);
            m.setMaxWidth(Double.MAX_VALUE);
            caja.getChildren().add(m);
        } else if ("ANULADO".equals(a.estado())) {
            Label m = new Label("Esta reserva fue cancelada.", Ui.icono(Feather.INFO));
            m.getStyleClass().add("aviso-gris");
            caja.getChildren().add(m);
        }

        long dias = Math.max(1, ChronoUnit.DAYS.between(a.fechaSalida(), a.fechaDevolucionPrevista()));
        VBox fechas = dato("Fechas", Ui.fecha(a.fechaSalida()) + "  →  " + Ui.fecha(a.fechaDevolucionPrevista()),
                dias + (dias == 1 ? " día" : " días") + (a.conjunto() != null && !"PARTICULAR".equals(a.conjunto())
                        ? " · " + a.conjunto() : ""));
        List<AlquilerDetalleTraje> trajes = s.alquileres().trajesDelCliente(a.idAlquiler(), idCliente);
        VBox listaTrajes = new VBox(4);
        for (AlquilerDetalleTraje t : trajes) {
            Label l = new Label(t.personaje() + " · " + t.danza() + "  (talla " + t.talla() + ", " + t.codigo() + ")",
                    Ui.icono(Feather.CHECK));
            l.getStyleClass().add("pieza-item");
            listaTrajes.getChildren().add(l);
        }
        Label tTrajes = new Label(Ui.espaciado("Trajes (" + trajes.size() + ")"));
        tTrajes.getStyleClass().add("campo-titulo");
        VBox cajaTrajes = new VBox(6, tTrajes, listaTrajes);
        HBox.setHgrow(cajaTrajes, Priority.ALWAYS);

        boolean cuenta = !List.of("RECHAZADO", "ANULADO").contains(a.estado());
        VBox montos = new VBox(6,
                monto("Total alquiler", Ui.soles(a.montoAlquiler()), false),
                monto("Garantía (reembolsable)", Ui.soles(a.montoGarantia()), false),
                monto("Pagado", Ui.soles(a.pagadoAlquiler()), false),
                monto("Saldo", cuenta ? Ui.soles(a.saldo()) : "—", true));
        montos.setMinWidth(260);

        HBox datos = new HBox(30, fechas, cajaTrajes, montos);
        datos.getStyleClass().add("reserva-datos");
        caja.getChildren().add(datos);
        caja.getStyleClass().add("reserva-tarjeta");
        return caja;
    }

    private static HBox lineaDeTiempo(String estado) {
        int actual = PASOS.indexOf(estado);
        boolean corte = "RECHAZADO".equals(estado) || "ANULADO".equals(estado);
        if (corte) {
            actual = 0;   
        }
        HBox linea = new HBox();
        linea.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < PASOS.size(); i++) {
            String clase;
            if (corte && i == 0) {
                clase = "paso-cortado";
            } else if (corte) {
                clase = "paso-pendiente";
            } else if (i < actual) {
                clase = "paso-hecho";
            } else if (i == actual) {
                clase = "paso-actual";
            } else {
                clase = "paso-pendiente";
            }
            Circle c = new Circle(14);
            c.getStyleClass().addAll("paso-circulo", clase);
            StackPane punto = new StackPane(c);
            if ("paso-hecho".equals(clase)) {
                punto.getChildren().add(Ui.icono(Feather.CHECK, 14));
            } else if ("paso-cortado".equals(clase)) {
                punto.getChildren().add(Ui.icono(Feather.X, 14));
            } else {
                Label n = new Label(String.valueOf(i + 1));
                n.getStyleClass().add("paso-numero");
                punto.getChildren().add(n);
            }
            punto.getStyleClass().add(clase);
            String texto = corte && i == 0 ? ("RECHAZADO".equals(estado) ? "Rechazado" : "Cancelado") : NOMBRES.get(i);
            Label nombre = new Label(texto);
            nombre.getStyleClass().addAll("paso-nombre", clase);
            VBox paso = new VBox(6, punto, nombre);
            paso.setAlignment(Pos.CENTER);
            paso.setMinWidth(90);
            linea.getChildren().add(paso);
            if (i < PASOS.size() - 1) {
                Region r = new Region();
                r.getStyleClass().addAll("paso-linea", !corte && i < actual ? "paso-hecho" : "paso-pendiente");
                HBox.setHgrow(r, Priority.ALWAYS);
                HBox.setMargin(r, new Insets(0, -20, 22, -20));
                linea.getChildren().add(r);
            }
        }
        return linea;
    }

    private static VBox dato(String titulo, String valor, String nota) {
        Label t = new Label(Ui.espaciado(titulo));
        t.getStyleClass().add("campo-titulo");
        Label v = new Label(valor);
        v.getStyleClass().add("reserva-valor");
        Label n = new Label(nota);
        n.getStyleClass().add("nota");
        VBox c = new VBox(6, t, v, n);
        c.setMinWidth(230);
        return c;
    }

    private static HBox monto(String concepto, String valor, boolean fuerte) {
        Label c = new Label(concepto);
        c.getStyleClass().add("resumen-concepto");
        Label v = new Label(valor);
        v.getStyleClass().add(fuerte ? "resumen-total-chico" : "resumen-monto");
        HBox h = new HBox(10, c, Ui.espacio(), v);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
