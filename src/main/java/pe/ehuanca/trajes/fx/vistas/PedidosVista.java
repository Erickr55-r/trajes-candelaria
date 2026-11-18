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

    private final Servicios s;
    /** Avisa al panel para actualizar la insignia de pendientes. */
    private final Runnable alCambiar;
    private final ComboBox<String> estado = UtilidadesInterfaz.comboTexto(PersonalizadorServicio.ESTADOS, "Todos");
    private final TextField texto = UtilidadesInterfaz.campo("", "Cliente, danza o N°...");
    private final Label cantidad = UtilidadesInterfaz.subtitulo("");
    private final TableView<PedidoResumen> tabla = UtilidadesInterfaz.tabla("No hay pedidos de diseño con esos filtros.");

    public PedidosVista(Servicios s, Runnable alCambiar) {
        this.s = s;
        this.alCambiar = alCambiar;
        getStyleClass().add("contenido");

        estado.setMaxWidth(200);
        texto.setPrefWidth(260);
        texto.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        estado.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        Button buscar = UtilidadesInterfaz.estilo(new Button("Buscar", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        buscar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargar));
        Button limpiar = new Button("Limpiar");
        limpiar.setOnAction(e -> {
            texto.clear();
            estado.getSelectionModel().selectFirst();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        HBox filtros = new HBox(10, UtilidadesInterfaz.etiqueta("Estado"), estado, UtilidadesInterfaz.etiqueta("Buscar"), texto, buscar, limpiar,
                UtilidadesInterfaz.espacio(), cantidad);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(UtilidadesInterfaz.colNumero("N°", PedidoResumen::idPedido, 55));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", PedidoResumen::cliente, 200));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Fecha", PedidoResumen::fechaPedido, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Danza", PedidoResumen::danza, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Modalidad",
                p -> "ALQUILER".equals(p.tipo()) ? "Alquilar (30%)" : "Confeccionar", 130));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Estado", PedidoResumen::estado, 140));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Estimado",
                p -> p.precioEstimado() == null ? p.montoTotal() : p.precioEstimado(), 100));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Precio final",
                p -> "PENDIENTE".equals(p.estado()) ? "Por cotizar" : UtilidadesInterfaz.soles(p.montoTotal()), 110));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", PedidoResumen::saldo, 95));
        TableColumn<PedidoResumen, PedidoResumen> accion = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna("Acción"));
        accion.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        accion.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoResumen p, boolean empty) {
                super.updateItem(p, empty);
                setText(null);
                setGraphic(empty || p == null ? null : AccionPedido.crear(s, p, () -> UtilidadesInterfaz.ejecutar(PedidosVista.this::cargar)));
            }
        });
        accion.setSortable(false);
        accion.setPrefWidth(180);
        accion.setMinWidth(170);
        tabla.getColumns().add(accion);
        UtilidadesInterfaz.alDobleClic(tabla, p -> {
            new PedidoDisenoDialogo(s, p.idPedido()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        tabla.setMinHeight(380);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        HBox cabecera = new HBox(UtilidadesInterfaz.encabezado("Confección a medida", "Pedidos de diseño",
                "Diseños que envían los clientes desde su portal. Cotiza los pendientes y avanza cada pedido con la "
                        + "columna Acción. Doble clic para ver el diseño, sus colores y los pagos."));
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<PedidoResumen> lista = s.personalizador().listarPedidos(estado.getValue(), texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " pedido(s)");
        alCambiar.run();
    }
}
