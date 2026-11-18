package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.ConjuntoFila;
import com.trajes.modelo.Opcion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ConjuntosVista extends VBox {

    private final Servicios s;
    private final TableView<ConjuntoFila> tabla = Ui.tabla("No hay conjuntos registrados.");

    public ConjuntosVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        tabla.getColumns().add(Ui.colTexto("Conjunto", ConjuntoFila::nombre, 230));
        tabla.getColumns().add(Ui.colTexto("Danza", ConjuntoFila::danza, 170));
        tabla.getColumns().add(Ui.colTexto("Delegado", ConjuntoFila::delegado, 200));
        tabla.getColumns().add(Ui.colTexto("Teléfono", ConjuntoFila::telefono, 110));
        tabla.getColumns().add(Ui.colNumero("Alquileres", ConjuntoFila::alquileres, 85));
        HBox.setHgrow(tabla, Priority.ALWAYS);

        TextField nombre = Ui.campo("", "Ej.: Diablada Bellavista");
        ComboBox<Opcion> danza = Ui.combo(s.trajes().danzas(), Opcion::nombre, null);
        ComboBox<Opcion> delegado = Ui.combo(s.clientes().opciones(), Opcion::nombre, "— Sin delegado —");
        TextField telefono = Ui.campo("", "Opcional");

        GridPane g = Ui.formulario();
        g.getColumnConstraints().get(1).setMinWidth(200);
        Ui.fila(g, 0, "Nombre *", nombre);
        Ui.fila(g, 1, "Danza *", danza);
        Ui.fila(g, 2, "Delegado", delegado);
        Ui.fila(g, 3, "Teléfono", telefono);

        Button guardar = Ui.estilo(new Button("Registrar conjunto"), "boton-primario");
        guardar.setMaxWidth(Double.MAX_VALUE);
        guardar.setOnAction(e -> {
            if (Ui.ejecutar(() -> {
                Opcion d = delegado.getValue();
                s.clientes().crearConjunto(Ui.requerido(nombre, "Nombre"), Ui.requerido(danza, "Danza").id(),
                        d == null ? null : d.id(), telefono.getText());
                cargar();
            })) {
                nombre.clear();
                telefono.clear();
                delegado.getSelectionModel().selectFirst();
                Ui.info("Conjunto registrado.");
            }
        });

        VBox formulario = Ui.panel(Ui.seccion("Nuevo conjunto"),
                Ui.subtitulo("Los conjuntos que alquilan 5 trajes o más reciben 10% de descuento."), g, guardar);
        formulario.setPrefWidth(380);
        formulario.setMinWidth(340);

        HBox cuerpo = new HBox(16, tabla, new VBox(formulario));
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        tabla.setMinHeight(380);
        getChildren().addAll(Ui.encabezado("Agrupaciones", "Conjuntos",
                "Agrupaciones de danza que alquilan trajes con un delegado."), cuerpo);
        cargar();
    }

    private void cargar() {
        tabla.getItems().setAll(s.clientes().conjuntos());
    }
}
