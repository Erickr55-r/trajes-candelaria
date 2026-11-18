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

    private final Servicios s;
    private final ComboBox<String> estado = UtilidadesInterfaz.comboTexto(
            List.of("SOLICITADO", "RESERVADO", "ENTREGADO", "DEVUELTO", "ANULADO", "RECHAZADO"), "Todos");
    private final TextField texto = UtilidadesInterfaz.campo("", "Cliente o conjunto...");
    private final Label cantidad = UtilidadesInterfaz.subtitulo("");
    private final TableView<AlquilerResumen> tabla = UtilidadesInterfaz.tabla("No hay alquileres con esos filtros.");

    public AlquileresVista(Servicios s) {
        this.s = s;
        getStyleClass().add("contenido");

        estado.setMaxWidth(170);
        texto.setPrefWidth(280);
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
        filtros.getStyleClass().add("barra-filtros");

        tabla.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", AlquilerResumen::cliente, 220));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 170));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Devuelto", AlquilerResumen::fechaDevolucionReal, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(170));
        UtilidadesInterfaz.alDobleClic(tabla, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        tabla.setMinHeight(380);
        HBox cabecera = new HBox(UtilidadesInterfaz.encabezado("Gestión de alquileres", "Alquileres",
                "Doble clic en un alquiler para ver su detalle, registrar pagos, entregarlo o recibirlo."));
        cabecera.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(cabecera, filtros, tabla);
        cargar();
    }

    private void cargar() {
        List<AlquilerResumen> lista = s.alquileres().listar(estado.getValue(), texto.getText());
        tabla.getItems().setAll(lista);
        cantidad.setText(lista.size() + " alquiler(es)");
    }
}
