/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.HistorialTraje;
import pe.ehuanca.trajes.modelo.MantenimientoFila;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.PiezaFila;
import pe.ehuanca.trajes.modelo.Traje;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Detalle de un traje: piezas, estado y mantenimiento, e historial de alquileres.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class TrajeDetalleDialogo {

    private final Servicios s;
    private final int idTraje;
    private final BorderPane raiz = new BorderPane();
    private int pestaniaActual;
    private Stage ventanaDialogo;

    public TrajeDetalleDialogo(Servicios s, int idTraje) {
        this.s = s;
        this.idTraje = idTraje;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargar)) {
            return;
        }
        ventanaDialogo = UtilidadesInterfaz.ventana("Detalle del traje", raiz, 980, 700);
        ventanaDialogo.showAndWait();
    }

    private void cargar() {
        Traje t = s.trajes().buscar(idTraje);

        HBox linea = new HBox(12, UtilidadesInterfaz.titulo(t.codigo() + " · " + t.personaje()), UtilidadesInterfaz.insigniaEstado(t.estado()));
        linea.setAlignment(Pos.CENTER_LEFT);
        Button editar = new Button("Editar datos");
        editar.setOnAction(e -> {
            new TrajeFormularioDialogo(s, idTraje).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        HBox arriba = new HBox(linea, UtilidadesInterfaz.espacio(), editar);
        arriba.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(4, arriba,
                UtilidadesInterfaz.subtitulo(t.danza() + "   ·   " + t.genero() + "   ·   talla " + t.talla()
                        + (t.color() != null ? "   ·   " + t.color() : "")),
                UtilidadesInterfaz.subtitulo("Alquiler: " + UtilidadesInterfaz.soles(t.precioAlquiler()) + "   ·   Garantía: " + UtilidadesInterfaz.soles(t.garantia())
                        + "   ·   Adquirido: " + UtilidadesInterfaz.fecha(t.fechaAdquisicion())));
        cabecera.getStyleClass().add("cabecera");
        raiz.setTop(cabecera);

        TabPane pestanias = new TabPane(
                new Tab("Piezas", piezas()),
                new Tab("Estado y mantenimiento", estado(t)),
                new Tab("Historial de alquileres", historial()));
        pestanias.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanias.getSelectionModel().select(pestaniaActual);
        pestanias.getSelectionModel().selectedIndexProperty()
                .addListener((o, a, b) -> pestaniaActual = b.intValue());
        VBox centro = new VBox(pestanias);
        VBox.setVgrow(pestanias, Priority.ALWAYS);
        centro.getStyleClass().add("contenido");
        raiz.setCenter(centro);

        Button cerrar = new Button("Cerrar");
        cerrar.setOnAction(e -> ventanaDialogo.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    /** Ejecuta una acción y recarga el diálogo. */
    private void hacer(UtilidadesInterfaz.Accion accion) {
        UtilidadesInterfaz.ejecutar(() -> {
            accion.ejecutar();
            cargar();
        });
    }

    // ------------------------------------------------------------------ piezas

    private VBox piezas() {
        TableView<PiezaFila> tabla = UtilidadesInterfaz.tabla("Este traje no tiene piezas registradas.");
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Pieza", PiezaFila::tipo, 160));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Descripción", PiezaFila::descripcion, 300));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Estado", PiezaFila::estado, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Valor de reposición", PiezaFila::valorReposicion, 150));
        tabla.getItems().setAll(s.trajes().piezas(idTraje));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button quitar = UtilidadesInterfaz.estilo(new Button("Quitar pieza seleccionada"), "boton-peligro");
        quitar.setOnAction(e -> {
            PiezaFila p = tabla.getSelectionModel().getSelectedItem();
            if (p == null) {
                UtilidadesInterfaz.mostrarInformacion("Selecciona una pieza de la tabla.");
            } else if (UtilidadesInterfaz.confirmar("¿Quitar la pieza \"" + p.tipo() + "\" de este traje?")) {
                hacer(() -> s.trajes().eliminarPieza(p.idPieza()));
            }
        });

        List<Opcion> tipos = s.trajes().tiposPieza();
        ComboBox<Opcion> tipo = UtilidadesInterfaz.combo(tipos, Opcion::nombre, null);
        tipo.setPrefWidth(180);
        TextField descripcion = UtilidadesInterfaz.campo("", "Descripción (opcional)");
        HBox.setHgrow(descripcion, Priority.ALWAYS);
        TextField valor = UtilidadesInterfaz.campo("", "Valor S/");
        valor.setPrefWidth(110);
        Button agregar = UtilidadesInterfaz.estilo(new Button("Agregar pieza"), "boton-primario");
        agregar.setOnAction(e -> hacer(() -> s.trajes().agregarPieza(idTraje,
                UtilidadesInterfaz.requerido(tipo, "Tipo de pieza").id(), descripcion.getText(),
                UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(valor, "Valor de reposición")))));

        HBox formulario = new HBox(10, UtilidadesInterfaz.etiqueta("Tipo"), tipo, descripcion, UtilidadesInterfaz.etiqueta("Reposición"), valor, agregar);
        formulario.setAlignment(Pos.CENTER_LEFT);
        HBox acciones = new HBox(quitar);
        return new VBox(10, tabla, acciones, UtilidadesInterfaz.seccion("Agregar pieza"), formulario);
    }

    // ------------------------------------------------------------------ estado

    private VBox estado(Traje t) {
        VBox acciones = new VBox(10);
        switch (t.estado()) {
            case "DISPONIBLE" -> {
                ComboBox<String> tipo = UtilidadesInterfaz.comboTexto(List.of("LAVADO", "REPARACION", "BORDADO"), null);
                tipo.setPrefWidth(150);
                TextField detalle = UtilidadesInterfaz.campo("", "Detalle (opcional)");
                HBox.setHgrow(detalle, Priority.ALWAYS);
                Button enviar = UtilidadesInterfaz.estilo(new Button("Enviar a mantenimiento"), "boton-dorado");
                enviar.setOnAction(e -> hacer(() ->
                        s.trajes().enviarAMantenimiento(idTraje, tipo.getValue(), detalle.getText())));
                HBox fila = new HBox(10, UtilidadesInterfaz.etiqueta("Tipo"), tipo, detalle, enviar);
                fila.setAlignment(Pos.CENTER_LEFT);

                Button baja = UtilidadesInterfaz.estilo(new Button("Dar de baja"), "boton-peligro");
                baja.setOnAction(e -> {
                    if (UtilidadesInterfaz.confirmar("¿Dar de baja el traje " + t.codigo() + "? Ya no se podrá alquilar.")) {
                        hacer(() -> s.trajes().darDeBaja(idTraje));
                    }
                });
                acciones.getChildren().addAll(UtilidadesInterfaz.seccion("Enviar a lavado o reparación"),
                        UtilidadesInterfaz.subtitulo("Mientras esté en mantenimiento el traje no se puede alquilar."), fila,
                        UtilidadesInterfaz.seccion("Retirar del inventario"), baja);
            }
            case "MANTENIMIENTO" -> {
                TextField costo = UtilidadesInterfaz.campo("", "0.00");
                costo.setPrefWidth(120);
                Button terminar = UtilidadesInterfaz.estilo(new Button("Terminar mantenimiento"), "boton-primario");
                terminar.setOnAction(e -> hacer(() ->
                        s.trajes().terminarMantenimiento(idTraje, UtilidadesInterfaz.monto(costo.getText()))));
                HBox fila = new HBox(10, UtilidadesInterfaz.etiqueta("Costo S/"), costo, terminar);
                fila.setAlignment(Pos.CENTER_LEFT);
                acciones.getChildren().addAll(UtilidadesInterfaz.seccion("El traje está en mantenimiento"),
                        UtilidadesInterfaz.subtitulo("Al terminar se registra el costo y el traje vuelve a estar DISPONIBLE."), fila);
            }
            case "BAJA" -> {
                Button reactivar = UtilidadesInterfaz.estilo(new Button("Reactivar traje"), "boton-primario");
                reactivar.setOnAction(e -> hacer(() -> s.trajes().reactivar(idTraje)));
                acciones.getChildren().addAll(UtilidadesInterfaz.seccion("El traje está dado de baja"),
                        UtilidadesInterfaz.subtitulo("Puedes reactivarlo para que vuelva a estar DISPONIBLE."), reactivar);
            }
            default -> { }
        }

        TableView<MantenimientoFila> tabla = UtilidadesInterfaz.tabla("Sin mantenimientos registrados.");
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Tipo", MantenimientoFila::tipo, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Inicio", MantenimientoFila::fechaInicio, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Fin", MantenimientoFila::fechaFin, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Costo", MantenimientoFila::costo, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Detalle", MantenimientoFila::detalle, 300));
        tabla.getItems().setAll(s.trajes().mantenimientos(idTraje));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return new VBox(12, UtilidadesInterfaz.panel(acciones), UtilidadesInterfaz.seccion("Historial de mantenimiento"), tabla);
    }

    // ------------------------------------------------------------------ historial

    private VBox historial() {
        TableView<HistorialTraje> tabla = UtilidadesInterfaz.tabla("Este traje todavía no se ha alquilado.");
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("N°", HistorialTraje::idAlquiler, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", HistorialTraje::cliente, 250));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Salida", HistorialTraje::fechaSalida, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", HistorialTraje::fechaDevolucionPrevista, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Precio", HistorialTraje::precio, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colEstado("Estado", HistorialTraje::estado, 120));
        tabla.getItems().setAll(s.trajes().historial(idTraje));
        UtilidadesInterfaz.alDobleClic(tabla, h -> {
            new AlquilerDetalleDialogo(s, h.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return new VBox(10, UtilidadesInterfaz.subtitulo("Doble clic para abrir el alquiler."), tabla);
    }
}
