package com.trajes.fx.portal;

import com.trajes.fx.Adornos;
import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.fx.vistas.TarjetaTraje;
import com.trajes.modelo.PedidoResumen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MisDisenosVista extends VBox {

    private static final double ALTO_IMAGEN = 230;

    private final Servicios s;
    private final PortalClienteView portal;

    public MisDisenosVista(Servicios s, PortalClienteView portal) {
        this.s = s;
        this.portal = portal;
        getStyleClass().add("contenido");
        int idCliente = Sesion.get().getCliente();   

        Button nuevo = Ui.estilo(new Button("Diseñar un traje", Ui.icono(Feather.SCISSORS)), "boton-primario");
        nuevo.setOnAction(e -> portal.ir(PortalClienteView.DISENIO));
        HBox cabecera = new HBox(10, Ui.encabezado("Confección a medida", "Mis diseños",
                "Cuando la tienda cotiza tu diseño, apruébalo para que empiece la confección o recházalo."),
                Ui.espacio(), nuevo);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().add(cabecera);

        List<PedidoResumen> lista = s.personalizador().pedidosDelCliente(idCliente);
        if (lista.isEmpty()) {
            VBox vacio = new VBox(10, Ui.seccion("Aún no enviaste diseños"),
                    Ui.subtitulo("En «Diseña tu traje» elige colores e implementos y envíalo a la tienda."));
            vacio.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            getChildren().add(vacio);
            return;
        }
        TarjetaTraje.Cuadricula cuadricula = new TarjetaTraje.Cuadricula();
        List<Node> cards = new ArrayList<>();
        for (PedidoResumen p : lista) {
            cards.add(tarjeta(p, idCliente));
        }
        cuadricula.mostrar(cards);
        getChildren().add(cuadricula);
    }

    private VBox tarjeta(PedidoResumen p, int idCliente) {
        StackPane imagen = new StackPane();
        imagen.setMinHeight(ALTO_IMAGEN);
        imagen.setPrefHeight(ALTO_IMAGEN);
        imagen.setMaxHeight(ALTO_IMAGEN);
        imagen.getStyleClass().add("diseno-imagen");
        byte[] png = s.personalizador().imagenDiseno(p.idPedido(), idCliente);
        if (png != null && png.length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(png)));
            iv.setPreserveRatio(true);
            iv.setFitHeight(ALTO_IMAGEN - 10);
            imagen.getChildren().add(iv);
        } else {
            String color = Adornos.colorDanza(p.danza());
            imagen.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + color + ", 18%), derive("
                    + color + ", -42%));");
            Group chakana = Adornos.chakanaEnGrupo(200, "chakana-tenue");
            Label d = new Label(p.danza());
            d.getStyleClass().add("placeholder-danza");
            imagen.getChildren().addAll(chakana, d);
        }
        Label estado = Ui.badge(p.estado());
        StackPane.setAlignment(estado, Pos.TOP_RIGHT);
        StackPane.setMargin(estado, new Insets(14));
        imagen.getChildren().add(estado);
        Rectangle arriba = new Rectangle();
        arriba.setArcWidth(36);
        arriba.setArcHeight(36);
        arriba.widthProperty().bind(imagen.widthProperty());
        arriba.setHeight(ALTO_IMAGEN);
        Rectangle recto = new Rectangle(0, ALTO_IMAGEN - 20, 0, 20);
        recto.widthProperty().bind(imagen.widthProperty());
        imagen.setClip(new Group(arriba, recto));

        Label meta = new Label(Ui.espaciado(p.danza()) + "  •  " + Ui.espaciado(p.tipo()));
        meta.getStyleClass().add("traje-meta");
        Label titulo = new Label("Diseño N° " + p.idPedido());
        titulo.getStyleClass().add("traje-nombre");
        Label datos = new Label("Talla " + p.talla() + " · " + p.numPartes() + " partes · " + p.numImplementos()
                + " implementos · entrega " + Ui.fecha(p.fechaEntrega()));
        datos.getStyleClass().add("traje-desc");
        datos.setWrapText(true);
        datos.setMinHeight(Region.USE_PREF_SIZE);

        Region sep = new Region();
        sep.getStyleClass().add("traje-separador");
        
        BigDecimal estimado = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizado = !List.of("PENDIENTE", "CANCELADO").contains(p.estado());
        Label lEstimado = new Label("Precio estimado  " + Ui.soles(estimado));
        lEstimado.getStyleClass().add(cotizado ? "precio-tachable" : "traje-precio-nota");
        Label precio = new Label(cotizado ? Ui.soles(p.montoTotal()) : "PENDIENTE".equals(p.estado()) ? "Por cotizar" : "—");
        precio.getStyleClass().add("traje-precio");
        Label nota = new Label(cotizado ? "Precio final de la tienda"
                : "CANCELADO".equals(p.estado()) ? "Pedido cancelado" : "La tienda está revisando tu diseño");
        nota.getStyleClass().add("traje-precio-nota");
        VBox cajaPrecio = new VBox(2, lEstimado, precio, nota);

        VBox cuerpo = new VBox(8, meta, titulo, datos, sep, cajaPrecio);
        if ("COTIZADO".equals(p.estado())) {
            Button aprobar = Ui.estilo(new Button("Aprobar", Ui.icono(Feather.CHECK)), "boton-primario");
            Button rechazar = Ui.estilo(new Button("Rechazar"), "boton-peligro");
            aprobar.setOnAction(e -> {
                if (Ui.confirmar("¿Aprobar el diseño N° " + p.idPedido() + " por " + Ui.soles(p.montoTotal())
                        + "? La tienda empezará la confección.")) {
                    Ui.ejecutar(() -> {
                        s.personalizador().aprobarCotizacion(p.idPedido(), idCliente);
                        portal.confirmar("¡Diseño aprobado!", "La tienda empezará a confeccionar tu traje N° "
                                        + p.idPedido() + ". Acércate a dejar tu adelanto.",
                                new ConfirmacionCheck.Opcion("Entendido", true,
                                        () -> portal.ir(PortalClienteView.MIS_DISENOS)));
                    });
                }
            });
            rechazar.setOnAction(e -> {
                if (Ui.confirmar("¿Rechazar la cotización del diseño N° " + p.idPedido() + "? El pedido quedará cancelado.")) {
                    Ui.ejecutar(() -> {
                        s.personalizador().rechazarCotizacion(p.idPedido(), idCliente);
                        portal.ir(PortalClienteView.MIS_DISENOS);
                    });
                }
            });
            aprobar.setMaxWidth(Double.MAX_VALUE);
            rechazar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(aprobar, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(rechazar, javafx.scene.layout.Priority.ALWAYS);
            cuerpo.getChildren().add(new HBox(10, aprobar, rechazar));
        }
        cuerpo.getStyleClass().add("traje-cuerpo");

        VBox card = new VBox(imagen, cuerpo);
        card.getStyleClass().add("traje-card");
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
}
