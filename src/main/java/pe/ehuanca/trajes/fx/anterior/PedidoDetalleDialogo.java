/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.anterior;

import pe.ehuanca.trajes.fx.anterior.tresd.Maniqui3D;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.fx.vistas.AccionPedido;

import pe.ehuanca.trajes.fx.anterior.tresd.Visor3D;
import pe.ehuanca.trajes.modelo.PedidoDetalle;
import pe.ehuanca.trajes.modelo.PedidoImplemento;
import pe.ehuanca.trajes.modelo.PedidoParte;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import pe.ehuanca.trajes.servicio.PersonalizadorServicio;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * LEGADO: personalizador 3D con figuras básicas (TriangleMesh). Ya no aparece en ningún menú;
 * se reemplazó por el personalizador por capas (fx/portal/DisenaTuTrajeVista). Se conserva por si acaso.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class PedidoDetalleDialogo {

    private final Servicios s;
    private final int idPedido;
    private final BorderPane raiz = new BorderPane();
    private final Visor3D visor = new Visor3D(420, 520);
    private Stage ventanaDialogo;

    public PedidoDetalleDialogo(Servicios s, int idPedido) {
        this.s = s;
        this.idPedido = idPedido;
    }

    /** Abre el diálogo y espera a que se cierre. */
    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargar)) {
            return;
        }
        ventanaDialogo = UtilidadesInterfaz.ventana("Pedido personalizado N° " + idPedido, raiz, 1180, 760);
        ventanaDialogo.setOnHidden(e -> visor.detener());
        ventanaDialogo.showAndWait();
    }

    private void cargar() {
        PedidoDetalle d = s.personalizador().detallePedido(idPedido);
        PedidoResumen p = d.resumen();

        // Maniquí con los colores e implementos guardados
        for (PedidoParte parte : d.partes()) {
            visor.maniqui().colorear(parte.parte(), Color.web(parte.colorHex()));
        }
        visor.maniqui().armar(p.danza(), d.partes().stream().map(PedidoParte::parte).toList());
        visor.maniqui().mostrarImplementos(d.implementos().stream()
                .map(i -> new Maniqui3D.Adorno(i.implemento(), i.parteDestino(), i.cantidad())).toList());

        raiz.setTop(cabecera(p));

        HBox tarjetas = new HBox(12,
                UtilidadesInterfaz.tarjeta("Total del pedido", UtilidadesInterfaz.soles(p.montoTotal()), "granate"),
                UtilidadesInterfaz.tarjeta("Adelanto pagado", UtilidadesInterfaz.soles(p.adelanto()), "verde"),
                UtilidadesInterfaz.tarjeta("Saldo pendiente", UtilidadesInterfaz.soles(p.saldo()), p.saldo().signum() > 0 ? "naranja" : "verde"));

        VBox derecha = new VBox(14, tarjetas);
        if (!"ENTREGADO".equals(p.estado()) && !"CANCELADO".equals(p.estado())) {
            derecha.getChildren().add(acciones(p));
        }
        derecha.getChildren().addAll(
                UtilidadesInterfaz.seccion("Partes y colores (" + d.partes().size() + ")"), tablaPartes(d),
                UtilidadesInterfaz.seccion("Implementos (" + d.implementos().size() + ")"), tablaImplementos(d));
        ScrollPane desplazable = new ScrollPane(derecha);
        desplazable.setFitToWidth(true);
        HBox.setHgrow(desplazable, Priority.ALWAYS);

        VBox cajaVisor = UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Diseño en 3D"), visor);
        VBox.setVgrow(visor, Priority.ALWAYS);
        cajaVisor.setPrefWidth(440);
        cajaVisor.setMinWidth(380);

        HBox cuerpo = new HBox(16, cajaVisor, desplazable);
        cuerpo.getStyleClass().add("contenido");
        raiz.setCenter(cuerpo);

        Button guardar = new Button("Guardar diseño como imagen");
        guardar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::guardarImagen));
        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventanaDialogo.close());
        HBox pie = new HBox(guardar, cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    private VBox cabecera(PedidoResumen p) {
        HBox linea = new HBox(12, UtilidadesInterfaz.titulo("Pedido N° " + p.idPedido()), UtilidadesInterfaz.insigniaEstado(p.estado()), UtilidadesInterfaz.insigniaEstado(p.tipo()));
        linea.setAlignment(Pos.CENTER_LEFT);
        VBox caja = new VBox(4, linea,
                UtilidadesInterfaz.subtitulo("Cliente: " + p.cliente() + "   ·   Danza: " + p.danza() + "   ·   Talla: " + p.talla()),
                UtilidadesInterfaz.subtitulo("Pedido: " + UtilidadesInterfaz.fecha(p.fechaPedido()) + "   ·   Entrega: " + UtilidadesInterfaz.fecha(p.fechaEntrega())));
        if (p.observacion() != null && !p.observacion().isBlank()) {
            caja.getChildren().add(UtilidadesInterfaz.subtitulo("Observación: " + p.observacion()));
        }
        caja.getStyleClass().add("cabecera");
        return caja;
    }

    private VBox acciones(PedidoResumen p) {
        HBox fila1 = new HBox(10);
        fila1.setAlignment(Pos.CENTER_LEFT);
        // misma acción que en la tabla de Pedidos (Cotizar, Iniciar confección, Marcar listo, Entregar)
        fila1.getChildren().add(AccionPedido.crear(s, p, () -> hacer(() -> { }, "Pedido actualizado.")));
        Button anular = UtilidadesInterfaz.estilo(new Button("Cancelar pedido"), "boton-peligro");
        anular.setOnAction(e -> {
            if (UtilidadesInterfaz.confirmar("¿Seguro que deseas CANCELAR el pedido N° " + idPedido + "? Esta acción no se puede deshacer.")) {
                hacer(() -> s.personalizador().cambiarEstado(idPedido, "CANCELADO"), "Pedido cancelado.");
            }
        });
        fila1.getChildren().addAll(UtilidadesInterfaz.espacio(), anular);

        TextField monto = UtilidadesInterfaz.campo(UtilidadesInterfaz.montoEditable(p.saldo()), "Monto S/");
        monto.setPrefWidth(120);
        Button pagar = UtilidadesInterfaz.estilo(new Button("Registrar adelanto"), "boton-dorado");
        pagar.setOnAction(e -> hacer(() -> s.personalizador().registrarAdelanto(idPedido, UtilidadesInterfaz.monto(monto.getText())),
                "Adelanto registrado."));
        HBox fila2 = new HBox(10, UtilidadesInterfaz.etiqueta("Pago a cuenta"), monto, pagar);
        fila2.setAlignment(Pos.CENTER_LEFT);

        return UtilidadesInterfaz.panel(UtilidadesInterfaz.seccion("Acciones"),
                UtilidadesInterfaz.subtitulo("Flujo: PENDIENTE → COTIZADO → APROBADO → EN_CONFECCION → LISTO → ENTREGADO. "
                        + "Para entregar, el saldo debe estar pagado."),
                fila1, fila2);
    }

    /** Ejecuta una acción, recarga el diálogo y muestra el mensaje de éxito. */
    private void hacer(UtilidadesInterfaz.Accion accion, String exito) {
        if (UtilidadesInterfaz.ejecutar(() -> {
            accion.ejecutar();
            cargar();
        })) {
            UtilidadesInterfaz.mostrarInformacion(exito);
        }
    }

    private void guardarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar diseño como imagen");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        fc.setInitialFileName("pedido_" + idPedido + ".png");
        File archivo = fc.showSaveDialog(ventanaDialogo);
        if (archivo != null) {
            visor.guardarCaptura(archivo);
            UtilidadesInterfaz.mostrarInformacion("Diseño guardado en:\n" + archivo.getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------ tablas

    private TableView<PedidoParte> tablaPartes(PedidoDetalle d) {
        TableView<PedidoParte> t = UtilidadesInterfaz.tabla("Sin partes.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Parte", PedidoParte::parte, 160));
        TableColumn<PedidoParte, PedidoParte> color = new TableColumn<>(UtilidadesInterfaz.cabeceraColumna("Color"));
        color.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        color.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PedidoParte item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Circle muestra = new Circle(7, Color.web(item.colorHex()));
                muestra.setStroke(Color.rgb(0, 0, 0, 0.3));
                HBox caja = new HBox(8, muestra, new Label(item.color()));
                caja.setAlignment(Pos.CENTER_LEFT);
                setGraphic(caja);
            }
        });
        color.setPrefWidth(180);
        t.getColumns().add(color);
        t.getColumns().add(UtilidadesInterfaz.colSoles("Precio base", PedidoParte::precioBase, 120));
        t.getItems().setAll(d.partes());
        t.setPrefHeight(60 + 30 * Math.max(1, d.partes().size()));
        return t;
    }

    private TableView<PedidoImplemento> tablaImplementos(PedidoDetalle d) {
        TableView<PedidoImplemento> t = UtilidadesInterfaz.tabla("Sin implementos.");
        t.getColumns().add(UtilidadesInterfaz.colTexto("Implemento", PedidoImplemento::implemento, 160));
        t.getColumns().add(UtilidadesInterfaz.colTexto("Va en", PedidoImplemento::parteDestino, 100));
        t.getColumns().add(UtilidadesInterfaz.colNumero("Cantidad", PedidoImplemento::cantidad, 80));
        t.getColumns().add(UtilidadesInterfaz.colSoles("P. unit.", PedidoImplemento::precio, 100));
        t.getColumns().add(UtilidadesInterfaz.colSoles("Subtotal", PedidoImplemento::subtotal, 110));
        t.getItems().setAll(d.implementos());
        t.setPrefHeight(60 + 30 * Math.max(1, d.implementos().size()));
        return t;
    }
}
