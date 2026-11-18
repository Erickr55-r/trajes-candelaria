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

    private final Servicios sErht;
    private final int idPedidoErht;
    private final BorderPane raizErht = new BorderPane();
    private Stage ventanaDialogoErht;

    public PedidoDisenoDialogo(Servicios s, int idPedido) {
        this.sErht = s;
        this.idPedidoErht = idPedido;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargarErht)) {
            return;
        }
        ventanaDialogoErht = UtilidadesInterfaz.ventana("Pedido de diseño N° " + idPedidoErht, raizErht, 1180, 780);
        ventanaDialogoErht.showAndWait();
    }

    private void cargarErht() {
        PedidoDetalle d = sErht.personalizador().detallePedido(idPedidoErht);
        PedidoResumen p = d.resumen();

        // cabecera
        Label tituloErht = UtilidadesInterfaz.titulo("Pedido de diseño N° " + p.idPedido());
        HBox lineaErht = new HBox(12, tituloErht, UtilidadesInterfaz.insigniaEstado(p.estado()), UtilidadesInterfaz.insigniaEstado(p.tipo()));
        lineaErht.setAlignment(Pos.CENTER_LEFT);
        VBox cabeceraErht = new VBox(6, lineaErht,
                UtilidadesInterfaz.subtitulo(p.cliente() + "   ·   " + p.danza() + ", talla " + p.talla()
                        + "   ·   pedido el " + UtilidadesInterfaz.fecha(p.fechaPedido()) + ", entrega " + UtilidadesInterfaz.fecha(p.fechaEntrega())));
        if (p.observacion() != null && !p.observacion().isBlank()) {
            cabeceraErht.getChildren().add(UtilidadesInterfaz.subtitulo("Observación: " + p.observacion()));
        }
        cabeceraErht.getStyleClass().add("cabecera");
        raizErht.setTop(cabeceraErht);

        // izquierda: imagen grande
        StackPane imagenErht = new StackPane();
        imagenErht.getStyleClass().add("estudio");
        imagenErht.setPrefSize(470, 560);
        imagenErht.setMinSize(470, 400);
        byte[] pngErht = sErht.personalizador().imagenDisenoTienda(idPedidoErht);
        if (pngErht != null && pngErht.length > 0) {
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(pngErht)));
            iv.setPreserveRatio(true);
            iv.fitWidthProperty().bind(imagenErht.widthProperty().subtract(20));
            iv.fitHeightProperty().bind(imagenErht.heightProperty().subtract(20));
            imagenErht.getChildren().add(iv);
        } else {
            Group chakanaErht = Adornos.chakanaEnGrupo(260, "chakana-tenue");
            Label sinErht = new Label("Pedido registrado en la tienda\n(sin imagen del diseño)");
            sinErht.getStyleClass().add("marcador-sub");
            sinErht.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
            imagenErht.getChildren().addAll(chakanaErht, sinErht);
        }
        Rectangle recorteErht = new Rectangle();
        recorteErht.setArcWidth(32);
        recorteErht.setArcHeight(32);
        recorteErht.widthProperty().bind(imagenErht.widthProperty());
        recorteErht.heightProperty().bind(imagenErht.heightProperty());
        imagenErht.setClip(recorteErht);

        // derecha: prendas, implementos, precios y acciones
        VBox derechaErht = new VBox(16,
                UtilidadesInterfaz.seccion("Prendas y colores"), tablaPrendasErht(d.partes()),
                UtilidadesInterfaz.seccion("Implementos"), tablaImplementosErht(d.implementos()),
                preciosErht(p), accionesErht(p));
        HBox.setHgrow(derechaErht, Priority.ALWAYS);

        HBox cuerpoErht = new HBox(24, imagenErht, derechaErht);
        cuerpoErht.setPadding(new Insets(22, 28, 22, 28));
        ScrollPane desplazableErht = new ScrollPane(cuerpoErht);
        desplazableErht.setFitToWidth(true);
        raizErht.setCenter(desplazableErht);

        Button cerrarErht = new Button("Cerrar");
        cerrarErht.setOnAction(e -> ventanaDialogoErht.close());
        HBox pieErht = new HBox(cerrarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
    }

    private static TableView<PedidoParte> tablaPrendasErht(List<PedidoParte> partes) {
        TableView<PedidoParte> t = UtilidadesInterfaz.tabla("Sin prendas.");
        TableColumn<PedidoParte, PedidoParte> muestraErht = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna("Color"));
        muestraErht.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        muestraErht.setCellFactory(c -> new TableCell<>() {
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
        muestraErht.setPrefWidth(70);
        muestraErht.setSortable(false);
        t.getColumns().add(UtilidadesInterfaz.colTexto("Prenda", PedidoParte::parte, 140));
        t.getColumns().add(muestraErht);
        t.getColumns().add(UtilidadesInterfaz.colTexto("Nombre del color", PedidoParte::color, 150));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Hex", p -> p.colorHex() == null ? "—" : p.colorHex().trim().toUpperCase(), 100));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Precio", PedidoParte::precioBase, 100));
        t.getItems().setAll(partes);
        t.setPrefHeight(Math.min(320, 70 + partes.size() * 52.0));
        return t;
    }

    private static TableView<PedidoImplemento> tablaImplementosErht(List<PedidoImplemento> lista) {
        TableView<PedidoImplemento> t = UtilidadesInterfaz.tabla("Sin implementos.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Implemento", PedidoImplemento::implemento, 180));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Va en", PedidoImplemento::parteDestino, 110));
        t.getColumns().add(UtilidadesInterfaz.colNumero("Cantidad", PedidoImplemento::cantidad, 90));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Subtotal", PedidoImplemento::subtotal, 100));
        t.getItems().setAll(lista);
        t.setPrefHeight(Math.min(260, 70 + Math.max(1, lista.size()) * 52.0));
        return t;
    }

    private static HBox preciosErht(PedidoResumen p) {
        BigDecimal estimadoErht = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizadoErht = !"PENDIENTE".equals(p.estado());
        HBox h = new HBox(12,
                UtilidadesInterfaz.tarjeta("Precio estimado", UtilidadesInterfaz.soles(estimadoErht), "gris"),
                UtilidadesInterfaz.tarjeta(cotizadoErht ? "Precio final" : "Precio final (por cotizar)", cotizadoErht ? UtilidadesInterfaz.soles(p.montoTotal()) : "—", "granate"),
                UtilidadesInterfaz.tarjeta("Pagado", UtilidadesInterfaz.soles(p.adelanto()), "verde"),
                UtilidadesInterfaz.tarjeta("Saldo", UtilidadesInterfaz.soles(p.saldo()), p.saldo().signum() > 0 ? "naranja" : "verde"));
        return h;
    }

    private VBox accionesErht(PedidoResumen p) {
        HBox fila1Erht = new HBox(10, AccionPedido.crear(sErht, p, () -> UtilidadesInterfaz.ejecutar(this::cargarErht)));
        fila1Erht.setAlignment(Pos.CENTER_LEFT);
        if (!List.of("ENTREGADO", "CANCELADO").contains(p.estado())) {
            Button cancelarErht = UtilidadesInterfaz.estilo(new Button("Cancelar pedido", UtilidadesInterfaz.icono(Feather.X_CIRCLE)), "boton-peligro");
            cancelarErht.setOnAction(e -> {
                if (UtilidadesInterfaz.confirmar("¿CANCELAR el pedido N° " + idPedidoErht + "? Esta acción no se puede deshacer.")
                        && UtilidadesInterfaz.ejecutar(() -> sErht.personalizador().cambiarEstado(idPedidoErht, "CANCELADO"))) {
                    UtilidadesInterfaz.ejecutar(this::cargarErht);
                }
            });
            fila1Erht.getChildren().addAll(UtilidadesInterfaz.espacio(), cancelarErht);
        }
        VBox cajaErht = new VBox(12, fila1Erht);
        if (!List.of("ENTREGADO", "CANCELADO", "PENDIENTE").contains(p.estado()) && p.saldo().signum() > 0) {
            TextField montoErht = UtilidadesInterfaz.campo(UtilidadesInterfaz.montoEditable(p.saldo()), "Monto S/");
            montoErht.setPrefWidth(130);
            Button pagarErht = UtilidadesInterfaz.estilo(new Button("Registrar pago", UtilidadesInterfaz.icono(Feather.CREDIT_CARD)), "boton-secundario");
            pagarErht.setOnAction(e -> {
                if (UtilidadesInterfaz.ejecutar(() -> sErht.personalizador().registrarAdelanto(idPedidoErht, UtilidadesInterfaz.monto(montoErht.getText())))) {
                    UtilidadesInterfaz.ejecutar(this::cargarErht);
                }
            });
            HBox fila2Erht = new HBox(10, UtilidadesInterfaz.etiqueta("Pago a cuenta"), montoErht, pagarErht);
            fila2Erht.setAlignment(Pos.CENTER_LEFT);
            cajaErht.getChildren().add(fila2Erht);
        }
        cajaErht.getChildren().add(0, UtilidadesInterfaz.subtitulo("Flujo: PENDIENTE → COTIZADO → (el cliente aprueba) → APROBADO → "
                + "EN CONFECCIÓN → LISTO → ENTREGADO."));
        cajaErht.getStyleClass().add("panel");
        return cajaErht;
    }
}
