package com.trajes.fx.vistas;

import com.trajes.fx.Adornos;
import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.PedidoDetalle;
import com.trajes.modelo.PedidoImplemento;
import com.trajes.modelo.PedidoParte;
import com.trajes.modelo.PedidoResumen;
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

public class PedidoDisenoDialogo {

    private final Servicios s;
    private final int idPedido;
    private final BorderPane raiz = new BorderPane();
    private Stage stage;

    public PedidoDisenoDialogo(Servicios s, int idPedido) {
        this.s = s;
        this.idPedido = idPedido;
    }

    public void mostrar() {
        if (!Ui.ejecutar(this::cargar)) {
            return;
        }
        stage = Ui.ventana("Pedido de diseño N° " + idPedido, raiz, 1180, 780);
        stage.showAndWait();
    }

    private void cargar() {
        PedidoDetalle d = s.personalizador().detallePedido(idPedido);
        PedidoResumen p = d.resumen();

        Label titulo = Ui.titulo("Pedido de diseño N° " + p.idPedido());
        HBox linea = new HBox(12, titulo, Ui.badge(p.estado()), Ui.badge(p.tipo()));
        linea.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(6, linea,
                Ui.subtitulo(p.cliente() + "   ·   " + p.danza() + ", talla " + p.talla()
                        + "   ·   pedido el " + Ui.fecha(p.fechaPedido()) + ", entrega " + Ui.fecha(p.fechaEntrega())));
        if (p.observacion() != null && !p.observacion().isBlank()) {
            cabecera.getChildren().add(Ui.subtitulo("Observación: " + p.observacion()));
        }
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);

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
            sin.getStyleClass().add("placeholder-sub");
            sin.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
            imagen.getChildren().addAll(chakana, sin);
        }
        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(imagen.widthProperty());
        clip.heightProperty().bind(imagen.heightProperty());
        imagen.setClip(clip);

        VBox derecha = new VBox(16,
                Ui.seccion("Prendas y colores"), tablaPrendas(d.partes()),
                Ui.seccion("Implementos"), tablaImplementos(d.implementos()),
                precios(p), acciones(p));
        HBox.setHgrow(derecha, Priority.ALWAYS);

        HBox cuerpo = new HBox(24, imagen, derecha);
        cuerpo.setPadding(new Insets(22, 28, 22, 28));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        raiz.setCenter(scroll);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> stage.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    private static TableView<PedidoParte> tablaPrendas(List<PedidoParte> partes) {
        TableView<PedidoParte> t = Ui.tabla("Sin prendas.");
        TableColumn<PedidoParte, PedidoParte> muestra = new TableColumn<>(Ui.cabeceraColumna("Color"));
        muestra.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        muestra.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoParte p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                if (empty || p == null || p.hex() == null) {
                    setGraphic(null);
                    return;
                }
                Rectangle r = new Rectangle(34, 22, Color.web(p.hex().trim()));
                r.setArcWidth(8);
                r.setArcHeight(8);
                r.setStroke(Color.rgb(0, 0, 0, 0.15));
                setGraphic(r);
            }
        });
        muestra.setPrefWidth(70);
        muestra.setSortable(false);
        t.getColumns().add(Ui.colTexto("Prenda", PedidoParte::parte, 140));
        t.getColumns().add(muestra);
        t.getColumns().add(Ui.colTexto("Nombre del color", PedidoParte::color, 150));
        t.getColumns().add(Ui.colTexto("Hex", p -> p.hex() == null ? "—" : p.hex().trim().toUpperCase(), 100));
        t.getColumns().add(Ui.colSoles("Precio", PedidoParte::precioBase, 100));
        t.getItems().setAll(partes);
        t.setPrefHeight(Math.min(320, 70 + partes.size() * 52.0));
        return t;
    }

    private static TableView<PedidoImplemento> tablaImplementos(List<PedidoImplemento> lista) {
        TableView<PedidoImplemento> t = Ui.tabla("Sin implementos.");
        t.getColumns().add(Ui.colTexto("Implemento", PedidoImplemento::implemento, 180));
        t.getColumns().add(Ui.colTexto("Va en", PedidoImplemento::parteDestino, 110));
        t.getColumns().add(Ui.colNumero("Cantidad", PedidoImplemento::cantidad, 90));
        t.getColumns().add(Ui.colSoles("Subtotal", PedidoImplemento::subtotal, 100));
        t.getItems().setAll(lista);
        t.setPrefHeight(Math.min(260, 70 + Math.max(1, lista.size()) * 52.0));
        return t;
    }

    private static HBox precios(PedidoResumen p) {
        BigDecimal estimado = p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado();
        boolean cotizado = !"PENDIENTE".equals(p.estado());
        HBox h = new HBox(12,
                Ui.tarjeta("Precio estimado", Ui.soles(estimado), "gris"),
                Ui.tarjeta(cotizado ? "Precio final" : "Precio final (por cotizar)", cotizado ? Ui.soles(p.montoTotal()) : "—", "granate"),
                Ui.tarjeta("Pagado", Ui.soles(p.adelanto()), "verde"),
                Ui.tarjeta("Saldo", Ui.soles(p.saldo()), p.saldo().signum() > 0 ? "naranja" : "verde"));
        return h;
    }

    private VBox acciones(PedidoResumen p) {
        HBox fila1 = new HBox(10, AccionPedido.crear(s, p, () -> Ui.ejecutar(this::cargar)));
        fila1.setAlignment(Pos.CENTER_LEFT);
        if (!List.of("ENTREGADO", "CANCELADO").contains(p.estado())) {
            Button cancelar = Ui.estilo(new Button("Cancelar pedido", Ui.icono(Feather.X_CIRCLE)), "boton-peligro");
            cancelar.setOnAction(e -> {
                if (Ui.confirmar("¿CANCELAR el pedido N° " + idPedido + "? Esta acción no se puede deshacer.")
                        && Ui.ejecutar(() -> s.personalizador().cambiarEstado(idPedido, "CANCELADO"))) {
                    Ui.ejecutar(this::cargar);
                }
            });
            fila1.getChildren().addAll(Ui.espacio(), cancelar);
        }
        VBox caja = new VBox(12, fila1);
        if (!List.of("ENTREGADO", "CANCELADO", "PENDIENTE").contains(p.estado()) && p.saldo().signum() > 0) {
            TextField monto = Ui.campo(Ui.montoEditable(p.saldo()), "Monto S/");
            monto.setPrefWidth(130);
            Button pagar = Ui.estilo(new Button("Registrar pago", Ui.icono(Feather.CREDIT_CARD)), "boton-secundario");
            pagar.setOnAction(e -> {
                if (Ui.ejecutar(() -> s.personalizador().registrarAdelanto(idPedido, Ui.monto(monto.getText())))) {
                    Ui.ejecutar(this::cargar);
                }
            });
            HBox fila2 = new HBox(10, Ui.etiqueta("Pago a cuenta"), monto, pagar);
            fila2.setAlignment(Pos.CENTER_LEFT);
            caja.getChildren().add(fila2);
        }
        caja.getChildren().add(0, Ui.subtitulo("Flujo: PENDIENTE → COTIZADO → (el cliente aprueba) → APROBADO → "
                + "EN CONFECCIÓN → LISTO → ENTREGADO."));
        caja.getStyleClass().add("panel");
        return caja;
    }
}
