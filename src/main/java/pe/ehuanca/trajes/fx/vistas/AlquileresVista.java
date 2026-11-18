/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
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
 * Lista de alquileres con filtro por estado y búsqueda por cliente o conjunto.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class AlquileresVista extends VBox {

    private final Servicios sErht;
    private final ComboBox<String> estadoErht = UtilidadesInterfaz.comboTexto(
            List.of("SOLICITADO", "RESERVADO", "ENTREGADO", "DEVUELTO", "ANULADO", "RECHAZADO"), "Todos");
    private final TextField textoErht = UtilidadesInterfaz.campo("", "Cliente o conjunto...");
    private final Label cantidadErht = UtilidadesInterfaz.subtitulo("");
    private final TableView<AlquilerResumen> tablaErht = UtilidadesInterfaz.tabla("No hay alquileres con esos filtros.");

    public AlquileresVista(Servicios s) {
        this.sErht = s;
        getStyleClass().add("contenido");

        estadoErht.setMaxWidth(170);
        textoErht.setPrefWidth(280);
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
        filtrosErht.getStyleClass().add("barra-filtros");

        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", AlquilerResumen::cliente, 220));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 170));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Devuelto", AlquilerResumen::fechaDevolucionReal, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(170));
        UtilidadesInterfaz.alDobleClic(tablaErht, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        tablaErht.setMinHeight(380);
        HBox cabeceraErht = new HBox(UtilidadesInterfaz.encabezado("Gestión de alquileres", "Alquileres",
                "Doble clic en un alquiler para ver su detalle, registrar pagos, entregarlo o recibirlo."));
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabeceraErht, filtrosErht, tablaErht);
        cargarErht();
    }

    private void cargarErht() {
        List<AlquilerResumen> listaErht = sErht.alquileres().listar(estadoErht.getValue(), textoErht.getText());
        tablaErht.getItems().setAll(listaErht);
        cantidadErht.setText(listaErht.size() + " alquiler(es)");
    }
}
