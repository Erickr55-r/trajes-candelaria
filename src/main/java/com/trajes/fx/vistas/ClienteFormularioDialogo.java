package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.Cliente;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/** Formulario para registrar o editar un cliente. */
public class ClienteFormularioDialogo {

    private final Servicios s;
    private final Integer idCliente;
    private Integer guardado;

    public ClienteFormularioDialogo(Servicios s, Integer idCliente) {
        this.s = s;
        this.idCliente = idCliente;
    }

    /** Muestra el formulario; devuelve el id del cliente guardado o null si se canceló. */
    public Integer mostrar() {
        Ui.ejecutar(this::abrir);
        return guardado;
    }

    private void abrir() {
        Cliente c = idCliente == null ? null : s.clientes().buscar(idCliente);

        ComboBox<String> tipo = Ui.comboTexto(List.of("DNI", "RUC", "CE"), null);
        if (c != null) {
            tipo.setValue(c.tipoDocumento());
        }
        TextField numero = Ui.campo(c == null ? "" : c.numeroDocumento(), "DNI 8 dígitos · RUC 11 dígitos");
        TextField nombres = Ui.campo(c == null ? "" : c.nombres(), "Nombres o razón social");
        TextField apellidos = Ui.campo(c == null ? "" : c.apellidos(), "Opcional");
        TextField telefono = Ui.campo(c == null ? "" : c.telefono(), "Opcional");
        TextField email = Ui.campo(c == null ? "" : c.email(), "Opcional");
        TextField direccion = Ui.campo(c == null ? "" : c.direccion(), "Opcional");

        GridPane g = Ui.formulario();
        Ui.fila(g, 0, "Tipo de documento *", tipo);
        Ui.fila(g, 1, "Número de documento *", numero);
        Ui.fila(g, 2, "Nombres *", nombres);
        Ui.fila(g, 3, "Apellidos", apellidos);
        Ui.fila(g, 4, "Teléfono", telefono);
        Ui.fila(g, 5, "Email", email);
        Ui.fila(g, 6, "Dirección", direccion);

        BorderPane raiz = new BorderPane();
        VBox cabecera = new VBox(4, Ui.titulo(c == null ? "Nuevo cliente" : "Editar cliente"),
                Ui.subtitulo("Los campos con * son obligatorios."));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);
        VBox centro = new VBox(g);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Stage stage = Ui.ventana(c == null ? "Nuevo cliente" : "Editar cliente", raiz, 540, 480);
        Button cancelar = new Button("Cancelar");
        cancelar.setOnAction(e -> stage.close());
        Button guardar = Ui.estilo(new Button("Guardar"), "boton-primario");
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> Ui.ejecutar(() -> {
            guardado = s.clientes().guardar(idCliente, Ui.requerido(tipo, "Tipo de documento"),
                    Ui.requerido(numero, "Número de documento"), Ui.requerido(nombres, "Nombres"),
                    apellidos.getText(), telefono.getText(), email.getText(), direccion.getText());
            stage.close();
            Ui.info("Cliente guardado correctamente.");
        }));
        HBox pie = new HBox(cancelar, guardar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
        stage.showAndWait();
    }
}
