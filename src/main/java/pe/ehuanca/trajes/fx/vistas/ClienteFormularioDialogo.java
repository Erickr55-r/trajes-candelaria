/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Cliente;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Formulario para registrar o editar un cliente.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
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
        UtilidadesInterfaz.ejecutar(this::abrir);
        return guardado;
    }

    private void abrir() {
        Cliente c = idCliente == null ? null : s.clientes().buscar(idCliente);

        ComboBox<String> tipo = UtilidadesInterfaz.comboTexto(List.of("DNI", "RUC", "CE"), null);
        if (c != null) {
            tipo.setValue(c.tipoDocumento());
        }
        TextField numero = UtilidadesInterfaz.campo(c == null ? "" : c.numeroDocumento(), "DNI 8 dígitos · RUC 11 dígitos");
        TextField nombres = UtilidadesInterfaz.campo(c == null ? "" : c.nombres(), "Nombres o razón social");
        TextField apellidos = UtilidadesInterfaz.campo(c == null ? "" : c.apellidos(), "Opcional");
        TextField telefono = UtilidadesInterfaz.campo(c == null ? "" : c.telefono(), "Opcional");
        TextField correo = UtilidadesInterfaz.campo(c == null ? "" : c.correo(), "Opcional");
        TextField direccion = UtilidadesInterfaz.campo(c == null ? "" : c.direccion(), "Opcional");

        GridPane g = UtilidadesInterfaz.formulario();
        UtilidadesInterfaz.fila(g, 0, "Tipo de documento *", tipo);
        UtilidadesInterfaz.fila(g, 1, "Número de documento *", numero);
        UtilidadesInterfaz.fila(g, 2, "Nombres *", nombres);
        UtilidadesInterfaz.fila(g, 3, "Apellidos", apellidos);
        UtilidadesInterfaz.fila(g, 4, "Teléfono", telefono);
        UtilidadesInterfaz.fila(g, 5, "Email", correo);
        UtilidadesInterfaz.fila(g, 6, "Dirección", direccion);

        BorderPane raiz = new BorderPane();
        VBox cabecera = new VBox(4, UtilidadesInterfaz.titulo(c == null ? "Nuevo cliente" : "Editar cliente"),
                UtilidadesInterfaz.subtitulo("Los campos con * son obligatorios."));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);
        VBox centro = new VBox(g);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Stage ventanaDialogo = UtilidadesInterfaz.ventana(c == null ? "Nuevo cliente" : "Editar cliente", raiz, 540, 480);
        Button cancelar = new Button("Cancelar");
        cancelar.setOnAction(e -> ventanaDialogo.close());
        Button guardar = UtilidadesInterfaz.estilo(new Button("Guardar"), "boton-primario");
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            guardado = s.clientes().guardar(idCliente, UtilidadesInterfaz.requerido(tipo, "Tipo de documento"),
                    UtilidadesInterfaz.requerido(numero, "Número de documento"), UtilidadesInterfaz.requerido(nombres, "Nombres"),
                    apellidos.getText(), telefono.getText(), correo.getText(), direccion.getText());
            ventanaDialogo.close();
            UtilidadesInterfaz.mostrarInformacion("Cliente guardado correctamente.");
        }));
        HBox pie = new HBox(cancelar, guardar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
        ventanaDialogo.showAndWait();
    }
}
