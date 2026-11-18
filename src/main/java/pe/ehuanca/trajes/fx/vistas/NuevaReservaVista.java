/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.fx.VistaPanelVendedor;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.TrajeDisponible;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nueva reserva: elegir cliente y fechas, buscar trajes libres, marcarlos y registrar.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class NuevaReservaVista extends VBox {

    /** Mismo descuento que AlquilerServicio: 10% para conjuntos con 5 trajes o más. */
    private static final BigDecimal DESCUENTO_CONJUNTO = new BigDecimal("0.90");
    private static final int MINIMO_DESCUENTO = 5;

    private final Servicios sErht;
    private final VistaPanelVendedor ventanaErht;

    private final ComboBox<Opcion> clienteErht;
    private final ComboBox<Opcion> conjuntoErht;
    private final DatePicker salidaErht = UtilidadesInterfaz.selectorFecha(LocalDate.now());
    private final DatePicker devolucionErht = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(3));
    private final ComboBox<Opcion> danzaErht;
    private final ComboBox<String> tallaErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, "Todas");

    private final TableView<TrajeDisponible> tablaErht = UtilidadesInterfaz.tabla("Elige las fechas y pulsa \"Buscar trajes libres\".");
    private final Map<Integer, BooleanProperty> marcadosErht = new LinkedHashMap<>();

    private final Label lCantidadErht = new Label();
    private final Label lSubtotalErht = new Label();
    private final Label lDescuentoErht = new Label();
    private final Label lTotalErht = new Label();
    private final Label lGarantiaErht = new Label();

    private final TextField adelantoErht = UtilidadesInterfaz.campo("", "0.00");
    private final ComboBox<String> metodoErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
    private final TextField observacionErht = UtilidadesInterfaz.campo("", "Opcional");

    /** Fechas con las que se hizo la última búsqueda. */
    private LocalDate buscadoDesdeErht;
    private LocalDate buscadoHastaErht;

    public NuevaReservaVista(Servicios s, VistaPanelVendedor ventana) {
        this.sErht = s;
        this.ventanaErht = ventana;
        getStyleClass().add("contenido");

        clienteErht = UtilidadesInterfaz.combo(s.clientes().opciones(), Opcion::nombre, null);
        clienteErht.getSelectionModel().clearSelection();
        clienteErht.setPromptText("Elige un cliente...");
        conjuntoErht = UtilidadesInterfaz.combo(s.clientes().opcionesConjunto(), Opcion::nombre, "— Sin conjunto —");
        danzaErht = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, "Todas");
        conjuntoErht.setOnAction(e -> recalcularErht());
        // Si cambian las fechas, la lista de trajes libres ya no vale.
        salidaErht.valueProperty().addListener((o, a, b) -> limpiarResultadosErht());
        devolucionErht.valueProperty().addListener((o, a, b) -> limpiarResultadosErht());

        getChildren().addAll(UtilidadesInterfaz.encabezado("Reservas", "Nueva reserva",
                        "Solo se muestran los trajes libres en el rango de fechas elegido."),
                datosErht(), tablaErht(), totalesErht(), pagoErht());
        recalcularErht();
    }

    private VBox datosErht() {
        GridPane g = UtilidadesInterfaz.formulario();
        ColumnConstraints etiquetaErht = new ColumnConstraints();
        etiquetaErht.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints controlErht = new ColumnConstraints();
        controlErht.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(etiquetaErht, controlErht);
        UtilidadesInterfaz.fila(g, 0, "Cliente *", clienteErht);
        UtilidadesInterfaz.fila(g, 1, "Conjunto", conjuntoErht);
        g.add(UtilidadesInterfaz.etiqueta("Salida *"), 2, 0);
        g.add(salidaErht, 3, 0);
        g.add(UtilidadesInterfaz.etiqueta("Devolución *"), 2, 1);
        g.add(devolucionErht, 3, 1);

        Button buscarErht = UtilidadesInterfaz.estilo(new Button("Buscar trajes libres"), "boton-primario");
        buscarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::buscarErht));
        HBox filtrosErht = new HBox(10, UtilidadesInterfaz.etiqueta("Danza"), danzaErht, UtilidadesInterfaz.etiqueta("Talla"), tallaErht, UtilidadesInterfaz.espacio(), buscarErht);
        danzaErht.setPrefWidth(240);
        tallaErht.setPrefWidth(100);
        filtrosErht.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(g, filtrosErht);
    }

    private TableView<TrajeDisponible> tablaErht() {
        tablaErht.setEditable(true);
        TableColumn<TrajeDisponible, Boolean> marcaErht = new TableColumn<>("✔");
        marcaErht.setCellValueFactory(c -> marcaErht(c.getValue()));
        marcaErht.setCellFactory(CheckBoxTableCell.forTableColumn(marcaErht));
        marcaErht.setEditable(true);
        marcaErht.setSortable(false);
        marcaErht.setPrefWidth(45);
        marcaErht.setMaxWidth(45);

        tablaErht.getColumns().add(marcaErht);
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Código", TrajeDisponible::codigo, 90));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Danza", TrajeDisponible::danza, 170));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Personaje", TrajeDisponible::personaje, 190));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Género", TrajeDisponible::genero, 80));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Talla", TrajeDisponible::talla, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Color", TrajeDisponible::color, 120));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Precio", TrajeDisponible::precioAlquiler, 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Garantía", TrajeDisponible::garantia, 100));
        // Doble clic en la fila también marca o desmarca el traje.
        UtilidadesInterfaz.alDobleClic(tablaErht, t -> marcaErht(t).set(!marcaErht(t).get()));
        tablaErht.setMinHeight(200);
        VBox.setVgrow(tablaErht, Priority.ALWAYS);
        return tablaErht;
    }

    private HBox totalesErht() {
        lTotalErht.getStyleClass().add("total-grande");
        lDescuentoErht.getStyleClass().add("descuento");
        CheckBox todosErht = new CheckBox("Marcar todos");
        todosErht.setOnAction(e -> tablaErht.getItems().forEach(t -> marcaErht(t).set(todosErht.isSelected())));
        HBox cajaErht = new HBox(24, todosErht, lCantidadErht, lSubtotalErht, lDescuentoErht, UtilidadesInterfaz.espacio(), lGarantiaErht, lTotalErht);
        cajaErht.getStyleClass().add("totales");
        return cajaErht;
    }

    private HBox pagoErht() {
        adelantoErht.setPrefWidth(110);
        metodoErht.setPrefWidth(160);
        HBox.setHgrow(observacionErht, Priority.ALWAYS);
        Button registrarErht = UtilidadesInterfaz.estilo(new Button("Registrar reserva"), "boton-primario");
        registrarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::registrarErht));
        HBox cajaErht = new HBox(10, UtilidadesInterfaz.etiqueta("Adelanto S/"), adelantoErht, UtilidadesInterfaz.etiqueta("Método"), metodoErht,
                UtilidadesInterfaz.etiqueta("Observación"), observacionErht, registrarErht);
        cajaErht.setAlignment(Pos.CENTER_LEFT);
        cajaErht.getStyleClass().add("barra-filtros");
        return cajaErht;
    }

    /**
     * Viene de Inicio o del Catálogo: pone las fechas (y el conjunto y la observación si llegan),
     * busca los trajes libres y deja marcado el elegido.
     */
    public void preparar(int idTraje, LocalDate desde, LocalDate hasta, Opcion conjuntoElegido, String nota) {
        if (conjuntoElegido != null) {
            conjuntoErht.getItems().stream().filter(o -> o != null && o.id().equals(conjuntoElegido.id()))
                    .findFirst().ifPresent(conjuntoErht::setValue);
        }
        if (nota != null && !nota.isBlank()) {
            observacionErht.setText(nota);
        }
        salidaErht.setValue(desde);
        devolucionErht.setValue(hasta);
        buscarErht();
        tablaErht.getItems().stream().filter(t -> t.idTraje() == idTraje).findFirst().ifPresent(t -> {
            marcaErht(t).set(true);
            tablaErht.getSelectionModel().select(t);
            tablaErht.scrollTo(t);
        });
    }

    // ------------------------------------------------------------------ lógica

    private void buscarErht() {
        LocalDate desdeErht = UtilidadesInterfaz.requerido(salidaErht, "Salida");
        LocalDate hastaErht = UtilidadesInterfaz.requerido(devolucionErht, "Devolución");
        if (hastaErht.isBefore(desdeErht)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de salida.");
        }
        Opcion d = danzaErht.getValue();
        List<TrajeDisponible> libresErht = sErht.alquileres().disponibles(desdeErht, hastaErht, d == null ? null : d.id(), tallaErht.getValue());
        marcadosErht.clear();
        tablaErht.getItems().setAll(libresErht);
        tablaErht.setPlaceholder(new Label("No hay trajes libres con esos filtros en esas fechas."));
        buscadoDesdeErht = desdeErht;
        buscadoHastaErht = hastaErht;
        recalcularErht();
    }

    private void limpiarResultadosErht() {
        if (buscadoDesdeErht == null) {
            return;
        }
        buscadoDesdeErht = null;
        buscadoHastaErht = null;
        marcadosErht.clear();
        tablaErht.getItems().clear();
        tablaErht.setPlaceholder(new Label("Cambiaron las fechas: pulsa \"Buscar trajes libres\" de nuevo."));
        recalcularErht();
    }

    /** Casilla de un traje (se crea la primera vez y recalcula los totales al cambiar). */
    private BooleanProperty marcaErht(TrajeDisponible t) {
        return marcadosErht.computeIfAbsent(t.idTraje(), id -> {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, a, b) -> recalcularErht());
            return p;
        });
    }

    private List<TrajeDisponible> seleccionadosErht() {
        List<TrajeDisponible> listaErht = new ArrayList<>();
        for (TrajeDisponible t : tablaErht.getItems()) {
            if (marcaErht(t).get()) {
                listaErht.add(t);
            }
        }
        return listaErht;
    }

    /** Recalcula los totales en vivo (mismo redondeo que el INSERT del servicio). */
    private void recalcularErht() {
        List<TrajeDisponible> selErht = seleccionadosErht();
        boolean descuentoErht = conjuntoErht.getValue() != null && selErht.size() >= MINIMO_DESCUENTO;
        BigDecimal subtotalErht = BigDecimal.ZERO;
        BigDecimal totalErht = BigDecimal.ZERO;
        BigDecimal garantiaErht = BigDecimal.ZERO;
        // Descuento por conjunto traje por traje; la garantía se suma sin descuento.
        for (TrajeDisponible t : selErht) {
            subtotalErht = subtotalErht.add(t.precioAlquiler());
            totalErht = totalErht.add(descuentoErht
                    ? t.precioAlquiler().multiply(DESCUENTO_CONJUNTO).setScale(2, RoundingMode.HALF_UP)
                    : t.precioAlquiler());
            garantiaErht = garantiaErht.add(t.garantia());
        }
        lCantidadErht.setText(selErht.size() + " traje(s) marcado(s)");
        lSubtotalErht.setText("Subtotal: " + UtilidadesInterfaz.soles(subtotalErht));
        if (descuentoErht) {
            lDescuentoErht.setText("Descuento conjunto 10%: −" + UtilidadesInterfaz.soles(subtotalErht.subtract(totalErht)));
        } else if (conjuntoErht.getValue() != null) {
            lDescuentoErht.setText("10% de descuento desde " + MINIMO_DESCUENTO + " trajes");
        } else {
            lDescuentoErht.setText("");
        }
        lGarantiaErht.setText("Garantía: " + UtilidadesInterfaz.soles(garantiaErht));
        lTotalErht.setText("Total alquiler: " + UtilidadesInterfaz.soles(totalErht));
    }

    private void registrarErht() {
        Opcion c = UtilidadesInterfaz.requerido(clienteErht, "Cliente");
        if (buscadoDesdeErht == null) {
            throw new ExcepcionReglaNegocio("Primero pulsa \"Buscar trajes libres\" y marca los trajes.");
        }
        List<Integer> idsErht = seleccionadosErht().stream().map(TrajeDisponible::idTraje).toList();
        if (idsErht.isEmpty()) {
            throw new ExcepcionReglaNegocio("Selecciona al menos un traje.");
        }
        Opcion cj = conjuntoErht.getValue();
        int id = sErht.alquileres().crear(c.id(), cj == null ? null : cj.id(), buscadoDesdeErht, buscadoHastaErht,
                idsErht, UtilidadesInterfaz.monto(adelantoErht.getText()), metodoErht.getValue(), observacionErht.getText());

        UtilidadesInterfaz.mostrarInformacion("Reserva N° " + id + " registrada con " + idsErht.size() + " traje(s).");
        new AlquilerDetalleDialogo(sErht, id).mostrar();
        ventanaErht.mostrar(VistaPanelVendedor.ALQUILERES);
    }
}
