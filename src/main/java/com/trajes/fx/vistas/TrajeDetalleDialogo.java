package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.HistorialTraje;
import com.trajes.modelo.MantenimientoFila;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.PiezaFila;
import com.trajes.modelo.Traje;
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

/** Detalle de un traje: piezas, estado y mantenimiento, e historial de alquileres. */
public class TrajeDetalleDialogo {

    private final Servicios s;
    private final int idTraje;
    private final BorderPane raiz = new BorderPane();
    private int pestaniaActual;
    private Stage stage;

    public TrajeDetalleDialogo(Servicios s, int idTraje) {
        this.s = s;
        this.idTraje = idTraje;
    }

    public void mostrar() {
        if (!Ui.ejecutar(this::cargar)) {
            return;
        }
        stage = Ui.ventana("Detalle del traje", raiz, 980, 700);
        stage.showAndWait();
    }

    private void cargar() {
        Traje t = s.trajes().buscar(idTraje);

        HBox linea = new HBox(12, Ui.titulo(t.codigo() + " · " + t.personaje()), Ui.badge(t.estado()));
        linea.setAlignment(Pos.CENTER_LEFT);
        Button editar = new Button("Editar datos");
        editar.setOnAction(e -> {
            new TrajeFormularioDialogo(s, idTraje).mostrar();
            Ui.ejecutar(this::cargar);
        });
        HBox arriba = new HBox(linea, Ui.espacio(), editar);
        arriba.setAlignment(Pos.CENTER_LEFT);
        VBox cabecera = new VBox(4, arriba,
                Ui.subtitulo(t.danza() + "   ·   " + t.genero() + "   ·   talla " + t.talla()
                        + (t.color() != null ? "   ·   " + t.color() : "")),
                Ui.subtitulo("Alquiler: " + Ui.soles(t.precioAlquiler()) + "   ·   Garantía: " + Ui.soles(t.garantia())
                        + "   ·   Adquirido: " + Ui.fecha(t.fechaAdquisicion())));
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
        cerrar.setOnAction(e -> stage.close());
        HBox pie = new HBox(cerrar);
        pie.getStyleClass().add("barra-acciones");
        raiz.setBottom(pie);
    }

    /** Ejecuta una acción y recarga el diálogo. */
    private void hacer(Ui.Accion accion) {
        Ui.ejecutar(() -> {
            accion.ejecutar();
            cargar();
        });
    }

    // ------------------------------------------------------------------ piezas

    private VBox piezas() {
        TableView<PiezaFila> tabla = Ui.tabla("Este traje no tiene piezas registradas.");
        tabla.getColumns().add(Ui.colTexto("Pieza", PiezaFila::tipo, 160));
        tabla.getColumns().add(Ui.colTexto("Descripción", PiezaFila::descripcion, 300));
        tabla.getColumns().add(Ui.colEstado("Estado", PiezaFila::estado, 110));
        tabla.getColumns().add(Ui.colSoles("Valor de reposición", PiezaFila::valorReposicion, 150));
        tabla.getItems().setAll(s.trajes().piezas(idTraje));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button quitar = Ui.estilo(new Button("Quitar pieza seleccionada"), "boton-peligro");
        quitar.setOnAction(e -> {
            PiezaFila p = tabla.getSelectionModel().getSelectedItem();
            if (p == null) {
                Ui.info("Selecciona una pieza de la tabla.");
            } else if (Ui.confirmar("¿Quitar la pieza \"" + p.tipo() + "\" de este traje?")) {
                hacer(() -> s.trajes().eliminarPieza(p.idPieza()));
            }
        });

        List<Opcion> tipos = s.trajes().tiposPieza();
        ComboBox<Opcion> tipo = Ui.combo(tipos, Opcion::nombre, null);
        tipo.setPrefWidth(180);
        TextField descripcion = Ui.campo("", "Descripción (opcional)");
        HBox.setHgrow(descripcion, Priority.ALWAYS);
        TextField valor = Ui.campo("", "Valor S/");
        valor.setPrefWidth(110);
        Button agregar = Ui.estilo(new Button("Agregar pieza"), "boton-primario");
        agregar.setOnAction(e -> hacer(() -> s.trajes().agregarPieza(idTraje,
                Ui.requerido(tipo, "Tipo de pieza").id(), descripcion.getText(),
                Ui.monto(Ui.requerido(valor, "Valor de reposición")))));

        HBox formulario = new HBox(10, Ui.etiqueta("Tipo"), tipo, descripcion, Ui.etiqueta("Reposición"), valor, agregar);
        formulario.setAlignment(Pos.CENTER_LEFT);
        HBox acciones = new HBox(quitar);
        return new VBox(10, tabla, acciones, Ui.seccion("Agregar pieza"), formulario);
    }

    // ------------------------------------------------------------------ estado

    private VBox estado(Traje t) {
        VBox acciones = new VBox(10);
        switch (t.estado()) {
            case "DISPONIBLE" -> {
                ComboBox<String> tipo = Ui.comboTexto(List.of("LAVADO", "REPARACION", "BORDADO"), null);
                tipo.setPrefWidth(150);
                TextField detalle = Ui.campo("", "Detalle (opcional)");
                HBox.setHgrow(detalle, Priority.ALWAYS);
                Button enviar = Ui.estilo(new Button("Enviar a mantenimiento"), "boton-dorado");
                enviar.setOnAction(e -> hacer(() ->
                        s.trajes().enviarAMantenimiento(idTraje, tipo.getValue(), detalle.getText())));
                HBox fila = new HBox(10, Ui.etiqueta("Tipo"), tipo, detalle, enviar);
                fila.setAlignment(Pos.CENTER_LEFT);

                Button baja = Ui.estilo(new Button("Dar de baja"), "boton-peligro");
                baja.setOnAction(e -> {
                    if (Ui.confirmar("¿Dar de baja el traje " + t.codigo() + "? Ya no se podrá alquilar.")) {
                        hacer(() -> s.trajes().darDeBaja(idTraje));
                    }
                });
                acciones.getChildren().addAll(Ui.seccion("Enviar a lavado o reparación"),
                        Ui.subtitulo("Mientras esté en mantenimiento el traje no se puede alquilar."), fila,
                        Ui.seccion("Retirar del inventario"), baja);
            }
            case "MANTENIMIENTO" -> {
                TextField costo = Ui.campo("", "0.00");
                costo.setPrefWidth(120);
                Button terminar = Ui.estilo(new Button("Terminar mantenimiento"), "boton-primario");
                terminar.setOnAction(e -> hacer(() ->
                        s.trajes().terminarMantenimiento(idTraje, Ui.monto(costo.getText()))));
                HBox fila = new HBox(10, Ui.etiqueta("Costo S/"), costo, terminar);
                fila.setAlignment(Pos.CENTER_LEFT);
                acciones.getChildren().addAll(Ui.seccion("El traje está en mantenimiento"),
                        Ui.subtitulo("Al terminar se registra el costo y el traje vuelve a estar DISPONIBLE."), fila);
            }
            case "BAJA" -> {
                Button reactivar = Ui.estilo(new Button("Reactivar traje"), "boton-primario");
                reactivar.setOnAction(e -> hacer(() -> s.trajes().reactivar(idTraje)));
                acciones.getChildren().addAll(Ui.seccion("El traje está dado de baja"),
                        Ui.subtitulo("Puedes reactivarlo para que vuelva a estar DISPONIBLE."), reactivar);
            }
            default -> { }
        }

        TableView<MantenimientoFila> tabla = Ui.tabla("Sin mantenimientos registrados.");
        tabla.getColumns().add(Ui.colTexto("Tipo", MantenimientoFila::tipo, 110));
        tabla.getColumns().add(Ui.colFecha("Inicio", MantenimientoFila::fechaInicio, 100));
        tabla.getColumns().add(Ui.colFecha("Fin", MantenimientoFila::fechaFin, 100));
        tabla.getColumns().add(Ui.colSoles("Costo", MantenimientoFila::costo, 100));
        tabla.getColumns().add(Ui.colTexto("Detalle", MantenimientoFila::detalle, 300));
        tabla.getItems().setAll(s.trajes().mantenimientos(idTraje));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return new VBox(12, Ui.panel(acciones), Ui.seccion("Historial de mantenimiento"), tabla);
    }

    // ------------------------------------------------------------------ historial

    private VBox historial() {
        TableView<HistorialTraje> tabla = Ui.tabla("Este traje todavía no se ha alquilado.");
        tabla.getColumns().add(Ui.colNumero("N°", HistorialTraje::idAlquiler, 60));
        tabla.getColumns().add(Ui.colTexto("Cliente", HistorialTraje::cliente, 250));
        tabla.getColumns().add(Ui.colFecha("Salida", HistorialTraje::fechaSalida, 100));
        tabla.getColumns().add(Ui.colFecha("Devolución", HistorialTraje::fechaDevolucionPrevista, 100));
        tabla.getColumns().add(Ui.colSoles("Precio", HistorialTraje::precio, 110));
        tabla.getColumns().add(Ui.colEstado("Estado", HistorialTraje::estado, 120));
        tabla.getItems().setAll(s.trajes().historial(idTraje));
        Ui.alDobleClic(tabla, h -> {
            new AlquilerDetalleDialogo(s, h.idAlquiler()).mostrar();
            Ui.ejecutar(this::cargar);
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return new VBox(10, Ui.subtitulo("Doble clic para abrir el alquiler."), tabla);
    }
}
