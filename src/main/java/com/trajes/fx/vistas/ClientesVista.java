package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.ClienteFila;
import org.kordamp.ikonli.feather.Feather;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class ClientesVista extends VBox {

    private final Servicios s;
    private final TextField texto = Ui.campo("", "Nombre o número de documento...");
    private final Label cantidad = Ui.subtitulo("");
    private final TableView<ClienteFila> tabla = Ui.tabla("No se encontraron clientes.");

    public ClientesVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        texto.setPrefWidth(320);
        texto.setOnAction(e -> Ui.ejecutar(this::cargar));
        Button buscar = Ui.estilo(new Button("Buscar", Ui.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> Ui.ejecutar(this::cargar));
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> {
            texto.clear();
            Ui.ejecutar(this::cargar);
        });
        HBox filtros = new HBox(10, Ui.etiqueta("Buscar"), texto, buscar, limpiar, Ui.espacio(), cantidad);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(Ui.colTexto("Tipo", ClienteFila::tipoDocumento, 55));
        tabla.getColumns().add(Ui.colTexto("Documento", ClienteFila::numeroDocumento, 115));
        tabla.getColumns().add(Ui.colTexto("Nombre", ClienteFila::nombreCompleto, 280));
        tabla.getColumns().add(Ui.colTexto("Teléfono", ClienteFila::telefono, 110));
        tabla.getColumns().add(Ui.colTexto("Email", ClienteFila::email, 220));
        tabla.getColumns().add(Ui.colNumero("Alquileres", ClienteFila::alquileres, 85));
        tabla.getColumns().add(Ui.colSoles("Total gastado", ClienteFila::totalGastado, 120));
        Ui.alDobleClic(tabla, c -> verDetalle(c.idCliente()));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.estilo(new Button("Nuevo cliente", Ui.icono(Feather.USER_PLUS)), "boton-primario");
        nuevo.setOnAction(e -> {
            new ClienteFormularioDialogo(s, null).mostrar();
            Ui.ejecutar(this::cargar);
        });
        Button editar = new Button("Editar", Ui.icono(Feather.EDIT_2));
        editar.setOnAction(e -> conSeleccion(c -> {
            new ClienteFormularioDialogo(s, c.idCliente()).mostrar();
            Ui.ejecutar(this::cargar);
        }));
        Button detalle = new Button("Ver detalle", Ui.icono(Feather.EYE));
        detalle.setOnAction(e -> conSeleccion(c -> verDetalle(c.idCliente())));

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(10, Ui.encabezado("Cartera", "Clientes",
                "Doble clic en un cliente para ver sus datos y alquileres."), Ui.espacio(), editar, detalle, nuevo);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<ClienteFila> lista = s.clientes().listar(texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " cliente(s)");
    }

    private void verDetalle(int idCliente) {
        new ClienteDetalleDialogo(s, idCliente).mostrar();
        Ui.ejecutar(this::cargar);
    }

    private void conSeleccion(Consumer<ClienteFila> accion) {
        ClienteFila c = tabla.getSelectionModel().getSelectedItem();
        if (c == null) {
            Ui.info("Selecciona un cliente de la tabla.");
            return;
        }
        accion.accept(c);
    }
}
