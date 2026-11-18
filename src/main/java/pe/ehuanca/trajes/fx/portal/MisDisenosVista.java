/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import pe.ehuanca.trajes.fx.Adornos;
import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.fx.vistas.TarjetaTraje;
import pe.ehuanca.trajes.modelo.PedidoResumen;
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

/**
 * Mis diseños: los pedidos personalizados del cliente, con su imagen, estado y precio cotizado.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class MisDisenosVista extends VBox {

    private static final double ALTO_IMAGEN = 230;

    private final Servicios s;
    private final VistaPortalCliente portal;

    public MisDisenosVista(Servicios s, VistaPortalCliente portal) {
        this.s = s;
        this.portal = portal;
        getStyleClass().add("contenido");
        int idCliente = Sesion.actual().idCliente();   // solo los diseños de este cliente

        Button nuevo = UtilidadesInterfaz.estilo(new Button("Diseñar un traje", UtilidadesInterfaz.icono(Feather.SCISSORS)), "boton-primario");
        nuevo.setOnAction(e -> portal.ir(VistaPortalCliente.DISENIO));
        HBox cabecera = new HBox(10, UtilidadesInterfaz.encabezado("Confección a medida", "Mis diseños",
                "Cuando la tienda cotiza tu diseño, apruébalo para que empiece la confección o recházalo."),
                UtilidadesInterfaz.espacio(), nuevo);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().add(cabecera);

        List<PedidoResumen> lista = s.personalizador().pedidosDelCliente(idCliente);
        if (lista.isEmpty()) {
            VBox vacio = new VBox(10, UtilidadesInterfaz.seccion("Aún no enviaste diseños"),
                    UtilidadesInterfaz.subtitulo("En «Diseña tu traje» elige colores e implementos y envíalo a la tienda."));
            vacio.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            getChildren().add(vacio);
            return;
        }
        TarjetaTraje.Cuadricula cuadricula = new TarjetaTraje.Cuadricula();
        List<Node> tarjetas = new ArrayList<>();
        for (PedidoResumen p : lista) {
            tarjetas.add(tarjeta(p, idCliente));
        }
        cuadricula.mostrar(tarjetas);
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
            d.getStyleClass().add("marcador-danza");
            imagen.getChildren().addAll(chakana, d);
        }
        Label estado = UtilidadesInterfaz.insigniaEstado(p.estado());
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

        Label meta = new Label(UtilidadesInterfaz.espaciado(p.danza()) + "  •  " + UtilidadesInterfaz.espaciado(p.tipo()));
        meta.getStyleClass().add("traje-datos");
        Label titulo = new Label("Diseño N° " + p.idPedido());
        titulo.getStyleClass().add("traje-nombre");
        Label datos = new Label("Talla " + p.talla() + " · " + p.numPartes() + " partes · " + p.numImplementos()
                + " implementos · entrega " + UtilidadesInterfaz.fecha(p.fechaEntrega()));
        datos.getStyleClass().add("traje-desc");
        datos.setWrapText(true);
        datos.setMinHeight(Region.USE_PREF_SIZE);

        Region sep = new Region();
        sep.getStyleClass().add("traje-separador");
        // Precio estimado (al enviar el diseño) y, cuando la tienda cotiza, el precio final.
        BigDecimal estimado = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizado = !List.of("PENDIENTE", "CANCELADO").contains(p.estado());
        Label lEstimado = new Label("Precio estimado  " + UtilidadesInterfaz.soles(estimado));
        lEstimado.getStyleClass().add(cotizado ? "precio-tachable" : "traje-precio-nota");
        Label precio = new Label(cotizado ? UtilidadesInterfaz.soles(p.montoTotal()) : "PENDIENTE".equals(p.estado()) ? "Por cotizar" : "—");
        precio.getStyleClass().add("traje-precio");
        Label nota = new Label(cotizado ? "Precio final de la tienda"
                : "CANCELADO".equals(p.estado()) ? "Pedido cancelado" : "La tienda está revisando tu diseño");
        nota.getStyleClass().add("traje-precio-nota");
        VBox cajaPrecio = new VBox(2, lEstimado, precio, nota);

        VBox cuerpo = new VBox(8, meta, titulo, datos, sep, cajaPrecio);
        if ("COTIZADO".equals(p.estado())) {
            Button aprobar = UtilidadesInterfaz.estilo(new Button("Aprobar", UtilidadesInterfaz.icono(Feather.CHECK)), "boton-primario");
            Button rechazar = UtilidadesInterfaz.estilo(new Button("Rechazar"), "boton-peligro");
            aprobar.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿Aprobar el diseño N° " + p.idPedido() + " por " + UtilidadesInterfaz.soles(p.montoTotal())
                        + "? La tienda empezará la confección.")) {
                    UtilidadesInterfaz.ejecutar(() -> {
                        s.personalizador().aprobarCotizacion(p.idPedido(), idCliente);
                        portal.confirmar("¡Diseño aprobado!", "La tienda empezará a confeccionar tu traje N° "
                                        + p.idPedido() + ". Acércate a dejar tu adelanto.",
                                new AnimacionConfirmacion.Opcion("Entendido", true,
                                        () -> portal.ir(VistaPortalCliente.MIS_DISENOS)));
                    });
                }
            });
            rechazar.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿Rechazar la cotización del diseño N° " + p.idPedido() + "? El pedido quedará cancelado.")) {
                    UtilidadesInterfaz.ejecutar(() -> {
                        s.personalizador().rechazarCotizacion(p.idPedido(), idCliente);
                        portal.ir(VistaPortalCliente.MIS_DISENOS);
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

        VBox tarjeta = new VBox(imagen, cuerpo);
        tarjeta.getStyleClass().add("traje-tarjeta");
        tarjeta.setMinWidth(0);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }
}
