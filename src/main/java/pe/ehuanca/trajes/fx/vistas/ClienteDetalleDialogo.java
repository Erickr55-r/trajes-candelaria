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

    private final Servicios s;
    private final int idCliente;
    private final BorderPane raiz = new BorderPane();
    private Stage ventanaDialogo;

    public ClienteDetalleDialogo(Servicios s, int idCliente) {
        this.s = s;
        this.idCliente = idCliente;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargar)) {
            return;
        }
        ventanaDialogo = UtilidadesInterfaz.ventana("Detalle del cliente", raiz, 1000, 660);
        ventanaDialogo.showAndWait();
    }

    private void cargar() {
        Cliente c = s.clientes().buscar(idCliente);
        List<AlquilerResumen> alquileres = s.alquileres().delCliente(idCliente);

        String nombre = c.nombres() + (c.apellidos() != null ? " " + c.apellidos() : "");
        Button editar = new Button("Editar datos");
        editar.setOnAction(e -> {
            new ClienteFormularioDialogo(s, idCliente).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        HBox arriba = new HBox(UtilidadesInterfaz.titulo(nombre), UtilidadesInterfaz.espacio(), editar);
        arriba.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(4, arriba,
                UtilidadesInterfaz.subtitulo(c.tipoDocumento() + " " + c.numeroDocumento()
                        + "   ·   Teléfono: " + UtilidadesInterfaz.texto(c.telefono())
                        + "   ·   Email: " + UtilidadesInterfaz.texto(c.correo())),
                UtilidadesInterfaz.subtitulo("Dirección: " + UtilidadesInterfaz.texto(c.direccion())
                        + "   ·   Cliente desde: " + UtilidadesInterfaz.fecha(c.fechaRegistro())));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);

        BigDecimal total = alquileres.stream().filter(a -> !List.of("ANULADO", "SOLICITADO", "RECHAZADO").contains(a.estado()))
                .map(AlquilerResumen::montoAlquiler).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activos = alquileres.stream()
                .filter(a -> "RESERVADO".equals(a.estado()) || "ENTREGADO".equals(a.estado())).count();
        HBox tarjetas = new HBox(12,
                UtilidadesInterfaz.tarjeta("Alquileres", String.valueOf(alquileres.size()), "granate"),
                UtilidadesInterfaz.tarjeta("Reservados o en curso", String.valueOf(activos), "azul"),
                UtilidadesInterfaz.tarjeta("Total gastado", UtilidadesInterfaz.soles(total), "dorado"));

        TableView<AlquilerResumen> tabla = UtilidadesInterfaz.tabla("Este cliente todavía no tiene alquileres.");
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 180));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(170));
        tabla.getItems().setAll(alquileres);
        UtilidadesInterfaz.alDobleClic(tabla, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox centro = new VBox(14, tarjetas, UtilidadesInterfaz.seccion("Alquileres del cliente"),
                UtilidadesInterfaz.subtitulo("Doble clic para abrir el alquiler."), tabla);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventanaDialogo.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }
}
