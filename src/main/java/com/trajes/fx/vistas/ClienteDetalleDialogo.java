package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.AlquilerResumen;
import com.trajes.modelo.Cliente;
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

/** Detalle de un cliente con sus alquileres. */
public class ClienteDetalleDialogo {

    private final Servicios s;
    private final int idCliente;
    private final BorderPane raiz = new BorderPane();
    private Stage stage;

    public ClienteDetalleDialogo(Servicios s, int idCliente) {
        this.s = s;
        this.idCliente = idCliente;
    }

    public void mostrar() {
        if (!Ui.ejecutar(this::cargar)) {
            return;
        }
        stage = Ui.ventana("Detalle del cliente", raiz, 1000, 660);
        stage.showAndWait();
    }

    private void cargar() {
        Cliente c = s.clientes().buscar(idCliente);
        List<AlquilerResumen> alquileres = s.alquileres().delCliente(idCliente);

        String nombre = c.nombres() + (c.apellidos() != null ? " " + c.apellidos() : "");
        Button editar = new Button("Editar datos");
        editar.setOnAction(e -> {
            new ClienteFormularioDialogo(s, idCliente).mostrar();
            Ui.ejecutar(this::cargar);
        });
        HBox arriba = new HBox(Ui.titulo(nombre), Ui.espacio(), editar);
        arriba.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(4, arriba,
                Ui.subtitulo(c.tipoDocumento() + " " + c.numeroDocumento()
                        + "   ·   Teléfono: " + Ui.texto(c.telefono())
                        + "   ·   Email: " + Ui.texto(c.email())),
                Ui.subtitulo("Dirección: " + Ui.texto(c.direccion())
                        + "   ·   Cliente desde: " + Ui.fecha(c.fechaRegistro())));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);

        BigDecimal total = alquileres.stream().filter(a -> !List.of("ANULADO", "SOLICITADO", "RECHAZADO").contains(a.estado()))
                .map(AlquilerResumen::montoAlquiler).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activos = alquileres.stream()
                .filter(a -> "RESERVADO".equals(a.estado()) || "ENTREGADO".equals(a.estado())).count();
        HBox tarjetas = new HBox(12,
                Ui.tarjeta("Alquileres", String.valueOf(alquileres.size()), "granate"),
                Ui.tarjeta("Reservados o en curso", String.valueOf(activos), "azul"),
                Ui.tarjeta("Total gastado", Ui.soles(total), "dorado"));

        TableView<AlquilerResumen> tabla = Ui.tabla("Este cliente todavía no tiene alquileres.");
        tabla.getColumns().add(Ui.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tabla.getColumns().add(Ui.colTexto("Conjunto", AlquilerResumen::conjunto, 180));
        tabla.getColumns().add(Ui.colFecha("Salida", AlquilerResumen::fechaSalida, 95));
        tabla.getColumns().add(Ui.colFecha("Devolución", AlquilerResumen::fechaDevolucionPrevista, 95));
        tabla.getColumns().add(Ui.colNumero("Trajes", AlquilerResumen::numTrajes, 60));
        tabla.getColumns().add(Ui.colSoles("Monto", AlquilerResumen::montoAlquiler, 105));
        tabla.getColumns().add(Ui.colSoles("Saldo", AlquilerResumen::saldo, 100));
        tabla.getColumns().add(Ui.colEstadoAlquiler(170));
        tabla.getItems().setAll(alquileres);
        Ui.alDobleClic(tabla, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            Ui.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        VBox centro = new VBox(14, tarjetas, Ui.seccion("Alquileres del cliente"),
                Ui.subtitulo("Doble clic para abrir el alquiler."), tabla);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> stage.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }
}
