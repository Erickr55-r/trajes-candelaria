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

    private final Servicios s;
    private final TableView<ConjuntoFila> tabla = UtilidadesInterfaz.tabla("No hay conjuntos registrados.");

    public ConjuntosVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", ConjuntoFila::nombre, 230));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Danza", ConjuntoFila::danza, 170));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Delegado", ConjuntoFila::delegado, 200));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Teléfono", ConjuntoFila::telefono, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", ConjuntoFila::alquileres, 85));
        HBox.setHgrow(tabla, Priority.ALWAYS);

        TextField nombre = UtilidadesInterfaz.campo("", "Ej.: Diablada Bellavista");
        ComboBox<Opcion> danza = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, null);
        ComboBox<Opcion> delegado = UtilidadesInterfaz.combo(s.clientes().opciones(), Opcion::nombre, "— Sin delegado —");
        TextField telefono = UtilidadesInterfaz.campo("", "Opcional");

        GridPane g = UtilidadesInterfaz.formulario();
        g.getColumnConstraints().get(1).setMinWidth(200);
        UtilidadesInterfaz.fila(g, 0, "Nombre *", nombre);
        UtilidadesInterfaz.fila(g, 1, "Danza *", danza);
        UtilidadesInterfaz.fila(g, 2, "Delegado", delegado);
        UtilidadesInterfaz.fila(g, 3, "Teléfono", telefono);

        Button guardar = UtilidadesInterfaz.estilo(new Button("Registrar conjunto"), "boton-primario");
        guardar.setMaxWidth(Double.MAX_VALUE);
        guardar.setOnAction(e -> {
            if (UtilidadesInterfaz.ejecutar(() -> {
                Opcion d = delegado.getValue();
                s.clientes().crearConjunto(UtilidadesInterfaz.requerido(nombre, "Nombre"), UtilidadesInterfaz.requerido(danza, "Danza").id(),
                        d == null ? null : d.id(), telefono.getText());
                cargar();
            })) {
                nombre.clear();
                telefono.clear();
                delegado.getSelectionModel().selectFirst();
                UtilidadesInterfaz.mostrarInformacion("Conjunto registrado.");
            }
        });

        VBox formulario = UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Nuevo conjunto"),
                UtilidadesInterfaz.subtitulo("Los conjuntos que alquilan 5 trajes o más reciben 10% de descuento."), g, guardar);
        formulario.setPrefWidth(380);
        formulario.setMinWidth(340);

        HBox cuerpo = new HBox(16, tabla, new VBox(formulario));
        VBox.setVgrow(cuerpo, Priority.ALWAYS);
        tabla.setMinHeight(380);
        getChildren().addAll(UtilidadesInterfaz.encabezado("Agrupaciones", "Conjuntos",
                "Agrupaciones de danza que alquilan trajes con un delegado."), cuerpo);
        cargar();
    }

    private void cargar() {
        tabla.getItems().setAll(s.clientes().conjuntos());
    }
}
