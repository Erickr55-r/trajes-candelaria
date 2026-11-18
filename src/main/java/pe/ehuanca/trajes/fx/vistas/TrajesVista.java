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

    private final Servicios s;
    private final ComboBox<Opcion> danza;
    private final ComboBox<String> estado = UtilidadesInterfaz.comboTexto(List.of("DISPONIBLE", "MANTENIMIENTO", "BAJA"), "Todos");
    private final ComboBox<String> talla = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, "Todas");
    private final TextField texto = UtilidadesInterfaz.campo("", "Código, personaje o color...");
    private final Label cantidad = UtilidadesInterfaz.subtitulo("");
    private final TableView<TrajeFila> tabla = UtilidadesInterfaz.tabla("No hay trajes con esos filtros.");

    public TrajesVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        danza = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, "Todas");
        danza.setPrefWidth(220);
        estado.setPrefWidth(150);
        talla.setPrefWidth(90);
        texto.setPrefWidth(220);
        danza.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        estado.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        talla.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        texto.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        Button buscar = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));

        HBox filtros = new HBox(10, UtilidadesInterfaz.etiqueta("Danza"), danza, UtilidadesInterfaz.etiqueta("Estado"), estado,
                UtilidadesInterfaz.etiqueta("Talla"), talla, texto, buscar, UtilidadesInterfaz.espacio(), cantidad);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Código", TrajeFila::codigo, 85));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Danza", TrajeFila::danza, 170));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Personaje", TrajeFila::personaje, 180));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Género", TrajeFila::genero, 75));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Talla", TrajeFila::talla, 55));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Color", TrajeFila::color, 120));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Precio", TrajeFila::precioAlquiler, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Garantía", TrajeFila::garantia, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", TrajeFila::vecesAlquilado, 80));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Estado", TrajeFila::estado, 130));
        UtilidadesInterfaz.alDobleClic(tabla, t -> verDetalle(t.idTraje()));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = UtilidadesInterfaz.estilo(new Button("Nuevo traje", UtilidadesInterfaz.icono(Feather.PLUS)), "boton-primario");
        nuevo.setOnAction(e -> {
            Integer id = new TrajeFormularioDialogo(s, null).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
            if (id != null) {
                verDetalle(id);
            }
        });
        Button editar = new Button("Editar", UtilidadesInterfaz.icono(Feather.EDIT_2));
        editar.setOnAction(e -> conSeleccion(t -> {
            new TrajeFormularioDialogo(s, t.idTraje()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        }));
        Button detalle = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalle.setOnAction(e -> conSeleccion(t -> verDetalle(t.idTraje())));

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(10, UtilidadesInterfaz.encabezado("Inventario", "Trajes",
                "Doble clic en un traje para ver sus piezas, mantenimiento e historial."),
                UtilidadesInterfaz.espacio(), editar, detalle, nuevo);
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
        UtilidadesInterfaz.ejecutar(this::cargar);
    }

    private void conSeleccion(java.util.function.Consumer<TrajeFila> accion) {
        TrajeFila t = tabla.getSelectionModel().getSelectedItem();
        if (t == null) {
            UtilidadesInterfaz.mostrarInformacion("Selecciona un traje de la tabla.");
            return;
        }
        accion.accept(t);
    }
}
