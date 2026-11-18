package com.trajes.fx.legacy;

import com.trajes.fx.legacy.tresd.Maniqui3D;
import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.fx.vistas.AccionPedido;

import com.trajes.fx.legacy.tresd.Visor3D;
import com.trajes.modelo.PedidoDetalle;
import com.trajes.modelo.PedidoImplemento;
import com.trajes.modelo.PedidoParte;
import com.trajes.modelo.PedidoResumen;
import com.trajes.servicio.PersonalizadorService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class PedidoDetalleDialogo {

    private final Servicios s;
    private final int idPedido;
    private final BorderPane raiz = new BorderPane();
    private final Visor3D visor = new Visor3D(420, 520);
    private Stage stage;

    public PedidoDetalleDialogo(Servicios s, int idPedido) {
        this.s = s;
        this.idPedido = idPedido;
    }

    public void mostrar() {
        if (!Ui.ejecutar(this::cargar)) {
            return;
        }
        stage = Ui.ventana("Pedido personalizado N° " + idPedido, raiz, 1180, 760);
        stage.setOnHidden(e -> visor.detener());
        stage.showAndWait();
    }

    private void cargar() {
        PedidoDetalle d = s.personalizador().detallePedido(idPedido);
        PedidoResumen p = d.resumen();

        for (PedidoParte parte : d.partes()) {
            visor.maniqui().colorear(parte.parte(), Color.web(parte.hex()));
        }
        visor.maniqui().armar(p.danza(), d.partes().stream().map(PedidoParte::parte).toList());
        visor.maniqui().mostrarImplementos(d.implementos().stream()
                .map(i -> new Maniqui3D.Adorno(i.implemento(), i.parteDestino(), i.cantidad())).toList());

        raiz.setTop(cabecera(p));

        HBox tarjetas = new HBox(12,
                Ui.tarjeta("Total del pedido", Ui.soles(p.montoTotal()), "granate"),
                Ui.tarjeta("Adelanto pagado", Ui.soles(p.adelanto()), "verde"),
                Ui.tarjeta("Saldo pendiente", Ui.soles(p.saldo()), p.saldo().signum() > 0 ? "naranja" : "verde"));

        VBox derecha = new VBox(14, tarjetas);
        if (!"ENTREGADO".equals(p.estado()) && !"CANCELADO".equals(p.estado())) {
            derecha.getChildren().add(acciones(p));
        }
        derecha.getChildren().addAll(
                Ui.seccion("Partes y colores (" + d.partes().size() + ")"), tablaPartes(d),
                Ui.seccion("Implementos (" + d.implementos().size() + ")"), tablaImplementos(d));
        ScrollPane scroll = new ScrollPane(derecha);
        scroll.setFitToWidth(true);
        HBox.setHgrow(scroll, Priority.ALWAYS);

        VBox cajaVisor = Ui.panel(Ui.seccion("Diseño en 3D"), visor);
        VBox.setVgrow(visor, Priority.ALWAYS);
        cajaVisor.setPrefWidth(440);
        cajaVisor.setMinWidth(380);

        HBox cuerpo = new HBox(16, cajaVisor, scroll);
        cuerpo.getStyleClass().add("contenido");
        raiz.setCenter(cuerpo);

        Button guardar = new Button("Guardar diseño como imagen");
        guardar.setOnAction(e -> Ui.ejecutar(this::guardarImagen));
        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> stage.close());
        HBox pie = new HBox(guardar, cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    private VBox cabecera(PedidoResumen p) {
        HBox linea = new HBox(12, Ui.titulo("Pedido N° " + p.idPedido()), Ui.badge(p.estado()), Ui.badge(p.tipo()));
        linea.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(4, linea,
                Ui.subtitulo("Cliente: " + p.cliente() + "   ·   Danza: " + p.danza() + "   ·   Talla: " + p.talla()),
                Ui.subtitulo("Pedido: " + Ui.fecha(p.fechaPedido()) + "   ·   Entrega: " + Ui.fecha(p.fechaEntrega())));
        if (p.observacion() != null && !p.observacion().isBlank()) {
            caja.getChildren().add(Ui.subtitulo("Observación: " + p.observacion()));
        }
        caja.getStyleClass().add("cabecera");
        return caja;
    }

    private VBox acciones(PedidoResumen p) {
        HBox fila1 = new HBox(10);
        fila1.setAlignment(Pos.CENTER_LEFT);
        
        fila1.getChildren().add(AccionPedido.crear(s, p, () -> hacer(() -> { }, "Pedido actualizado.")));
        Button anular = Ui.estilo(new Button("Cancelar pedido"), "boton-peligro");
        anular.setOnAction(e -> {
            if (Ui.confirmar("¿Seguro que deseas CANCELAR el pedido N° " + idPedido + "? Esta acción no se puede deshacer.")) {
                hacer(() -> s.personalizador().cambiarEstado(idPedido, "CANCELADO"), "Pedido cancelado.");
            }
        });
        fila1.getChildren().addAll(Ui.espacio(), anular);

        TextField monto = Ui.campo(Ui.montoEditable(p.saldo()), "Monto S/");
        monto.setPrefWidth(120);
        Button pagar = Ui.estilo(new Button("Registrar adelanto"), "boton-dorado");
        pagar.setOnAction(e -> hacer(() -> s.personalizador().registrarAdelanto(idPedido, Ui.monto(monto.getText())),
                "Adelanto registrado."));
        HBox fila2 = new HBox(10, Ui.etiqueta("Pago a cuenta"), monto, pagar);
        fila2.setAlignment(Pos.CENTER_LEFT);

        return Ui.panel(Ui.seccion("Acciones"),
                Ui.subtitulo("Flujo: PENDIENTE → COTIZADO → APROBADO → EN_CONFECCION → LISTO → ENTREGADO. "
                        + "Para entregar, el saldo debe estar pagado."),
                fila1, fila2);
    }

    private void hacer(Ui.Accion accion, String exito) {
        if (Ui.ejecutar(() -> {
            accion.ejecutar();
            cargar();
        })) {
            Ui.info(exito);
        }
    }

    private void guardarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar diseño como imagen");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        fc.setInitialFileName("pedido_" + idPedido + ".png");
        File archivo = fc.showSaveDialog(stage);
        if (archivo != null) {
            visor.guardarPng(archivo);
            Ui.info("Diseño guardado en:\n" + archivo.getAbsolutePath());
        }
    }

    private TableView<PedidoParte> tablaPartes(PedidoDetalle d) {
        TableView<PedidoParte> t = Ui.tabla("Sin partes.");
        t.getColumns().add(Ui.colTexto("Parte", PedidoParte::parte, 160));
        TableColumn<PedidoParte, PedidoParte> color = new TableColumn<>(Ui.cabeceraColumna("Color"));
        color.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        color.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoParte item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Circle muestra = new Circle(7, Color.web(item.hex()));
                muestra.setStroke(Color.rgb(0, 0, 0, 0.3));
                HBox caja = new HBox(8, muestra, new Label(item.color()));
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });
        color.setPrefWidth(180);
        t.getColumns().add(color);
        t.getColumns().add(Ui.colSoles("Precio base", PedidoParte::precioBase, 120));
        t.getItems().setAll(d.partes());
        t.setPrefHeight(60 + 30 * Math.max(1, d.partes().size()));
        return t;
    }

    private TableView<PedidoImplemento> tablaImplementos(PedidoDetalle d) {
        TableView<PedidoImplemento> t = Ui.tabla("Sin implementos.");
        t.getColumns().add(Ui.colTexto("Implemento", PedidoImplemento::implemento, 160));
        t.getColumns().add(Ui.colTexto("Va en", PedidoImplemento::parteDestino, 100));
        t.getColumns().add(Ui.colNumero("Cantidad", PedidoImplemento::cantidad, 80));
        t.getColumns().add(Ui.colSoles("P. unit.", PedidoImplemento::precio, 100));
        t.getColumns().add(Ui.colSoles("Subtotal", PedidoImplemento::subtotal, 110));
        t.getItems().setAll(d.implementos());
        t.setPrefHeight(60 + 30 * Math.max(1, d.implementos().size()));
        return t;
    }
}
