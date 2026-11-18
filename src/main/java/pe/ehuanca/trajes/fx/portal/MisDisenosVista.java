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

    private final Servicios sErht;
    private final VistaPortalCliente portalErht;

    public MisDisenosVista(Servicios s, VistaPortalCliente portal) {
        this.sErht = s;
        this.portalErht = portal;
        getStyleClass().add("contenido");
        int idClienteErht = Sesion.actual().idCliente();

        Button nuevoErht = UtilidadesInterfaz.estilo(new Button("Diseñar un traje", UtilidadesInterfaz.icono(Feather.SCISSORS)), "boton-primario");
        nuevoErht.setOnAction(e -> portal.ir(VistaPortalCliente.DISENIO));
        HBox cabeceraErht = new HBox(10, UtilidadesInterfaz.encabezado("Confección a medida", "Mis diseños",
                "Cuando la tienda cotiza tu diseño, apruébalo para que empiece la confección o recházalo."),
                UtilidadesInterfaz.espacio(), nuevoErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().add(cabeceraErht);

        List<PedidoResumen> listaErht = s.personalizador().pedidosDelCliente(idClienteErht);
        if (listaErht.isEmpty()) {
            VBox vacioErht = new VBox(10, UtilidadesInterfaz.seccion("Aún no enviaste diseños"),
                    UtilidadesInterfaz.subtitulo("En «Diseña tu traje» elige colores e implementos y envíalo a la tienda."));
            vacioErht.getStyleClass().addAll("tarjeta-blanca", "vacio-catalogo");
            getChildren().add(vacioErht);
            return;
        }
        TarjetaTraje.Cuadricula cuadriculaErht = new TarjetaTraje.Cuadricula();
        List<Node> tarjetasErht = new ArrayList<>();
        for (PedidoResumen p : listaErht) {
            tarjetasErht.add(tarjetaErht(p, idClienteErht));
        }
        cuadriculaErht.mostrar(tarjetasErht);
        getChildren().add(cuadriculaErht);
    }

    private VBox tarjetaErht(PedidoResumen p, int idCliente) {
        StackPane imagenErht = new StackPane();
        imagenErht.setMinHeight(ALTO_IMAGEN);
        imagenErht.setPrefHeight(ALTO_IMAGEN);
        imagenErht.setMaxHeight(ALTO_IMAGEN);
        imagenErht.getStyleClass().add("diseno-imagen");
        byte[] pngErht = sErht.personalizador().imagenDiseno(p.idPedido(), idCliente);
        if (pngErht != null && pngErht.length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(pngErht)));
            iv.setPreserveRatio(true);
            iv.setFitHeight(ALTO_IMAGEN - 10);
            imagenErht.getChildren().add(iv);
        } else {
            String colorErht = Adornos.colorDanza(p.danza());
            imagenErht.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + colorErht + ", 18%), derive("
                    + colorErht + ", -42%));");
            Group chakanaErht = Adornos.chakanaEnGrupo(200, "chakana-tenue");
            Label d = new Label(p.danza());
            d.getStyleClass().add("marcador-danza");
            imagenErht.getChildren().addAll(chakanaErht, d);
        }
        Label estadoErht = UtilidadesInterfaz.insigniaEstado(p.estado());
        StackPane.setAlignment(estadoErht, Pos.TOP_RIGHT);
        StackPane.setMargin(estadoErht, new Insets(14));
        imagenErht.getChildren().add(estadoErht);
        Rectangle arribaErht = new Rectangle();
        arribaErht.setArcWidth(36);
        arribaErht.setArcHeight(36);
        arribaErht.widthProperty().bind(imagenErht.widthProperty());
        arribaErht.setHeight(ALTO_IMAGEN);
        Rectangle rectoErht = new Rectangle(0, ALTO_IMAGEN - 20, 0, 20);
        rectoErht.widthProperty().bind(imagenErht.widthProperty());
        imagenErht.setClip(new Group(arribaErht, rectoErht));

        Label metaErht = new Label(UtilidadesInterfaz.espaciado(p.danza()) + "  •  " + UtilidadesInterfaz.espaciado(p.tipo()));
        metaErht.getStyleClass().add("traje-datos");
        Label tituloErht = new Label("Diseño N° " + p.idPedido());
        tituloErht.getStyleClass().add("traje-nombre");
        Label datosErht = new Label("Talla " + p.talla() + " · " + p.numPartes() + " partes · " + p.numImplementos()
                + " implementos · entrega " + UtilidadesInterfaz.fecha(p.fechaEntrega()));
        datosErht.getStyleClass().add("traje-desc");
        datosErht.setWrapText(true);
        datosErht.setMinHeight(Region.USE_PREF_SIZE);

        Region sepErht = new Region();
        sepErht.getStyleClass().add("traje-separador");
        BigDecimal estimadoErht = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizadoErht = !List.of("PENDIENTE", "CANCELADO").contains(p.estado());
        Label lEstimadoErht = new Label("Precio estimado  " + UtilidadesInterfaz.soles(estimadoErht));
        lEstimadoErht.getStyleClass().add(cotizadoErht ? "precio-tachable" : "traje-precio-nota");
        Label precioErht = new Label(cotizadoErht ? UtilidadesInterfaz.soles(p.montoTotal()) : "PENDIENTE".equals(p.estado()) ? "Por cotizar" : "—");
        precioErht.getStyleClass().add("traje-precio");
        Label notaErht = new Label(cotizadoErht ? "Precio final de la tienda"
                : "CANCELADO".equals(p.estado()) ? "Pedido cancelado" : "La tienda está revisando tu diseño");
        notaErht.getStyleClass().add("traje-precio-nota");
        VBox cajaPrecioErht = new VBox(2, lEstimadoErht, precioErht, notaErht);

        VBox cuerpoErht = new VBox(8, metaErht, tituloErht, datosErht, sepErht, cajaPrecioErht);
        if ("COTIZADO".equals(p.estado())) {
            Button aprobarErht = UtilidadesInterfaz.estilo(new Button("Aprobar", UtilidadesInterfaz.icono(Feather.CHECK)), "boton-primario");
            Button rechazarErht = UtilidadesInterfaz.estilo(new Button("Rechazar"), "boton-peligro");
            aprobarErht.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿Aprobar el diseño N° " + p.idPedido() + " por " + UtilidadesInterfaz.soles(p.montoTotal())
                        + "? La tienda empezará la confección.")) {
                    UtilidadesInterfaz.ejecutar(() -> {
                        sErht.personalizador().aprobarCotizacion(p.idPedido(), idCliente);
                        portalErht.confirmar("¡Diseño aprobado!", "La tienda empezará a confeccionar tu traje N° "
                                        + p.idPedido() + ". Acércate a dejar tu adelanto.",
                                new AnimacionConfirmacion.Opcion("Entendido", true,
                                        () -> portalErht.ir(VistaPortalCliente.MIS_DISENOS)));
                    });
                }
            });
            rechazarErht.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿Rechazar la cotización del diseño N° " + p.idPedido() + "? El pedido quedará cancelado.")) {
                    UtilidadesInterfaz.ejecutar(() -> {
                        sErht.personalizador().rechazarCotizacion(p.idPedido(), idCliente);
                        portalErht.ir(VistaPortalCliente.MIS_DISENOS);
                    });
                }
            });
            aprobarErht.setMaxWidth(Double.MAX_VALUE);
            rechazarErht.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(aprobarErht, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(rechazarErht, javafx.scene.layout.Priority.ALWAYS);
            cuerpoErht.getChildren().add(new HBox(10, aprobarErht, rechazarErht));
        }
        cuerpoErht.getStyleClass().add("traje-cuerpo");

        VBox tarjetaErht = new VBox(imagenErht, cuerpoErht);
        tarjetaErht.getStyleClass().add("traje-tarjeta");
        tarjetaErht.setMinWidth(0);
        tarjetaErht.setMaxWidth(Double.MAX_VALUE);
        return tarjetaErht;
    }
}
