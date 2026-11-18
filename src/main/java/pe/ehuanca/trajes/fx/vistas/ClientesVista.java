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

    private final Servicios s;
    private final TextField texto = UtilidadesInterfaz.campo("", "Nombre o número de documento...");
    private final Label cantidad = UtilidadesInterfaz.subtitulo("");
    private final TableView<ClienteFila> tabla = UtilidadesInterfaz.tabla("No se encontraron clientes.");

    public ClientesVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        texto.setPrefWidth(320);
        texto.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        Button buscar = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> {
            texto.clear();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        HBox filtros = new HBox(10, UtilidadesInterfaz.etiqueta("Buscar"), texto, buscar, limpiar, UtilidadesInterfaz.espacio(), cantidad);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Tipo", ClienteFila::tipoDocumento, 55));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Documento", ClienteFila::numeroDocumento, 115));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Nombre", ClienteFila::nombreCompleto, 280));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Teléfono", ClienteFila::telefono, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Email", ClienteFila::correo, 220));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Alquileres", ClienteFila::alquileres, 85));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Total gastado", ClienteFila::totalGastado, 120));
        UtilidadesInterfaz.alDobleClic(tabla, c -> verDetalle(c.idCliente()));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button nuevo = UtilidadesInterfaz.estilo(new Button("Nuevo cliente", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
        nuevo.setOnAction(e -> {
            new ClienteFormularioDialogo(s, null).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        Button editar = new Button("Editar", UtilidadesInterfaz.icono(Feather.EDIT_2));
        editar.setOnAction(e -> conSeleccion(c -> {
            new ClienteFormularioDialogo(s, c.idCliente()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        }));
        Button detalle = new Button("Ver detalle", UtilidadesInterfaz.icono(Feather.EYE));
        detalle.setOnAction(e -> conSeleccion(c -> verDetalle(c.idCliente())));

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(10, UtilidadesInterfaz.encabezado("Cartera", "Clientes",
                "Doble clic en un cliente para ver sus datos y alquileres."), UtilidadesInterfaz.espacio(), editar, detalle, nuevo);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<ClienteFila> lista = s.clientes().listar(texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " cliente(s)");
    }

    private void verDetalle(int idCliente) {
        new ClienteDetalleDialogo(s, idCliente).mostrar();
        UtilidadesInterfaz.ejecutar(this::cargar);
    }

    private void conSeleccion(Consumer<ClienteFila> accion) {
        ClienteFila c = tabla.getSelectionModel().getSelectedItem();
        if (c == null) {
            UtilidadesInterfaz.mostrarInformacion("Selecciona un cliente de la tabla.");
            return;
        }
        accion.accept(c);
    }
}
