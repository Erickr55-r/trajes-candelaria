package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.PedidoResumen;
import com.trajes.servicio.PersonalizadorService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

public class PedidosVista extends VBox {

    private final Servicios s;
     
    private final Runnable alCambiar;
    private final ComboBox<String> estado = Ui.comboTexto(PersonalizadorService.ESTADOS, "Todos");
    private final TextField texto = Ui.campo("", "Cliente, danza o N°...");
    private final Label cantidad = Ui.subtitulo("");
    private final TableView<PedidoResumen> tabla = Ui.tabla("No hay pedidos de diseño con esos filtros.");

    public PedidosVista(Servicios s, Runnable alCambiar) {
        this.s = s;
        this.alCambiar = alCambiar;
        getStyleClass().add("contenido");

        estado.setMaxWidth(200);
        texto.setPrefWidth(260);
        texto.setOnAction(e -> Ui.ejecutar(this::cargar));
        estado.setOnAction(e -> Ui.ejecutar(this::cargar));
        Button buscar = Ui.estilo(new Button("Buscar", Ui.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> Ui.ejecutar(this::cargar));
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> {
            texto.clear();
            estado.getSelectionModel().selectFirst();
            Ui.ejecutar(this::cargar);
        });
        HBox filtros = new HBox(10, Ui.etiqueta("Estado"), estado, Ui.etiqueta("Buscar"), texto, buscar, limpiar,
                Ui.espacio(), cantidad);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(Ui.colNumero("N°", PedidoResumen::idPedido, 55));
        tabla.getColumns().add(Ui.colTexto("Cliente", PedidoResumen::cliente, 200));
        tabla.getColumns().add(Ui.colFecha("Fecha", PedidoResumen::fechaPedido, 100));
        tabla.getColumns().add(Ui.colTexto("Danza", PedidoResumen::danza, 110));
        tabla.getColumns().add(Ui.colTexto("Modalidad",
                p -> "ALQUILER".equals(p.tipo()) ? "Alquilar (30%)" : "Confeccionar", 130));
        tabla.getColumns().add(Ui.colEstado("Estado", PedidoResumen::estado, 140));
        tabla.getColumns().add(Ui.colSoles("Estimado",
                p -> p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado(), 100));
        tabla.getColumns().add(Ui.colTexto("Precio final",
                p -> "PENDIENTE".equals(p.estado()) ? "Por cotizar" : Ui.soles(p.montoTotal()), 110));
        tabla.getColumns().add(Ui.colSoles("Saldo", PedidoResumen::saldo, 95));
        TableColumn<PedidoResumen, PedidoResumen> accion = new TableColumn<>(Ui.cabeceraColumna("Acción"));
        accion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        accion.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoResumen p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                setGraphic(empty || p == null ? null : AccionPedido.crear(s, p, () -> Ui.ejecutar(PedidosVista.this::cargar)));
            }
        });
        accion.setSortable(false);
        accion.setPrefWidth(180);
        accion.setMinWidth(170);
        tabla.getColumns().add(accion);
        Ui.alDobleClic(tabla, p -> {
            new PedidoDisenoDialogo(s, p.idPedido()).mostrar();
            Ui.ejecutar(this::cargar);
        });
        tabla.setMinHeight(380);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        HBox cabecera = new HBox(Ui.encabezado("Confección a medida", "Pedidos de diseño",
                "Diseños que envían los clientes desde su portal. Cotiza los pendientes y avanza cada pedido con la "
                        + "columna Acción. Doble clic para ver el diseño, sus colores y los pagos."));
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<PedidoResumen> lista = s.personalizador().listarPedidos(estado.getValue(), texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " pedido(s)");
        alCambiar.run();
    }
}
