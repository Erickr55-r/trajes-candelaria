/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.TrajeFila;
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

/**
 * Inventario de trajes con filtros.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class TrajesVista extends VBox {

    private final Servicios sErht;
    private final ComboBox<Opcion> danzaErht;
    private final ComboBox<String> estadoErht = UtilidadesInterfaz.comboTexto(List.of("DISPONIBLE", "MANTENIMIENTO", "BAJA"), "Todos");
    private final ComboBox<String> tallaErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, "Todas");
    private final TextField textoErht = UtilidadesInterfaz.campo("", "Código, personaje o color...");
    private final Label cantidadErht = UtilidadesInterfaz.subtitulo("");
    private final TableView<TrajeFila> tablaErht = UtilidadesInterfaz.tabla("No hay trajes con esos filtros.");

    public TrajesVista(Servicios s) {
        this.sErht = s;
        getStyleClass().add("contenido");

        danzaErht = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, "Todas");
        danzaErht.setPrefWidth(220);
        estadoErht.setPrefWidth(150);
        tallaErht.setPrefWidth(90);
        textoErht.setPrefWidth(220);
        danzaErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        estadoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        tallaErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        textoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        Button buscarErht = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));

        HBox filtrosErht = new HBox(10, UtilidadesInterfaz.etiqueta("Danza"), danzaErht, UtilidadesInterfaz.etiqueta("Estado"), estadoErht,
                UtilidadesInterfaz.etiqueta("Talla"), tallaErht, textoErht, buscarErht, UtilidadesInterfaz.espacio(), cantidadErht);
        filtrosErht.getStyleClass().add("barra-filtros");

        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Código", TrajeFila::codigo, 85));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Danza", TrajeFila::danza, 170));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Personaje", TrajeFila::personaje, 180));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Género", TrajeFila::genero, 75));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Talla", TrajeFila::talla, 55));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Color", TrajeFila::color, 120));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Precio", TrajeFila::precioAlquiler, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Garantía", TrajeFila::garantia, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", TrajeFila::vecesAlquilado, 80));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Estado", TrajeFila::estado, 130));
        UtilidadesInterfaz.alDobleClic(tablaErht, t -> verDetalleErht(t.idTraje()));
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        Button nuevoErht = UtilidadesInterfaz.estilo(new Button("Nuevo traje", UtilidadesInterfaz.icono(Feather.PLUS)), "boton-primario");
        nuevoErht.setOnAction(e -> {
            Integer id = new TrajeFormularioDialogo(s, null).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
            if (id != null) {
                verDetalleErht(id);
            }
        });
        Button editarErht = new Button("Editar", UtilidadesInterfaz.icono(Feather.EDIT_2));
        editarErht.setOnAction(e -> conSeleccionErht(t -> {
            new TrajeFormularioDialogo(s, t.idTraje()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        }));
        Button detalleErht = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalleErht.setOnAction(e -> conSeleccionErht(t -> verDetalleErht(t.idTraje())));

        tablaErht.setMinHeight(380);
        HBox cabeceraErht = new HBox(10, UtilidadesInterfaz.encabezado("Inventario", "Trajes",
                "Doble clic en un traje para ver sus piezas, mantenimiento e historial."),
                UtilidadesInterfaz.espacio(), editarErht, detalleErht, nuevoErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, filtrosErht, tablaErht);
        cargarErht();
    }

    private void cargarErht() {
        Opcion d = danzaErht.getValue();
        List<TrajeFila> listaErht = sErht.trajes().listar(d == null ? null : d.id(), estadoErht.getValue(), tallaErht.getValue(),
                textoErht.getText());
        tablaErht.getItems().setAll(listaErht);
        cantidadErht.setText(listaErht.size() + " traje(s)");
    }

    private void verDetalleErht(int idTraje) {
        new TrajeDetalleDialogo(sErht, idTraje).mostrar();
        UtilidadesInterfaz.ejecutar(this::cargarErht);
    }

    private void conSeleccionErht(java.util.function.Consumer<TrajeFila> accion) {
        TrajeFila t = tablaErht.getSelectionModel().getSelectedItem();
        if (t == null) {
            UtilidadesInterfaz.mostrarInformacion("Selecciona un traje de la tabla.");
            return;
        }
        accion.accept(t);
    }
}
