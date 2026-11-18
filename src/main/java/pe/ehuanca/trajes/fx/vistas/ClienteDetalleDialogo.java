/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import pe.ehuanca.trajes.modelo.Cliente;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;

/**
 * Detalle de un cliente con sus alquileres.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class ClienteDetalleDialogo {

    private final Servicios sErht;
    private final int idClienteErht;
    private final BorderPane raizErht = new BorderPane();
    private Stage ventanaDialogoErht;

    public ClienteDetalleDialogo(Servicios s, int idCliente) {
        this.sErht = s;
        this.idClienteErht = idCliente;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargarErht)) {
            return;
        }
        ventanaDialogoErht = UtilidadesInterfaz.ventana("Detalle del cliente", raizErht, 1000, 660);
        ventanaDialogoErht.showAndWait();
    }

    private void cargarErht() {
        Cliente c = sErht.clientes().buscar(idClienteErht);
        List<AlquilerResumen> alquileresErht = sErht.alquileres().delCliente(idClienteErht);

        String nombreErht = c.nombres() + (c.apellidos() != null ? " " + c.apellidos() : "");
        Button editarErht = new Button("Editar datos");
        editarErht.setOnAction(e -> {
            new ClienteFormularioDialogo(sErht, idClienteErht).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox arribaErht = new HBox(UtilidadesInterfaz.titulo(nombreErht), UtilidadesInterfaz.espacio(), editarErht);
        arribaErht.setAlignment(Pos.CENTER_LEFT);
        VBox cabeceraErht = new VBox(4, arribaErht,
                UtilidadesInterfaz.subtitulo(c.tipoDocumento() + " " + c.numeroDocumento()
                        + "   ·   Teléfono: " + UtilidadesInterfaz.texto(c.telefono())
                        + "   ·   Email: " + UtilidadesInterfaz.texto(c.correo())),
                UtilidadesInterfaz.subtitulo("Dirección: " + UtilidadesInterfaz.texto(c.direccion())
                        + "   ·   Cliente desde: " + UtilidadesInterfaz.fecha(c.fechaRegistro())));
        cabeceraErht.getStyleClass().add("cabecera");
        raizErht.setTop(cabeceraErht);

        BigDecimal totalErht = alquileresErht.stream().filter(a -> !List.of("ANULADO", "SOLICITADO", "RECHAZADO").contains(a.estado()))
                .map(AlquilerResumen::montoAlquiler).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activosErht = alquileresErht.stream()
                .filter(a -> "RESERVADO".equals(a.estado()) || "ENTREGADO".equals(a.estado())).count();
        HBox tarjetasErht = new HBox(12,
                UtilidadesInterfaz.tarjeta("Alquileres", String.valueOf(alquileresErht.size()), "granate"),
                UtilidadesInterfaz.tarjeta("Reservados o en curso", String.valueOf(activosErht), "azul"),
                UtilidadesInterfaz.tarjeta("Total gastado", UtilidadesInterfaz.soles(totalErht), "dorado"));

        TableView<AlquilerResumen> tablaErht = UtilidadesInterfaz.tabla("Este cliente todavía no tiene alquileres.");
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 180));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(170));
        tablaErht.getItems().setAll(alquileresErht);
        UtilidadesInterfaz.alDobleClic(tablaErht, a -> {
            new AlquilerDetalleDialogo(sErht, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        VBox centroErht = new VBox(14, tarjetasErht, UtilidadesInterfaz.seccion("Alquileres del cliente"),
                UtilidadesInterfaz.subtitulo("Doble clic para abrir el alquiler."), tablaErht);
        centroErht.getStyleClass().add("contenido");
        raizErht.setCenter(centroErht);

        Button cerrarErht = new Button("Cerrar");
        cerrarErht.setOnAction(e -> ventanaDialogoErht.close());
        HBox pieErht = new HBox(cerrarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
    }
}
