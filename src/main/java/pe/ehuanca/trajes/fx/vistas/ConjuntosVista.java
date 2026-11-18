/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.ConjuntoFila;
import pe.ehuanca.trajes.modelo.Opcion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Conjuntos de danza: lista y formulario para registrar uno nuevo.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class ConjuntosVista extends VBox {

    private final Servicios sErht;
    private final TableView<ConjuntoFila> tablaErht = UtilidadesInterfaz.tabla("No hay conjuntos registrados.");

    public ConjuntosVista(Servicios s) {
        this.sErht = s;
        getStyleClass().add("contenido");

        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", ConjuntoFila::nombre, 230));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Danza", ConjuntoFila::danza, 170));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Delegado", ConjuntoFila::delegado, 200));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Teléfono", ConjuntoFila::telefono, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", ConjuntoFila::alquileres, 85));
        HBox.setHgrow(tablaErht, Priority.ALWAYS);

        TextField nombreErht = UtilidadesInterfaz.campo("", "Ej.: Diablada Bellavista");
        ComboBox<Opcion> danzaErht = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, null);
        ComboBox<Opcion> delegadoErht = UtilidadesInterfaz.combo(s.clientes().opciones(), Opcion::nombre, "— Sin delegado —");
        TextField telefonoErht = UtilidadesInterfaz.campo("", "Opcional");

        GridPane g = UtilidadesInterfaz.formulario();
        g.getColumnConstraints().get(1).setMinWidth(200);
        UtilidadesInterfaz.fila(g, 0, "Nombre *", nombreErht);
        UtilidadesInterfaz.fila(g, 1, "Danza *", danzaErht);
        UtilidadesInterfaz.fila(g, 2, "Delegado", delegadoErht);
        UtilidadesInterfaz.fila(g, 3, "Teléfono", telefonoErht);

        Button guardarErht = UtilidadesInterfaz.estilo(new Button("Registrar conjunto"), "boton-primario");
        guardarErht.setMaxWidth(Double.MAX_VALUE);
        guardarErht.setOnAction(e -> {
            if (UtilidadesInterfaz.ejecutar(() -> {
                Opcion d = delegadoErht.getValue();
                s.clientes().crearConjunto(UtilidadesInterfaz.requerido(nombreErht, "Nombre"), UtilidadesInterfaz.requerido(danzaErht, "Danza").id(),
                        d == null ? null : d.id(), telefonoErht.getText());
                cargarErht();
            })) {
                nombreErht.clear();
                telefonoErht.clear();
                delegadoErht.getSelectionModel().selectFirst();
                UtilidadesInterfaz.mostrarInformacion("Conjunto registrado.");
            }
        });

        VBox formularioErht = UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Nuevo conjunto"),
                UtilidadesInterfaz.subtitulo("Los conjuntos que alquilan 5 trajes o más reciben 10% de descuento."), g, guardarErht);
        formularioErht.setPrefWidth(380);
        formularioErht.setMinWidth(340);

        HBox cuerpoErht = new HBox(16, tablaErht, new VBox(formularioErht));
        VBox.setVgrow(cuerpoErht, Priority.ALWAYS);
        tablaErht.setMinHeight(380);
        getChildren().addAll(UtilidadesInterfaz.encabezado("Agrupaciones", "Conjuntos",
                "Agrupaciones de danza que alquilan trajes con un delegado."), cuerpoErht);
        cargarErht();
    }

    private void cargarErht() {
        tablaErht.getItems().setAll(sErht.clientes().conjuntos());
    }
}
