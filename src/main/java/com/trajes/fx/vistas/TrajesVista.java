package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.TrajeFila;
import org.kordamp.ikonli.feather.Feather;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Inventario de trajes con filtros. */
public class TrajesVista extends VBox {

    private final Servicios s;
    private final ComboBox<Opcion> danza;
    private final ComboBox<String> estado = Ui.comboTexto(List.of("DISPONIBLE", "MANTENIMIENTO", "BAJA"), "Todos");
    private final ComboBox<String> talla = Ui.comboTexto(Ui.TALLAS, "Todas");
    private final TextField texto = Ui.campo("", "Código, personaje o color...");
    private final Label cantidad = Ui.subtitulo("");
    private final TableView<TrajeFila> tabla = Ui.tabla("No hay trajes con esos filtros.");

    public TrajesVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        danza = Ui.combo(s.trajes().danzas(), Opcion::nombre, "Todas");
        danza.setPrefWidth(220);
        estado.setPrefWidth(150);
        talla.setPrefWidth(90);
        texto.setPrefWidth(220);
        danza.setOnAction(e -> Ui.ejecutar(this::cargar));
        estado.setOnAction(e -> Ui.ejecutar(this::cargar));
        talla.setOnAction(e -> Ui.ejecutar(this::cargar));
        texto.setOnAction(e -> Ui.ejecutar(this::cargar));
        Button buscar = Ui.estilo(new Button("Buscar", Ui.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> Ui.ejecutar(this::cargar));

        HBox filtros = new HBox(10, Ui.etiqueta("Danza"), danza, Ui.etiqueta("Estado"), estado,
                Ui.etiqueta("Talla"), talla, texto, buscar, Ui.espacio(), cantidad);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(Ui.colTexto("Código", TrajeFila::codigo, 85));
        tabla.getColumns().add(Ui.colTexto("Danza", TrajeFila::danza, 170));
        tabla.getColumns().add(Ui.colTexto("Personaje", TrajeFila::personaje, 180));
        tabla.getColumns().add(Ui.colTexto("Género", TrajeFila::genero, 75));
        tabla.getColumns().add(Ui.colTexto("Talla", TrajeFila::talla, 55));
        tabla.getColumns().add(Ui.colTexto("Color", TrajeFila::color, 120));
        tabla.getColumns().add(Ui.colSoles("Precio", TrajeFila::precioAlquiler, 95));
        tabla.getColumns().add(Ui.colSoles("Garantía", TrajeFila::garantia, 95));
        tabla.getColumns().add(Ui.colNumero("Alquileres", TrajeFila::vecesAlquilado, 80));
        tabla.getColumns().add(Ui.colEstado("Estado", TrajeFila::estado, 130));
        Ui.alDobleClic(tabla, t -> verDetalle(t.idTraje()));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = Ui.estilo(new Button("Nuevo traje", Ui.icono(Feather.PLUS)), "boton-primario");
        nuevo.setOnAction(e -> {
            Integer id = new TrajeFormularioDialogo(s, null).mostrar();
            Ui.ejecutar(this::cargar);
            if (id != null) {
                verDetalle(id);
            }
        });
        Button editar = new Button("Editar", Ui.icono(Feather.EDIT_2));
        editar.setOnAction(e -> conSeleccion(t -> {
            new TrajeFormularioDialogo(s, t.idTraje()).mostrar();
            Ui.ejecutar(this::cargar);
        }));
        Button detalle = new Button("Ver detalle", Ui.icono(Feather.EYE));
        detalle.setOnAction(e -> conSeleccion(t -> verDetalle(t.idTraje())));

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(10, Ui.encabezado("Inventario", "Trajes",
                "Doble clic en un traje para ver sus piezas, mantenimiento e historial."),
                Ui.espacio(), editar, detalle, nuevo);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        Opcion d = danza.getValue();
        List<TrajeFila> lista = s.trajes().listar(d == null ? null : d.id(), estado.getValue(), talla.getValue(),
                texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " traje(s)");
    }

    private void verDetalle(int idTraje) {
        new TrajeDetalleDialogo(s, idTraje).mostrar();
        Ui.ejecutar(this::cargar);
    }

    private void conSeleccion(java.util.function.Consumer<TrajeFila> accion) {
        TrajeFila t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            Ui.info("Selecciona un traje de la tabla.");
            return;
        }
        accion.accept(t);
    }
}
