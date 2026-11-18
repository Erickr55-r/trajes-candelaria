/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Adornos;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.PedidoDetalle;
import pe.ehuanca.trajes.modelo.PedidoImplemento;
import pe.ehuanca.trajes.modelo.PedidoParte;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de un pedido de diseño para la tienda: imagen grande del diseño, prendas con su color (muestra y hex),
 * implementos, precios y la acción según el estado (Cotizar, Iniciar confección, Marcar listo, Entregar).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class PedidoDisenoDialogo {

    private final Servicios s;
    private final int idPedido;
    private final BorderPane raiz = new BorderPane();
    private Stage ventanaDialogo;

    public PedidoDisenoDialogo(Servicios s, int idPedido) {
        this.s = s;
        this.idPedido = idPedido;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargar)) {
            return;
        }
        ventanaDialogo = UtilidadesInterfaz.ventana("Pedido de diseño N° " + idPedido, raiz, 1180, 780);
        ventanaDialogo.showAndWait();
    }

    private void cargar() {
        PedidoDetalle d = s.personalizador().detallePedido(idPedido);
        PedidoResumen p = d.resumen();

        // cabecera
        Label titulo = UtilidadesInterfaz.titulo("Pedido de diseño N° " + p.idPedido());
        HBox linea = new HBox(12, titulo, UtilidadesInterfaz.insigniaEstado(p.estado()), UtilidadesInterfaz.insigniaEstado(p.tipo()));
        linea.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(6, linea,
                UtilidadesInterfaz.subtitulo(p.cliente() + "   ·   " + p.danza() + ", talla " + p.talla()
                        + "   ·   pedido el " + UtilidadesInterfaz.fecha(p.fechaPedido()) + ", entrega " + UtilidadesInterfaz.fecha(p.fechaEntrega())));
        if (p.observacion() != null && !p.observacion().isBlank()) {
            cabecera.getChildren().add(UtilidadesInterfaz.subtitulo("Observación: " + p.observacion()));
        }
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);

        // izquierda: imagen grande
        StackPane imagen = new StackPane();
        imagen.getStyleClass().add("estudio");
        imagen.setPrefSize(470, 560);
        imagen.setMinSize(470, 400);
        byte[] png = s.personalizador().imagenDisenoTienda(idPedido);
        if (png != null && png.length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(png)));
            iv.setPreserveRatio(true);
            iv.fitWidthProperty().bind(imagen.widthProperty().subtract(20));
            iv.fitHeightProperty().bind(imagen.heightProperty().subtract(20));
            imagen.getChildren().add(iv);
        } else {
            Group chakana = Adornos.chakanaEnGrupo(260, "chakana-tenue");
            Label sin = new Label("Pedido registrado en la tienda\n(sin imagen del diseño)");
            sin.getStyleClass().add("marcador-sub");
            sin.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
            imagen.getChildren().addAll(chakana, sin);
        }
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(32);
        recorte.setArcHeight(32);
        recorte.widthProperty().bind(imagen.widthProperty());
        recorte.heightProperty().bind(imagen.heightProperty());
        imagen.setClip(recorte);

        // derecha: prendas, implementos, precios y acciones
        VBox derecha = new VBox(16,
                UtilidadesInterfaz.seccion("Prendas y colores"), tablaPrendas(d.partes()),
                UtilidadesInterfaz.seccion("Implementos"), tablaImplementos(d.implementos()),
                precios(p), acciones(p));
        HBox.setHgrow(derecha, Priority.ALWAYS);

        HBox cuerpo = new HBox(24, imagen, derecha);
        cuerpo.setPadding(new Insets(22, 28, 22, 28));
        ScrollPane desplazable = new ScrollPane(cuerpo);
        desplazable.setFitToWidth(true);
        raiz.setCenter(desplazable);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventanaDialogo.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    private static TableView<PedidoParte> tablaPrendas(List<PedidoParte> partes) {
        TableView<PedidoParte> t = UtilidadesInterfaz.tabla("Sin prendas.");
        TableColumn<PedidoParte, PedidoParte> muestra = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna("Color"));
        muestra.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        muestra.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoParte p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                if (empty || p == null || p.colorHex() == null) {
                    setGraphic(null);
                    return;
                }
                Rectangle r = new Rectangle(34, 22, Color.web(p.colorHex().trim()));
                r.setArcWidth(8);
                r.setArcHeight(8);
                r.setStroke(Color.rgb(0, 0, 0, 0.15));
                setGraphic(r);
            }
        });
        muestra.setPrefWidth(70);
        muestra.setSortable(false);
        t.getColumns().add(UtilidadesInterfaz.colTexto("Prenda", PedidoParte::parte, 140));
        t.getColumns().add(muestra);
        t.getColumns().add(UtilidadesInterfaz.colTexto("Nombre del color", PedidoParte::color, 150));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Hex", p -> p.colorHex() == null ? "—" : p.colorHex().trim().toUpperCase(), 100));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Precio", PedidoParte::precioBase, 100));
        t.getItems().setAll(partes);
        t.setPrefHeight(Math.min(320, 70 + partes.size() * 52.0));
        return t;
    }

    private static TableView<PedidoImplemento> tablaImplementos(List<PedidoImplemento> lista) {
        TableView<PedidoImplemento> t = UtilidadesInterfaz.tabla("Sin implementos.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Implemento", PedidoImplemento::implemento, 180));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Va en", PedidoImplemento::parteDestino, 110));
        t.getColumns().add(UtilidadesInterfaz.colNumero("Cantidad", PedidoImplemento::cantidad, 90));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Subtotal", PedidoImplemento::subtotal, 100));
        t.getItems().setAll(lista);
        t.setPrefHeight(Math.min(260, 70 + Math.max(1, lista.size()) * 52.0));
        return t;
    }

    private static HBox precios(PedidoResumen p) {
        BigDecimal estimado = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizado = !"PENDIENTE".equals(p.estado());
        HBox h = new HBox(12,
                UtilidadesInterfaz.tarjeta("Precio estimado", UtilidadesInterfaz.soles(estimado), "gris"),
                UtilidadesInterfaz.tarjeta(cotizado ? "Precio final" : "Precio final (por cotizar)", cotizado ? UtilidadesInterfaz.soles(p.montoTotal()) : "—", "granate"),
                UtilidadesInterfaz.tarjeta("Pagado", UtilidadesInterfaz.soles(p.adelanto()), "verde"),
                UtilidadesInterfaz.tarjeta("Saldo", UtilidadesInterfaz.soles(p.saldo()), p.saldo().signum() > 0 ? "naranja" : "verde"));
        return h;
    }

    private VBox acciones(PedidoResumen p) {
        HBox fila1 = new HBox(10, AccionPedido.crear(s, p, () -> UtilidadesInterfaz.ejecutar(this::cargar)));
        fila1.setAlignment(Pos.CENTER_LEFT);
        if (!List.of("ENTREGADO", "CANCELADO").contains(p.estado())) {
            Button cancelar = UtilidadesInterfaz.estilo(new Button("Cancelar pedido", UtilidadesInterfaz.icono(Feather.X_CIRCLE)), "boton-peligro");
            cancelar.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿CANCELAR el pedido N° " + idPedido + "? Esta acción no se puede deshacer.")
                        && UtilidadesInterfaz.ejecutar(() -> s.personalizador().cambiarEstado(idPedido, "CANCELADO"))) {
                    UtilidadesInterfaz.ejecutar(this::cargar);
                }
            });
            fila1.getChildren().addAll(UtilidadesInterfaz.espacio(), cancelar);
        }
        VBox caja = new VBox(12, fila1);
        if (!List.of("ENTREGADO", "CANCELADO", "PENDIENTE").contains(p.estado()) && p.saldo().signum() > 0) {
            TextField monto = UtilidadesInterfaz.campo(UtilidadesInterfaz.montoEditable(p.saldo()), "Monto S/");
            monto.setPrefWidth(130);
            Button pagar = UtilidadesInterfaz.estilo(new Button("Registrar pago", UtilidadesInterfaz.icono(Feather.CREDIT_CARD)), "boton-secundario");
            pagar.setOnAction(e -> {
                if (UtilidadesInterfaz.ejecutar(() -> s.personalizador().registrarAdelanto(idPedido, UtilidadesInterfaz.monto(monto.getText())))) {
                    UtilidadesInterfaz.ejecutar(this::cargar);
                }
            });
            HBox fila2 = new HBox(10, UtilidadesInterfaz.etiqueta("Pago a cuenta"), monto, pagar);
            fila2.setAlignment(Pos.CENTER_LEFT);
            caja.getChildren().add(fila2);
        }
        caja.getChildren().add(0, UtilidadesInterfaz.subtitulo("Flujo: PENDIENTE → COTIZADO → (el cliente aprueba) → APROBADO → "
                + "EN CONFECCIÓN → LISTO → ENTREGADO."));
        caja.getStyleClass().add("panel");
        return caja;
    }
}
