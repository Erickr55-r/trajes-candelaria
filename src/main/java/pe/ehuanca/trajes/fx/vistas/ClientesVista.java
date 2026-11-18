/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.ClienteFila;
import org.kordamp.ikonli.feather.Feather;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lista de clientes con búsqueda por nombre o documento.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class ClientesVista extends VBox {

    private final Servicios sErht;
    private final TextField textoErht = UtilidadesInterfaz.campo("", "Nombre o número de documento...");
    private final Label cantidadErht = UtilidadesInterfaz.subtitulo("");
    private final TableView<ClienteFila> tablaErht = UtilidadesInterfaz.tabla("No se encontraron clientes.");

    public ClientesVista(Servicios s) {
        this.sErht = s;
        getStyleClass().add("contenido");

        textoErht.setPrefWidth(320);
        textoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        Button buscarErht = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        Button limpiarErht = new Button("Limpiar");
        limpiarErht.setOnAction(e -> {
            textoErht.clear();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox filtrosErht = new HBox(10, UtilidadesInterfaz.etiqueta("Buscar"), textoErht, buscarErht, limpiarErht, UtilidadesInterfaz.espacio(), cantidadErht);
        filtrosErht.getStyleClass().add("barra-filtros");

        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Tipo", ClienteFila::tipoDocumento, 55));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Documento", ClienteFila::numeroDocumento, 115));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Nombre", ClienteFila::nombreCompleto, 280));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Teléfono", ClienteFila::telefono, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Email", ClienteFila::correo, 220));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", ClienteFila::alquileres, 85));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Total gastado", ClienteFila::totalGastado, 120));
        UtilidadesInterfaz.alDobleClic(tablaErht, c -> verDetalleErht(c.idCliente()));
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        Button nuevoErht = UtilidadesInterfaz.estilo(new Button("Nuevo cliente", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
        nuevoErht.setOnAction(e -> {
            new ClienteFormularioDialogo(s, null).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        Button editarErht = new Button("Editar", UtilidadesInterfaz.icono(Feather.EDIT_2));
        editarErht.setOnAction(e -> conSeleccionErht(c -> {
            new ClienteFormularioDialogo(s, c.idCliente()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        }));
        Button detalleErht = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalleErht.setOnAction(e -> conSeleccionErht(c -> verDetalleErht(c.idCliente())));

        tablaErht.setMinHeight(380);
        HBox cabeceraErht = new HBox(10, UtilidadesInterfaz.encabezado("Cartera", "Clientes",
                "Doble clic en un cliente para ver sus datos y alquileres."), UtilidadesInterfaz.espacio(), editarErht, detalleErht, nuevoErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, filtrosErht, tablaErht);
        cargarErht();
    }

    private void cargarErht() {
        List<ClienteFila> listaErht = sErht.clientes().listar(textoErht.getText());
        tablaErht.getItems().setAll(listaErht);
        cantidadErht.setText(listaErht.size() + " cliente(s)");
    }

    private void verDetalleErht(int idCliente) {
        new ClienteDetalleDialogo(sErht, idCliente).mostrar();
        UtilidadesInterfaz.ejecutar(this::cargarErht);
    }

    private void conSeleccionErht(Consumer<ClienteFila> accion) {
        ClienteFila c = tablaErht.getSelectionModel().getSelectedItem();
        if (c == null) {
            UtilidadesInterfaz.mostrarInformacion("Selecciona un cliente de la tabla.");
            return;
        }
        accion.accept(c);
    }
}
