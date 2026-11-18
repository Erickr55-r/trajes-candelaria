/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Adornos;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.PiezaFila;
import pe.ehuanca.trajes.modelo.Resumen;
import pe.ehuanca.trajes.modelo.TrajeFila;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inicio estilo tienda web: banner, buscador de disponibilidad (fn_trajes_disponibles), resultados en
 * tarjetas con filtro por danza y orden, y los próximos movimientos.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class InicioVista extends VBox {

    @FunctionalInterface
    public interface AccionReservar {
        void reservar(TrajeFila traje, LocalDate desde, LocalDate hasta, Opcion conjunto, String nota);
    }

    private static final String RECOJO_TIENDA = "Recojo y prueba en tienda";
    private static final String ENTREGA_EN_TIENDA = "Entrega en local del conjunto";
    private static final String ORDEN_RECOMENDADO = "Recomendados";
    private static final String ORDEN_MENOR = "Precio menor";
    private static final String ORDEN_MAYOR = "Precio mayor";
    private static final String ORDEN_POPULAR = "Más alquilados";
    private static final int CANTIDAD_MAS_ALQUILADOS = 3;
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final Servicios sErht;
    private final AccionReservar accionErht;
    private final boolean tiendaErht;
    private final Runnable verSolicitudesErht;

    private final ToggleGroup modalidadErht = new ToggleGroup();
    private final ComboBox<Opcion> danzaErht;
    private final ComboBox<Opcion> conjuntoErht;
    private final DatePicker recojoErht = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(1));
    private final DatePicker devolucionErht = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(4));
    private final Label duracionErht = new Label();

    private final Label ubicacionErht = new Label();
    private final Label tituloResultadosErht = new Label();
    private final Label subResultadosErht = UtilidadesInterfaz.subtitulo("");
    private final ToggleGroup filtroDanzaErht = new ToggleGroup();
    private final FlowPane etiquetasDanzaErht = new FlowPane(8, 8);
    private final ComboBox<String> ordenErht = UtilidadesInterfaz.comboTexto(
            List.of(ORDEN_RECOMENDADO, ORDEN_MENOR, ORDEN_MAYOR, ORDEN_POPULAR), null);
    private final TarjetaTraje.Cuadricula cuadriculaErht = new TarjetaTraje.Cuadricula();

    private final HBox tarjetasErht = new HBox(18);
    private final TableView<AlquilerResumen> tablaErht = UtilidadesInterfaz.tabla("No hay salidas ni devoluciones pendientes.");

    private List<TrajeFila> encontradosErht = List.of();
    private Set<Integer> masAlquiladosErht = Set.of();
    private Map<Integer, LocalDate> libreDesdeErht = Map.of();
    private final Map<Integer, List<String>> piezasErht = new HashMap<>();
    private LocalDate buscadoDesdeErht;
    private LocalDate buscadoHastaErht;

    public static InicioVista paraTienda(Servicios s, AccionReservar accion, Runnable verSolicitudes) {
        return new InicioVista(s, accion, true, verSolicitudes);
    }

    public static InicioVista paraCliente(Servicios s, AccionReservar accion) {
        return new InicioVista(s, accion, false, null);
    }

    private InicioVista(Servicios s, AccionReservar accion, boolean tienda, Runnable verSolicitudes) {
        this.sErht = s;
        this.accionErht = accion;
        this.tiendaErht = tienda;
        this.verSolicitudesErht = verSolicitudes;
        getStyleClass().add("inicio");

        List<Opcion> danzasErht = s.trajes().danzas();
        danzaErht = UtilidadesInterfaz.combo(danzasErht, Opcion::nombre, "Todas las danzas");
        conjuntoErht = UtilidadesInterfaz.combo(s.clientes().opcionesConjunto(), Opcion::nombre, "Sin conjunto (opcional)");

        VBox buscadorErht = buscadorErht();
        VBox.setMargin(buscadorErht, new Insets(-92, 44, 0, 44));
        VBox resultadosErht = resultadosErht(danzasErht);
        VBox.setMargin(resultadosErht, new Insets(46, 0, 0, 0));
        getChildren().addAll(portadaErht(), buscadorErht, resultadosErht);
        if (tienda) {
            VBox movimientosErht = movimientosErht();
            VBox.setMargin(movimientosErht, new Insets(54, 0, 0, 0));
            getChildren().add(movimientosErht);
        }
        actualizarDuracionErht();
        UtilidadesInterfaz.ejecutar(this::buscarErht);
        if (tienda) {
            UtilidadesInterfaz.ejecutar(this::cargarMovimientosErht);
        }
    }

    private StackPane portadaErht() {
        Label chipErht = new Label("Festividad Virgen de la Candelaria  •  Disponibilidad en tiempo real",
                UtilidadesInterfaz.icono(Feather.STAR));
        chipErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-portada");

        Text t1 = new Text("Luce tu mejor traje\nen la ");
        t1.getStyleClass().addAll("portada-titulo", "portada-blanco");
        Text t2 = new Text("Candelaria");
        t2.getStyleClass().addAll("portada-titulo", "portada-dorado");
        TextFlow tituloErht = new TextFlow(t1, t2);
        tituloErht.setLineSpacing(-8);

        TextFlow parrafoErht = new TextFlow(
                textoNormalErht("Más de "), negritaErht("50 trajes de luces y autóctonos"),
                textoNormalErht(" para diablada, morenada, caporales y más. Elige tus fechas, mira qué está "),
                negritaErht("libre en tiempo real"), textoNormalErht(" y resérvalo con "), negritaErht("garantía devuelta"),
                textoNormalErht(" si el traje vuelve sin daños."));
        parrafoErht.setLineSpacing(4);
        parrafoErht.setMaxWidth(560);

        VBox textosErht = new VBox(20, chipErht, tituloErht, parrafoErht);
        textosErht.setMaxWidth(660);
        textosErht.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(textosErht, Pos.CENTER_LEFT);

        Group chakanaErht = Adornos.chakanaEnGrupo(196, "adorno");
        Group grecaArribaErht = Adornos.greca(5, 9, "adorno-linea", "adorno");
        Group grecaAbajoErht = Adornos.greca(5, 9, "adorno-linea", "adorno");
        grecaAbajoErht.setScaleY(-1);
        VBox adornosErht = new VBox(24, grecaArribaErht, chakanaErht, grecaAbajoErht);
        adornosErht.setAlignment(Pos.CENTER);
        adornosErht.setOpacity(0.33);
        adornosErht.setMouseTransparent(true);
        adornosErht.setManaged(false);

        StackPane bannerErht = new StackPane(adornosErht, textosErht);
        bannerErht.getStyleClass().add("portada");
        Runnable ubicarErht = () -> {
            adornosErht.autosize();
            double x = bannerErht.getWidth() - adornosErht.getWidth() - Math.max(40, bannerErht.getWidth() * 0.06);
            adornosErht.relocate(x, Math.max(24, (bannerErht.getHeight() - 92 - adornosErht.getHeight()) / 2));
            adornosErht.setVisible(x > 700);
        };
        bannerErht.widthProperty().addListener((o, a, b) -> ubicarErht.run());
        bannerErht.heightProperty().addListener((o, a, b) -> ubicarErht.run());
        return bannerErht;
    }

    private static Text textoNormalErht(String t) {
        Text x = new Text(t);
        x.getStyleClass().add("portada-parrafo");
        return x;
    }

    private static Text negritaErht(String t) {
        Text x = new Text(t);
        x.getStyleClass().add("portada-negrita");
        return x;
    }

    private VBox buscadorErht() {
        ToggleButton enTiendaErht = pestanaErht(RECOJO_TIENDA, Feather.SHOPPING_BAG);
        ToggleButton pestanaTiendaErht = pestanaErht(ENTREGA_EN_TIENDA, Feather.TRUCK);
        enTiendaErht.setSelected(true);
        modalidadErht.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                modalidadErht.selectToggle(antes);
            } else {
                actualizarUbicacionErht();
            }
        });
        HBox pestanasErht = new HBox(6, enTiendaErht, pestanaTiendaErht);
        Region lineaErht = new Region();
        lineaErht.getStyleClass().add("buscador-linea");

        GridPane filaErht = new GridPane();
        filaErht.setHgap(14);
        for (double a : new double[]{240, 260, 180, 180}) {
            ColumnConstraints c = new ColumnConstraints();
            c.setMinWidth(90);
            c.setPrefWidth(a);
            c.setHgrow(Priority.ALWAYS);
            filaErht.getColumnConstraints().add(c);
        }
        ColumnConstraints cBotonErht = new ColumnConstraints();
        cBotonErht.setMinWidth(Region.USE_PREF_SIZE);
        filaErht.getColumnConstraints().add(cBotonErht);

        Button verErht = UtilidadesInterfaz.estilo(new Button("Ver disponibles", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        verErht.setPrefHeight(44);
        verErht.setMinWidth(Region.USE_PREF_SIZE);
        verErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::buscarErht));

        filaErht.add(campoErht("Danza", danzaErht), 0, 0);
        if (tiendaErht) {
            filaErht.add(campoErht("Conjunto", conjuntoErht), 1, 0);
        } else {
            filaErht.getColumnConstraints().get(1).setPrefWidth(0);
            filaErht.getColumnConstraints().get(1).setMinWidth(0);
            filaErht.getColumnConstraints().get(1).setHgrow(javafx.scene.layout.Priority.NEVER);
            filaErht.setHgap(14);
        }
        filaErht.add(campoErht("Fecha de recojo", recojoErht), 2, 0);
        filaErht.add(campoErht("Devolución", devolucionErht), 3, 0);
        VBox cajaBotonErht = new VBox(verErht);
        cajaBotonErht.setAlignment(Pos.BOTTOM_LEFT);
        filaErht.add(cajaBotonErht, 4, 0);

        recojoErht.valueProperty().addListener((o, a, b) -> actualizarDuracionErht());
        devolucionErht.valueProperty().addListener((o, a, b) -> actualizarDuracionErht());

        Label notaErht = new Label("Se excluyen los trajes en mantenimiento y los solicitados, reservados o entregados en esas "
                + "fechas. Conjuntos que alquilan 5 trajes o más: 10% de descuento.");
        notaErht.getStyleClass().add("nota");
        notaErht.setWrapText(true);
        notaErht.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(notaErht, Priority.ALWAYS);
        duracionErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox pieErht = new HBox(14, duracionErht, notaErht);
        pieErht.setAlignment(Pos.CENTER_LEFT);

        VBox cajaErht = new VBox(pestanasErht, lineaErht, filaErht, pieErht);
        cajaErht.getStyleClass().add("buscador");
        return cajaErht;
    }

    private ToggleButton pestanaErht(String texto, Feather ikon) {
        ToggleButton b = new ToggleButton(texto, UtilidadesInterfaz.icono(ikon));
        b.getStyleClass().add("pestana");
        b.setToggleGroup(modalidadErht);
        return b;
    }

    private static VBox campoErht(String titulo, Region control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.setMaxWidth(Double.MAX_VALUE);
        control.setMinWidth(90);
        control.setPrefWidth(120);
        return new VBox(7, l, control);
    }

    private static long diasErht(LocalDate a, LocalDate b) {
        return Math.max(1, ChronoUnit.DAYS.between(a, b));
    }

    private static String textoDiasErht(long n) {
        return n + (n == 1 ? " día" : " días");
    }

    private void actualizarDuracionErht() {
        LocalDate a = recojoErht.getValue();
        LocalDate b = devolucionErht.getValue();
        duracionErht.getStyleClass().removeAll("etiqueta-opcion-verde", "etiqueta-opcion-rojo");
        if (!duracionErht.getStyleClass().contains("etiqueta-opcion")) {
            duracionErht.getStyleClass().add("etiqueta-opcion");
        }
        if (a == null || b == null || b.isBefore(a)) {
            duracionErht.setText(a == null || b == null ? "Elige las fechas" : "La devolución es antes del recojo");
            duracionErht.setGraphic(UtilidadesInterfaz.icono(Feather.ALERT_CIRCLE));
            duracionErht.getStyleClass().add("etiqueta-opcion-rojo");
        } else {
            duracionErht.setText("Duración: " + textoDiasErht(diasErht(a, b)));
            duracionErht.setGraphic(UtilidadesInterfaz.icono(Feather.CLOCK));
            duracionErht.getStyleClass().add("etiqueta-opcion-verde");
        }
    }

    private VBox resultadosErht(List<Opcion> danzas) {
        ubicacionErht.getStyleClass().add("sobretitulo");
        ubicacionErht.setGraphic(UtilidadesInterfaz.icono(Feather.MAP_PIN));
        ubicacionErht.setGraphicTextGap(8);
        actualizarUbicacionErht();
        tituloResultadosErht.getStyleClass().add("resultados-titulo");
        subResultadosErht.setMinHeight(Region.USE_PREF_SIZE);

        etiquetasDanzaErht.getChildren().add(etiquetaFiltroErht("Todas", null));
        for (Opcion d : danzas) {
            etiquetasDanzaErht.getChildren().add(etiquetaFiltroErht(d.nombre(), d));
        }
        ((ToggleButton) etiquetasDanzaErht.getChildren().get(0)).setSelected(true);
        filtroDanzaErht.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                filtroDanzaErht.selectToggle(antes);
            } else {
                pintarErht();
            }
        });
        HBox.setHgrow(etiquetasDanzaErht, Priority.ALWAYS);

        ordenErht.setValue(ORDEN_RECOMENDADO);
        ordenErht.setPrefWidth(200);
        ordenErht.setMinWidth(200);
        ordenErht.setOnAction(e -> pintarErht());
        Label lOrdenErht = UtilidadesInterfaz.etiqueta("Ordenar por");
        lOrdenErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox ordenCajaErht = new HBox(10, lOrdenErht, ordenErht);
        ordenCajaErht.setAlignment(Pos.CENTER_RIGHT);
        ordenCajaErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox filtrosErht = new HBox(20, etiquetasDanzaErht, ordenCajaErht);
        filtrosErht.setAlignment(Pos.TOP_LEFT);

        VBox cajaErht = new VBox(8, ubicacionErht, tituloResultadosErht, subResultadosErht, filtrosErht, cuadriculaErht);
        VBox.setMargin(filtrosErht, new Insets(10, 0, 8, 0));
        cajaErht.getStyleClass().add("inicio-seccion");
        return cajaErht;
    }

    private ToggleButton etiquetaFiltroErht(String texto, Opcion d) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("etiqueta-opcion-filtro");
        b.setUserData(d);
        b.setToggleGroup(filtroDanzaErht);
        return b;
    }

    private boolean esEntregaEnTiendaErht() {
        return modalidadErht.getSelectedToggle() != null
                && ENTREGA_EN_TIENDA.equals(((ToggleButton) modalidadErht.getSelectedToggle()).getText());
    }

    private void actualizarUbicacionErht() {
        ubicacionErht.setText(UtilidadesInterfaz.espaciado("Juliaca – Tienda principal") + "  •  "
                + UtilidadesInterfaz.espaciado(esEntregaEnTiendaErht() ? "Entrega en local del conjunto" : "Recojo en tienda"));
    }

    private void buscarErht() {
        LocalDate desdeErht = UtilidadesInterfaz.requerido(recojoErht, "Fecha de recojo");
        LocalDate hastaErht = UtilidadesInterfaz.requerido(devolucionErht, "Devolución");
        if (hastaErht.isBefore(desdeErht)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de recojo.");
        }
        Opcion d = danzaErht.getValue();
        Set<String> codigosErht = new HashSet<>(
                sErht.alquileres().codigosDisponibles(desdeErht, hastaErht, d == null ? null : d.nombre()));

        List<TrajeFila> inventarioErht = sErht.trajes().listar(null, null, null, null);
        encontradosErht = inventarioErht.stream().filter(t -> codigosErht.contains(t.codigo())).toList();
        masAlquiladosErht = new HashSet<>();
        inventarioErht.stream()
                .filter(t -> t.vecesAlquilado() > 0 && !"BAJA".equals(t.estado()))
                .sorted(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed())
                .limit(CANTIDAD_MAS_ALQUILADOS)
                .forEach(t -> masAlquiladosErht.add(t.idTraje()));
        libreDesdeErht = TarjetaTraje.proximaFechaLibre(sErht.alquileres().ocupaciones(), LocalDate.now());
        buscadoDesdeErht = desdeErht;
        buscadoHastaErht = hastaErht;

        for (Node n : etiquetasDanzaErht.getChildren()) {
            Opcion o = (Opcion) n.getUserData();
            if ((d == null && o == null) || (d != null && o != null && o.id().equals(d.id()))) {
                ((ToggleButton) n).setSelected(true);
            }
        }
        pintarErht();
    }

    private void pintarErht() {
        if (buscadoDesdeErht == null) {
            return;
        }
        Opcion filtroErht = filtroDanzaErht.getSelectedToggle() == null ? null
                : (Opcion) filtroDanzaErht.getSelectedToggle().getUserData();
        List<TrajeFila> listaErht = new ArrayList<>(encontradosErht.stream()
                .filter(t -> filtroErht == null || t.danza().equals(filtroErht.nombre())).toList());

        Comparator<TrajeFila> porPrecioErht = Comparator.comparing(TrajeFila::precioAlquiler);
        switch (ordenErht.getValue() == null ? ORDEN_RECOMENDADO : ordenErht.getValue()) {
            case ORDEN_MENOR -> listaErht.sort(porPrecioErht);
            case ORDEN_MAYOR -> listaErht.sort(porPrecioErht.reversed());
            case ORDEN_POPULAR -> listaErht.sort(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed());
            default -> listaErht.sort(Comparator.comparing((TrajeFila t) -> !masAlquiladosErht.contains(t.idTraje()))
                    .thenComparing(t -> !listoHoyErht(t))
                    .thenComparing(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed()));
        }

        long n = listaErht.size();
        long diasErht = diasErht(buscadoDesdeErht, buscadoHastaErht);
        tituloResultadosErht.setText(n + (n == 1 ? " traje disponible" : " trajes disponibles") + " para la Candelaria");
        subResultadosErht.setText("Libres del " + UtilidadesInterfaz.fecha(buscadoDesdeErht) + " al " + UtilidadesInterfaz.fecha(buscadoHastaErht) + " · "
                + textoDiasErht(diasErht) + ". El precio es por todo el periodo; la garantía se paga aparte y se devuelve.");

        if (listaErht.isEmpty()) {
            VBox cajaErht = new VBox(UtilidadesInterfaz.seccion("No hay trajes libres con esos filtros"),
                    UtilidadesInterfaz.subtitulo("Prueba con otras fechas u otra danza."));
            cajaErht.getStyleClass().add("vacio-catalogo");
            cuadriculaErht.mostrar(List.of(cajaErht));
            return;
        }
        String sufijoErht = "/ " + textoDiasErht(diasErht);
        List<Node> tarjetasTrajesErht = new ArrayList<>();
        for (TrajeFila t : listaErht) {
            boolean hoyErht = listoHoyErht(t);
            LocalDate libreErht = libreDesdeErht.getOrDefault(t.idTraje(), LocalDate.now());
            tarjetasTrajesErht.add(TarjetaTraje.crear(new TarjetaTraje.Datos(t, masAlquiladosErht.contains(t.idTraje()),
                    hoyErht ? "Listo hoy en tienda" : "Disponible desde " + libreErht.format(DIA_MES),
                    hoyErht ? TarjetaTraje.Punto.VERDE : TarjetaTraje.Punto.AMBAR,
                    piezasErht(t.idTraje()), TarjetaTraje.tallas(t, encontradosErht), sufijoErht, true,
                    () -> reservarErht(t),
                    tiendaErht ? () -> new TrajeDetalleDialogo(sErht, t.idTraje()).mostrar() : () -> reservarErht(t),
                    tiendaErht ? "Reservar" : "Ver traje", !tiendaErht)));
        }
        cuadriculaErht.mostrar(tarjetasTrajesErht);
    }

    private boolean listoHoyErht(TrajeFila t) {
        return "DISPONIBLE".equals(t.estado())
                && !libreDesdeErht.getOrDefault(t.idTraje(), LocalDate.now()).isAfter(LocalDate.now());
    }

    private List<String> piezasErht(int idTraje) {
        return piezasErht.computeIfAbsent(idTraje,
                id -> sErht.trajes().piezas(id).stream().map(PiezaFila::tipo).toList());
    }

    private void reservarErht(TrajeFila t) {
        accionErht.reservar(t, buscadoDesdeErht, buscadoHastaErht, tiendaErht ? conjuntoErht.getValue() : null,
                esEntregaEnTiendaErht() ? "Entrega en local del conjunto" : null);
    }

    private VBox movimientosErht() {
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Movimiento",
                a -> switch (a.estado()) {
                    case "SOLICITADO" -> "Por aprobar";
                    case "RESERVADO" -> "Sale";
                    default -> "Debe volver";
                }, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colFecha("Fecha",
                a -> "ENTREGADO".equals(a.estado()) ? a.fechaDevolucionPrevista() : a.fechaSalida(), 100));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", AlquilerResumen::cliente, 230));
        tablaErht.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 180));
        tablaErht.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 70));
        tablaErht.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 110));
        tablaErht.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(200));
        UtilidadesInterfaz.alDobleClic(tablaErht, a -> {
            new AlquilerDetalleDialogo(sErht, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarMovimientosErht);
        });
        tablaErht.setPrefHeight(440);
        tablaErht.setMinHeight(320);

        Button actualizarErht = new Button("Actualizar", UtilidadesInterfaz.icono(Feather.REFRESH_CW));
        actualizarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarMovimientosErht));
        Label tituloErht = new Label("Próximos movimientos");
        tituloErht.getStyleClass().add("resultados-titulo");
        VBox titulosErht = new VBox(6, UtilidadesInterfaz.sobretitulo("Agenda de la tienda"), tituloErht,
                UtilidadesInterfaz.subtitulo("Solicitudes de clientes por aprobar, reservas que salen en los próximos 30 días "
                        + "y alquileres en curso. Doble clic para ver el detalle."));
        HBox cabeceraErht = new HBox(10, titulosErht, UtilidadesInterfaz.espacio(), actualizarErht);
        cabeceraErht.setAlignment(Pos.BOTTOM_LEFT);

        VBox cajaErht = new VBox(18, cabeceraErht, tarjetasErht, tablaErht);
        cajaErht.getStyleClass().add("inicio-seccion");
        return cajaErht;
    }

    private void cargarMovimientosErht() {
        Resumen r = sErht.alquileres().resumen();
        List<AlquilerResumen> proximosErht = sErht.alquileres().proximosMovimientos();
        LocalDate hoyErht = LocalDate.now();
        long devolucionesHoyErht = proximosErht.stream()
                .filter(a -> "ENTREGADO".equals(a.estado()) && hoyErht.equals(a.fechaDevolucionPrevista()))
                .count();
        int solicitudesErht = sErht.alquileres().contarSolicitudes();
        javafx.scene.layout.HBox tSolicitudesErht = UtilidadesInterfaz.tarjeta("Solicitudes pendientes", String.valueOf(solicitudesErht),
                solicitudesErht > 0 ? "granate" : "gris", Feather.INBOX);
        if (verSolicitudesErht != null) {
            tSolicitudesErht.getStyleClass().add("tarjeta-clic");
            tSolicitudesErht.setOnMouseClicked(e -> verSolicitudesErht.run());
        }
        tarjetasErht.getChildren().setAll(tSolicitudesErht,
                UtilidadesInterfaz.tarjeta("Trajes disponibles", String.valueOf(r.disponibles()), "verde", Feather.CHECK_CIRCLE),
                UtilidadesInterfaz.tarjeta("Alquileres activos", String.valueOf(r.fuera()), "azul", Feather.SHOPPING_BAG),
                UtilidadesInterfaz.tarjeta("Devoluciones para hoy", String.valueOf(devolucionesHoyErht), "naranja", Feather.ROTATE_CCW),
                UtilidadesInterfaz.tarjeta("Ingresos del mes",
                        UtilidadesInterfaz.soles(r.ingresosMes() == null ? BigDecimal.ZERO : r.ingresosMes()), "dorado",
                        Feather.TRENDING_UP));
        tablaErht.getItems().setAll(proximosErht);
    }
}
