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

    private final Servicios sErht;
    private final int idTrajeErht;
    private final BorderPane raizErht = new BorderPane();
    private int pestaniaActualErht;
    private Stage ventanaDialogoErht;

    public TrajeDetalleDialogo(Servicios s, int idTraje) {
        this.sErht = s;
        this.idTrajeErht = idTraje;
    }

    public void mostrar() {
        if (!UtilidadesInterfaz.ejecutar(this::cargarErht)) {
            return;
        }
        ventanaDialogoErht = UtilidadesInterfaz.ventana("Detalle del traje", raizErht, 980, 700);
        ventanaDialogoErht.showAndWait();
    }

    private void cargarErht() {
        Traje t = sErht.trajes().buscar(idTrajeErht);

        HBox lineaErht = new HBox(12, UtilidadesInterfaz.titulo(t.codigo() + " · " + t.personaje()), UtilidadesInterfaz.insigniaEstado(t.estado()));
        lineaErht.setAlignment(Pos.CENTER_LEFT);
        Button editarErht = new Button("Editar datos");
        editarErht.setOnAction(e -> {
            new TrajeFormularioDialogo(sErht, idTrajeErht).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox arribaErht = new HBox(lineaErht, UtilidadesInterfaz.espacio(), editarErht);
        arribaErht.setAlignment(Pos.CENTER_LEFT);
        VBox cabeceraErht = new VBox(4, arribaErht,
                UtilidadesInterfaz.subtitulo(t.danza() + "   ·   " + t.genero() + "   ·   talla " + t.talla()
                        + (t.color() != null ? "   ·   " + t.color() : "")),
                UtilidadesInterfaz.subtitulo("Alquiler: " + UtilidadesInterfaz.soles(t.precioAlquiler()) + "   ·   Garantía: " + UtilidadesInterfaz.soles(t.garantia())
                        + "   ·   Adquirido: " + UtilidadesInterfaz.fecha(t.fechaAdquisicion())));
        cabeceraErht.getStyleClass().add("cabecera");
        raizErht.setTop(cabeceraErht);

        TabPane pestaniasErht = new TabPane(
                new Tab("Piezas", piezasErht()),
                new Tab("Estado y mantenimiento", estadoErht(t)),
                new Tab("Historial de alquileres", historialErht()));
        pestaniasErht.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestaniasErht.getSelectionModel().select(pestaniaActualErht);
        pestaniasErht.getSelectionModel().selectedIndexProperty()
                .addListener((o, a, b) -> pestaniaActualErht = b.intValue());
        VBox centroErht = new VBox(pestaniasErht);
        VBox.setVgrow(pestaniasErht, Priority.ALWAYS);
        centroErht.getStyleClass().add("contenido");
        raizErht.setCenter(centroErht);

        Button cerrarErht = new Button("Cerrar");
        cerrarErht.setOnAction(e -> ventanaDialogoErht.close());
        HBox pieErht = new HBox(cerrarErht);
        pieErht.getStyleClass().add("barra-acciones");
        raizErht.setBottom(pieErht);
    }

    /** Ejecuta una acción y recarga el diálogo. */
    private void hacerErht(UtilidadesInterfaz.Accion accion) {
        UtilidadesInterfaz.ejecutar(() -> {
            accion.ejecutar();
            cargarErht();
        });
    }

    // ------------------------------------------------------------------ piezas

    private VBox piezasErht() {
        TableView<PiezaFila> tablaErht = UtilidadesInterfaz.tabla("Este traje no tiene piezas registradas.");
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Pieza", PiezaFila::tipo, 160));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Descripción", PiezaFila::descripcion, 300));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Estado", PiezaFila::estado, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Valor de reposición", PiezaFila::valorReposicion, 150));
        tablaErht.getItems().setAll(sErht.trajes().piezas(idTrajeErht));
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        Button quitarErht = UtilidadesInterfaz.estilo(new Button("Quitar pieza seleccionada"), "boton-peligro");
        quitarErht.setOnAction(e -> {
            PiezaFila p = tablaErht.getSelectionModel().getSelectedItem();
            if (p == null) {
                UtilidadesInterfaz.mostrarInformacion("Selecciona una pieza de la tabla.");
            } else if (UtilidadesInterfaz.confirmar("¿Quitar la pieza \"" + p.tipo() + "\" de este traje?")) {
                hacerErht(() -> sErht.trajes().eliminarPieza(p.idPieza()));
            }
        });

        List<Opcion> tiposErht = sErht.trajes().tiposPieza();
        ComboBox<Opcion> tipoErht = UtilidadesInterfaz.combo(tiposErht, Opcion::nombre, null);
        tipoErht.setPrefWidth(180);
        TextField descripcionErht = UtilidadesInterfaz.campo("", "Descripción (opcional)");
        HBox.setHgrow(descripcionErht, Priority.ALWAYS);
        TextField valorErht = UtilidadesInterfaz.campo("", "Valor S/");
        valorErht.setPrefWidth(110);
        Button agregarErht = UtilidadesInterfaz.estilo(new Button("Agregar pieza"), "boton-primario");
        agregarErht.setOnAction(e -> hacerErht(() -> sErht.trajes().agregarPieza(idTrajeErht,
                UtilidadesInterfaz.requerido(tipoErht, "Tipo de pieza").id(), descripcionErht.getText(),
                UtilidadesInterfaz.monto(UtilidadesInterfaz.requerido(valorErht, "Valor de reposición")))));

        HBox formularioErht = new HBox(10, UtilidadesInterfaz.etiqueta("Tipo"), tipoErht, descripcionErht, UtilidadesInterfaz.etiqueta("Reposición"), valorErht, agregarErht);
        formularioErht.setAlignment(Pos.CENTER_LEFT);
        HBox accionesErht = new HBox(quitarErht);
        return new VBox(10, tablaErht, accionesErht, UtilidadesInterfaz.seccion("Agregar pieza"), formularioErht);
    }

    // ------------------------------------------------------------------ estado

    private VBox estadoErht(Traje t) {
        VBox accionesErht = new VBox(10);
        switch (t.estado()) {
            case "DISPONIBLE" -> {
                ComboBox<String> tipoErht = UtilidadesInterfaz.comboTexto(List.of("LAVADO", "REPARACION", "BORDADO"), null);
                tipoErht.setPrefWidth(150);
                TextField detalleErht = UtilidadesInterfaz.campo("", "Detalle (opcional)");
                HBox.setHgrow(detalleErht, Priority.ALWAYS);
                Button enviarErht = UtilidadesInterfaz.estilo(new Button("Enviar a mantenimiento"), "boton-dorado");
                enviarErht.setOnAction(e -> hacerErht(() ->
                        sErht.trajes().enviarAMantenimiento(idTrajeErht, tipoErht.getValue(), detalleErht.getText())));
                HBox filaErht = new HBox(10, UtilidadesInterfaz.etiqueta("Tipo"), tipoErht, detalleErht, enviarErht);
                filaErht.setAlignment(Pos.CENTER_LEFT);

                Button bajaErht = UtilidadesInterfaz.estilo(new Button("Dar de baja"), "boton-peligro");
                bajaErht.setOnAction(e -> {
                    if (UtilidadesInterfaz.confirmar("¿Dar de baja el traje " + t.codigo() + "? Ya no se podrá alquilar.")) {
                        hacerErht(() -> sErht.trajes().darDeBaja(idTrajeErht));
                    }
                });
                accionesErht.getChildren().addAll(UtilidadesInterfaz.seccion("Enviar a lavado o reparación"),
                        UtilidadesInterfaz.subtitulo("Mientras esté en mantenimiento el traje no se puede alquilar."), filaErht,
                        UtilidadesInterfaz.seccion("Retirar del inventario"), bajaErht);
            }
            case "MANTENIMIENTO" -> {
                TextField costoErht = UtilidadesInterfaz.campo("", "0.00");
                costoErht.setPrefWidth(120);
                Button terminarErht = UtilidadesInterfaz.estilo(new Button("Terminar mantenimiento"), "boton-primario");
                terminarErht.setOnAction(e -> hacerErht(() ->
                        sErht.trajes().terminarMantenimiento(idTrajeErht, UtilidadesInterfaz.monto(costoErht.getText()))));
                HBox filaErht = new HBox(10, UtilidadesInterfaz.etiqueta("Costo S/"), costoErht, terminarErht);
                filaErht.setAlignment(Pos.CENTER_LEFT);
                accionesErht.getChildren().addAll(UtilidadesInterfaz.seccion("El traje está en mantenimiento"),
                        UtilidadesInterfaz.subtitulo("Al terminar se registra el costo y el traje vuelve a estar DISPONIBLE."), filaErht);
            }
            case "BAJA" -> {
                Button reactivarErht = UtilidadesInterfaz.estilo(new Button("Reactivar traje"), "boton-primario");
                reactivarErht.setOnAction(e -> hacerErht(() -> sErht.trajes().reactivar(idTrajeErht)));
                accionesErht.getChildren().addAll(UtilidadesInterfaz.seccion("El traje está dado de baja"),
                        UtilidadesInterfaz.subtitulo("Puedes reactivarlo para que vuelva a estar DISPONIBLE."), reactivarErht);
            }
            default -> { }
        }

        TableView<MantenimientoFila> tablaErht = UtilidadesInterfaz.tabla("Sin mantenimientos registrados.");
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Tipo", MantenimientoFila::tipo, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Inicio", MantenimientoFila::fechaInicio, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Fin", MantenimientoFila::fechaFin, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Costo", MantenimientoFila::costo, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Detalle", MantenimientoFila::detalle, 300));
        tablaErht.getItems().setAll(sErht.trajes().mantenimientos(idTrajeErht));
        VBox.setVgrow(tablaErht, Priority.ALWAYS);

        return new VBox(12, UtilidadesInterfaz.panel(accionesErht), UtilidadesInterfaz.seccion("Historial de mantenimiento"), tablaErht);
    }

    // ------------------------------------------------------------------ historial

    private VBox historialErht() {
        TableView<HistorialTraje> tablaErht = UtilidadesInterfaz.tabla("Este traje todavía no se ha alquilado.");
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("N°", HistorialTraje::idAlquiler, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", HistorialTraje::cliente, 250));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Salida", HistorialTraje::fechaSalida, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Devolución", HistorialTraje::fechaDevolucionPrevista, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Precio", HistorialTraje::precio, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstado("Estado", HistorialTraje::estado, 120));
        tablaErht.getItems().setAll(sErht.trajes().historial(idTrajeErht));
        UtilidadesInterfaz.alDobleClic(tablaErht, h -> {
            new AlquilerDetalleDialogo(sErht, h.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        VBox.setVgrow(tablaErht, Priority.ALWAYS);
        return new VBox(10, UtilidadesInterfaz.subtitulo("Doble clic para abrir el alquiler."), tablaErht);
    }
}
