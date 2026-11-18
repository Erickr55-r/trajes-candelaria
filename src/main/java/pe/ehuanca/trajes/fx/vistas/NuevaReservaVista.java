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

    private final Servicios s;
    private final VistaPanelVendedor ventana;

    private final ComboBox<Opcion> cliente;
    private final ComboBox<Opcion> conjunto;
    private final DatePicker salida = UtilidadesInterfaz.selectorFecha(LocalDate.now());
    private final DatePicker devolucion = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(3));
    private final ComboBox<Opcion> danza;
    private final ComboBox<String> talla = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, "Todas");

    private final TableView<TrajeDisponible> tabla = UtilidadesInterfaz.tabla("Elige las fechas y pulsa \"Buscar trajes libres\".");
    private final Map<Integer, BooleanProperty> marcados = new LinkedHashMap<>();

    private final Label lCantidad = new Label();
    private final Label lSubtotal = new Label();
    private final Label lDescuento = new Label();
    private final Label lTotal = new Label();
    private final Label lGarantia = new Label();

    private final TextField adelanto = UtilidadesInterfaz.campo("", "0.00");
    private final ComboBox<String> metodo = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.METODOS, null);
    private final TextField observacion = UtilidadesInterfaz.campo("", "Opcional");

    /** Fechas con las que se hizo la última búsqueda. */
    private LocalDate buscadoDesde;
    private LocalDate buscadoHasta;

    public NuevaReservaVista(Servicios s, VistaPanelVendedor ventana) {
        this.s = s;
        this.ventana = ventana;
        getStyleClass().add("contenido");

        cliente = UtilidadesInterfaz.combo(s.clientes().opciones(), Opcion::nombre, null);
        cliente.getSelectionModel().clearSelection();
        cliente.setPromptText("Elige un cliente...");
        conjunto = UtilidadesInterfaz.combo(s.clientes().opcionesConjunto(), Opcion::nombre, "— Sin conjunto —");
        danza = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, "Todas");
        conjunto.setOnAction(e -> recalcular());
        // Si cambian las fechas, la lista de trajes libres ya no vale.
        salida.valueProperty().addListener((o, a, b) -> limpiarResultados());
        devolucion.valueProperty().addListener((o, a, b) -> limpiarResultados());

        getChildren().addAll(UtilidadesInterfaz.encabezado("Reservas", "Nueva reserva",
                        "Solo se muestran los trajes libres en el rango de fechas elegido."),
                datos(), tabla(), totales(), pago());
        recalcular();
    }

    private VBox datos() {
        GridPane g = UtilidadesInterfaz.formulario();
        ColumnConstraints etiqueta = new ColumnConstraints();
        etiqueta.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints control = new ColumnConstraints();
        control.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(etiqueta, control);
        UtilidadesInterfaz.fila(g, 0, "Cliente *", cliente);
        UtilidadesInterfaz.fila(g, 1, "Conjunto", conjunto);
        g.add(UtilidadesInterfaz.etiqueta("Salida *"), 2, 0);
        g.add(salida, 3, 0);
        g.add(UtilidadesInterfaz.etiqueta("Devolución *"), 2, 1);
        g.add(devolucion, 3, 1);

        Button buscar = UtilidadesInterfaz.estilo(new Button("Buscar trajes libres"), "boton-primario");
        buscar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::buscar));
        HBox filtros = new HBox(10, UtilidadesInterfaz.etiqueta("Danza"), danza, UtilidadesInterfaz.etiqueta("Talla"), talla, UtilidadesInterfaz.espacio(), buscar);
        danza.setPrefWidth(240);
        talla.setPrefWidth(100);
        filtros.setAlignment(Pos.CENTER_LEFT);
        return UtilidadesInterfaz.panel(g, filtros);
    }

    private TableView<TrajeDisponible> tabla() {
        tabla.setEditable(true);
        TableColumn<TrajeDisponible, Boolean> marca = new TableColumn<>("✔");
        marca.setCellValueFactory(c -> marca(c.getValue()));
        marca.setCellFactory(CheckBoxTableCell.forTableColumn(marca));
        marca.setEditable(true);
        marca.setSortable(false);
        marca.setPrefWidth(45);
        marca.setMaxWidth(45);

        tabla.getColumns().add(marca);
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Código", TrajeDisponible::codigo, 90));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Danza", TrajeDisponible::danza, 170));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Personaje", TrajeDisponible::personaje, 190));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Género", TrajeDisponible::genero, 80));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Talla", TrajeDisponible::talla, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Color", TrajeDisponible::color, 120));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Precio", TrajeDisponible::precioAlquiler, 100));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Garantía", TrajeDisponible::garantia, 100));
        // Doble clic en la fila también marca o desmarca el traje.
        UtilidadesInterfaz.alDobleClic(tabla, t -> marca(t).set(!marca(t).get()));
        tabla.setMinHeight(200);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return tabla;
    }

    private HBox totales() {
        lTotal.getStyleClass().add("total-grande");
        lDescuento.getStyleClass().add("descuento");
        CheckBox todos = new CheckBox("Marcar todos");
        todos.setOnAction(e -> tabla.getItems().forEach(t -> marca(t).set(todos.isSelected())));
        HBox caja = new HBox(24, todos, lCantidad, lSubtotal, lDescuento, UtilidadesInterfaz.espacio(), lGarantia, lTotal);
        caja.getStyleClass().add("totales");
        return caja;
    }

    private HBox pago() {
        adelanto.setPrefWidth(110);
        metodo.setPrefWidth(160);
        HBox.setHgrow(observacion, Priority.ALWAYS);
        Button registrar = UtilidadesInterfaz.estilo(new Button("Registrar reserva"), "boton-primario");
        registrar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::registrar));
        HBox caja = new HBox(10, UtilidadesInterfaz.etiqueta("Adelanto S/"), adelanto, UtilidadesInterfaz.etiqueta("Método"), metodo,
                UtilidadesInterfaz.etiqueta("Observación"), observacion, registrar);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("barra-filtros");
        return caja;
    }

    /**
     * Viene de Inicio o del Catálogo: pone las fechas (y el conjunto y la observación si llegan),
     * busca los trajes libres y deja marcado el elegido.
     */
    public void preparar(int idTraje, LocalDate desde, LocalDate hasta, Opcion conjuntoElegido, String nota) {
        if (conjuntoElegido != null) {
            conjunto.getItems().stream().filter(o -> o != null && o.id().equals(conjuntoElegido.id()))
                    .findFirst().ifPresent(conjunto::setValue);
        }
        if (nota != null && !nota.isBlank()) {
            observacion.setText(nota);
        }
        salida.setValue(desde);
        devolucion.setValue(hasta);
        buscar();
        tabla.getItems().stream().filter(t -> t.idTraje() == idTraje).findFirst().ifPresent(t -> {
            marca(t).set(true);
            tabla.getSelectionModel().select(t);
            tabla.scrollTo(t);
        });
    }

    // ------------------------------------------------------------------ lógica

    private void buscar() {
        LocalDate desde = UtilidadesInterfaz.requerido(salida, "Salida");
        LocalDate hasta = UtilidadesInterfaz.requerido(devolucion, "Devolución");
        if (hasta.isBefore(desde)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de salida.");
        }
        Opcion d = danza.getValue();
        List<TrajeDisponible> libres = s.alquileres().disponibles(desde, hasta, d == null ? null : d.id(), talla.getValue());
        marcados.clear();
        tabla.getItems().setAll(libres);
        tabla.setPlaceholder(new Label("No hay trajes libres con esos filtros en esas fechas."));
        buscadoDesde = desde;
        buscadoHasta = hasta;
        recalcular();
    }

    private void limpiarResultados() {
        if (buscadoDesde == null) {
            return;
        }
        buscadoDesde = null;
        buscadoHasta = null;
        marcados.clear();
        tabla.getItems().clear();
        tabla.setPlaceholder(new Label("Cambiaron las fechas: pulsa \"Buscar trajes libres\" de nuevo."));
        recalcular();
    }

    /** Casilla de un traje (se crea la primera vez y recalcula los totales al cambiar). */
    private BooleanProperty marca(TrajeDisponible t) {
        return marcados.computeIfAbsent(t.idTraje(), id -> {
            BooleanProperty p = new SimpleBooleanProperty(false);
            p.addListener((o, a, b) -> recalcular());
            return p;
        });
    }

    private List<TrajeDisponible> seleccionados() {
        List<TrajeDisponible> lista = new ArrayList<>();
        for (TrajeDisponible t : tabla.getItems()) {
            if (marca(t).get()) {
                lista.add(t);
            }
        }
        return lista;
    }

    /** Recalcula los totales en vivo (mismo redondeo que el INSERT del servicio). */
    private void recalcular() {
        List<TrajeDisponible> sel = seleccionados();
        boolean descuento = conjunto.getValue() != null && sel.size() >= MINIMO_DESCUENTO;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal garantia = BigDecimal.ZERO;
        // Descuento por conjunto traje por traje; la garantía se suma sin descuento.
        for (TrajeDisponible t : sel) {
            subtotal = subtotal.add(t.precioAlquiler());
            total = total.add(descuento
                    ? t.precioAlquiler().multiply(DESCUENTO_CONJUNTO).setScale(2, RoundingMode.HALF_UP)
                    : t.precioAlquiler());
            garantia = garantia.add(t.garantia());
        }
        lCantidad.setText(sel.size() + " traje(s) marcado(s)");
        lSubtotal.setText("Subtotal: " + UtilidadesInterfaz.soles(subtotal));
        if (descuento) {
            lDescuento.setText("Descuento conjunto 10%: −" + UtilidadesInterfaz.soles(subtotal.subtract(total)));
        } else if (conjunto.getValue() != null) {
            lDescuento.setText("10% de descuento desde " + MINIMO_DESCUENTO + " trajes");
        } else {
            lDescuento.setText("");
        }
        lGarantia.setText("Garantía: " + UtilidadesInterfaz.soles(garantia));
        lTotal.setText("Total alquiler: " + UtilidadesInterfaz.soles(total));
    }

    private void registrar() {
        Opcion c = UtilidadesInterfaz.requerido(cliente, "Cliente");
        if (buscadoDesde == null) {
            throw new ExcepcionReglaNegocio("Primero pulsa \"Buscar trajes libres\" y marca los trajes.");
        }
        List<Integer> ids = seleccionados().stream().map(TrajeDisponible::idTraje).toList();
        if (ids.isEmpty()) {
            throw new ExcepcionReglaNegocio("Selecciona al menos un traje.");
        }
        Opcion cj = conjunto.getValue();
        int id = s.alquileres().crear(c.id(), cj == null ? null : cj.id(), buscadoDesde, buscadoHasta,
                ids, UtilidadesInterfaz.monto(adelanto.getText()), metodo.getValue(), observacion.getText());

        UtilidadesInterfaz.mostrarInformacion("Reserva N° " + id + " registrada con " + ids.size() + " traje(s).");
        new AlquilerDetalleDialogo(s, id).mostrar();
        ventana.mostrar(VistaPanelVendedor.ALQUILERES);
    }
}
