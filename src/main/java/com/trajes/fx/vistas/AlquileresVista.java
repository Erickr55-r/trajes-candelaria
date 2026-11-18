package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.AlquilerResumen;
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

public class AlquileresVista extends VBox {

    private final Servicios s;
    private final ComboBox<String> estado = Ui.comboTexto(
            List.of("SOLICITADO", "RESERVADO", "ENTREGADO", "DEVUELTO", "ANULADO", "RECHAZADO"), "Todos");
    private final TextField texto = Ui.campo("", "Cliente o conjunto...");
    private final Label cantidad = Ui.subtitulo("");
    private final TableView<AlquilerResumen> tabla = Ui.tabla("No hay alquileres con esos filtros.");

    public AlquileresVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        estado.setMaxWidth(170);
        texto.setPrefWidth(280);
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
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(Ui.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tabla.getColumns().add(Ui.colTexto("Cliente", AlquilerResumen::cliente, 220));
        tabla.getColumns().add(Ui.colTexto("Conjunto", AlquilerResumen::conjunto, 170));
        tabla.getColumns().add(Ui.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tabla.getColumns().add(Ui.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tabla.getColumns().add(Ui.colFecha("Devuelto", AlquilerResumen::fechaDevolucionReal, 95));
        tabla.getColumns().add(Ui.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tabla.getColumns().add(Ui.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tabla.getColumns().add(Ui.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tabla.getColumns().add(Ui.colEstadoAlquiler(170));
        Ui.alDobleClic(tabla, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            Ui.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(Ui.encabezado("Gestión de alquileres", "Alquileres",
                "Doble clic en un alquiler para ver su detalle, registrar pagos, entregarlo o recibirlo."));
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<AlquilerResumen> lista = s.alquileres().listar(estado.getValue(), texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " alquiler(es)");
    }
}
