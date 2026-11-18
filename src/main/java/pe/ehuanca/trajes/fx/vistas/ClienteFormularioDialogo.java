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

    private final Servicios sErht;
    private final Integer idClienteErht;
    private Integer guardadoErht;

    public ClienteFormularioDialogo(Servicios s, Integer idCliente) {
        this.sErht = s;
        this.idClienteErht = idCliente;
    }

    /** Muestra el formulario; devuelve el id del cliente guardado o null si se canceló. */
    public Integer mostrar() {
        UtilidadesInterfaz.ejecutar(this::abrirErht);
        return guardadoErht;
    }

    private void abrirErht() {
        Cliente c = idClienteErht == null ? null : sErht.clientes().buscar(idClienteErht);

        ComboBox<String> tipoErht = UtilidadesInterfaz.comboTexto(List.of("DNI", "RUC", "CE"), null);
        if (c != null) {
            tipoErht.setValue(c.tipoDocumento());
        }
        TextField numeroErht = UtilidadesInterfaz.campo(c == null ? "" : c.numeroDocumento(), "DNI 8 dígitos · RUC 11 dígitos");
        TextField nombresErht = UtilidadesInterfaz.campo(c == null ? "" : c.nombres(), "Nombres o razón social");
        TextField apellidosErht = UtilidadesInterfaz.campo(c == null ? "" : c.apellidos(), "Opcional");
        TextField telefonoErht = UtilidadesInterfaz.campo(c == null ? "" : c.telefono(), "Opcional");
        TextField correoErht = UtilidadesInterfaz.campo(c == null ? "" : c.correo(), "Opcional");
        TextField direccionErht = UtilidadesInterfaz.campo(c == null ? "" : c.direccion(), "Opcional");

        GridPane g = UtilidadesInterfaz.formulario();
        UtilidadesInterfaz.fila(g, 0, "Tipo de documento *", tipoErht);
        UtilidadesInterfaz.fila(g, 1, "Número de documento *", numeroErht);
        UtilidadesInterfaz.fila(g, 2, "Nombres *", nombresErht);
        UtilidadesInterfaz.fila(g, 3, "Apellidos", apellidosErht);
        UtilidadesInterfaz.fila(g, 4, "Teléfono", telefonoErht);
        UtilidadesInterfaz.fila(g, 5, "Email", correoErht);
        UtilidadesInterfaz.fila(g, 6, "Dirección", direccionErht);

        BorderPane raizErht = new BorderPane();
        VBox cabeceraErht = new VBox(4, UtilidadesInterfaz.titulo(c == null ? "Nuevo cliente" : "Editar cliente"),
                UtilidadesInterfaz.subtitulo("Los campos con * son obligatorios."));
        cabeceraErht.getStyleClass().add("cabecera");
        raizErht.setTop(cabeceraErht);
        VBox centroErht = new VBox(g);
        centroErht.getStyleClass().add("contenido");
        raizErht.setCenter(centroErht);

        Stage ventanaDialogoErht = UtilidadesInterfaz.ventana(c == null ? "Nuevo cliente" : "Editar cliente", raizErht, 540, 480);
        Button cancelarErht = new Button("Cancelar");
        cancelarErht.setOnAction(e -> ventanaDialogoErht.close());
        Button guardarErht = UtilidadesInterfaz.estilo(new Button("Guardar"), "boton-primario");
        guardarErht.setDefaultButton(true);
        guardarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(() -> {
            guardadoErht = sErht.clientes().guardar(idClienteErht, UtilidadesInterfaz.requerido(tipoErht, "Tipo de documento"),
                    UtilidadesInterfaz.requerido(numeroErht, "Número de documento"), UtilidadesInterfaz.requerido(nombresErht, "Nombres"),
                    apellidosErht.getText(), telefonoErht.getText(), correoErht.getText(), direccionErht.getText());
            ventanaDialogoErht.close();
            UtilidadesInterfaz.mostrarInformacion("Cliente guardado correctamente.");
        }));
        HBox pieErht = new HBox(cancelarErht, guardarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
        ventanaDialogoErht.showAndWait();
    }
}
