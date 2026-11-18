/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import pe.ehuanca.trajes.servicio.PersonalizadorServicio;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

/**
 * Pedidos de diseño (los que envían los clientes desde «Diseña tu traje» y los antiguos de la tienda):
 * cliente, fecha, modalidad y estado, con la acción que toca en cada fila. Doble clic abre el diseño.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class PedidosVista extends VBox {

    private final Servicios sErht;
    private final Runnable alCambiarErht;
    private final ComboBox<String> estadoErht = UtilidadesInterfaz.comboTexto(PersonalizadorServicio.ESTADOS, "Todos");
    private final TextField textoErht = UtilidadesInterfaz.campo("", "Cliente, danza o N°...");
    private final Label cantidadErht = UtilidadesInterfaz.subtitulo("");
    private final TableView<PedidoResumen> tablaErht = UtilidadesInterfaz.tabla("No hay pedidos de diseño con esos filtros.");

    public PedidosVista(Servicios s, Runnable alCambiar) {
        this.sErht = s;
        this.alCambiarErht = alCambiar;
        getStyleClass().add("contenido");

        estadoErht.setMaxWidth(200);
        textoErht.setPrefWidth(260);
        textoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        estadoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        Button buscarErht = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        Button limpiarErht = new Button("Limpiar");
        limpiarErht.setOnAction(e -> {
            textoErht.clear();
            estadoErht.getSelectionModel().selectFirst();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox filtrosErht = new HBox(10, UtilidadesInterfaz.etiqueta("Estado"), estadoErht, UtilidadesInterfaz.etiqueta("Buscar"), textoErht, buscarErht, limpiarErht,
                UtilidadesInterfaz.espacio(), cantidadErht);
        filtrosErht.setAlignment(Pos.CENTER_LEFT);
        filtrosErht.getStyleClass().add("barra-filtros");

        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("N°", PedidoResumen::idPedido, 55));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", PedidoResumen::cliente, 200));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Fecha", PedidoResumen::fechaPedido, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Danza", PedidoResumen::danza, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Modalidad",
                p -> "ALQUILER".equals(p.tipo()) ? "Alquilar (30%)" : "Confeccionar", 130));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Estado", PedidoResumen::estado, 140));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Estimado",
                p -> p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado(), 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Precio final",
                p -> "PENDIENTE".equals(p.estado()) ? "Por cotizar" : UtilidadesInterfaz.soles(p.montoTotal()), 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", PedidoResumen::saldo, 95));
        TableColumn<PedidoResumen, PedidoResumen> accionErht = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna("Acción"));
        accionErht.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        accionErht.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoResumen p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                setGraphic(empty || p == null ? null : AccionPedido.crear(s, p, () -> UtilidadesInterfaz.ejecutar(PedidosVista.this::cargarErht)));
            }
        });
        accionErht.setSortable(false);
        accionErht.setPrefWidth(180);
        accionErht.setMinWidth(170);
        tablaErht.getColumns().add(accionErht);
        UtilidadesInterfaz.alDobleClic(tablaErht, p -> {
            new PedidoDisenoDialogo(s, p.idPedido()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        tablaErht.setMinHeight(380);
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        HBox cabeceraErht = new HBox(UtilidadesInterfaz.encabezado("Confección a medida", "Pedidos de diseño",
                "Diseños que envían los clientes desde su portal. Cotiza los pendientes y avanza cada pedido con la "
                        + "columna Acción. Doble clic para ver el diseño, sus colores y los pagos."));
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, filtrosErht, tablaErht);
        cargarErht();
    }

    private void cargarErht() {
        List<PedidoResumen> listaErht = sErht.personalizador().listarPedidos(estadoErht.getValue(), textoErht.getText());
        tablaErht.getItems().setAll(listaErht);
        cantidadErht.setText(listaErht.size() + " pedido(s)");
        alCambiarErht.run();
    }
}
