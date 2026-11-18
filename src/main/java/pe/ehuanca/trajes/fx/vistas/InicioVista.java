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

    /** Qué hace el botón Reservar de cada tarjeta (la tienda abre Nueva reserva; el cliente envía una solicitud). */
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

    private final Servicios s;
    private final AccionReservar accion;
    /** true: panel de la tienda (conjunto + próximos movimientos); false: portal del cliente. */
    private final boolean tienda;
    /** Tienda: abre la pantalla Solicitudes (clic en el indicador). */
    private final Runnable verSolicitudes;

    // buscador
    private final ToggleGroup modalidad = new ToggleGroup();
    private final ComboBox<Opcion> danza;
    private final ComboBox<Opcion> conjunto;
    private final DatePicker recojo = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(1));
    private final DatePicker devolucion = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(4));
    private final Label duracion = new Label();

    // resultados
    private final Label ubicacion = new Label();
    private final Label tituloResultados = new Label();
    private final Label subResultados = UtilidadesInterfaz.subtitulo("");
    private final ToggleGroup filtroDanza = new ToggleGroup();
    private final FlowPane etiquetasDanza = new FlowPane(8, 8);
    private final ComboBox<String> orden = UtilidadesInterfaz.comboTexto(
            List.of(ORDEN_RECOMENDADO, ORDEN_MENOR, ORDEN_MAYOR, ORDEN_POPULAR), null);
    private final TarjetaTraje.Cuadricula cuadricula = new TarjetaTraje.Cuadricula();

    // próximos movimientos
    private final HBox tarjetas = new HBox(18);
    private final TableView<AlquilerResumen> tabla = UtilidadesInterfaz.tabla("No hay salidas ni devoluciones pendientes.");

    // última búsqueda
    private List<TrajeFila> encontrados = List.of();
    private Set<Integer> masAlquilados = Set.of();
    private Map<Integer, LocalDate> libreDesde = Map.of();
    private final Map<Integer, List<String>> piezas = new HashMap<>();
    private LocalDate buscadoDesde;
    private LocalDate buscadoHasta;

    public static InicioVista paraTienda(Servicios s, AccionReservar accion, Runnable verSolicitudes) {
        return new InicioVista(s, accion, true, verSolicitudes);
    }

    public static InicioVista paraCliente(Servicios s, AccionReservar accion) {
        return new InicioVista(s, accion, false, null);
    }

    private InicioVista(Servicios s, AccionReservar accion, boolean tienda, Runnable verSolicitudes) {
        this.s = s;
        this.accion = accion;
        this.tienda = tienda;
        this.verSolicitudes = verSolicitudes;
        getStyleClass().add("inicio");

        List<Opcion> danzas = s.trajes().danzas();
        danza = UtilidadesInterfaz.combo(danzas, Opcion::nombre, "Todas las danzas");
        conjunto = UtilidadesInterfaz.combo(s.clientes().opcionesConjunto(), Opcion::nombre, "Sin conjunto (opcional)");

        VBox buscador = buscador();
        // La tarjeta de búsqueda se superpone al borde inferior del banner.
        VBox.setMargin(buscador, new Insets(-92, 44, 0, 44));
        VBox resultados = resultados(danzas);
        VBox.setMargin(resultados, new Insets(46, 0, 0, 0));
        getChildren().addAll(portada(), buscador, resultados);
        if (tienda) {
            VBox movimientos = movimientos();
            VBox.setMargin(movimientos, new Insets(54, 0, 0, 0));
            getChildren().add(movimientos);
        }
        actualizarDuracion();
        UtilidadesInterfaz.ejecutar(this::buscar);
        if (tienda) {
            UtilidadesInterfaz.ejecutar(this::cargarMovimientos);
        }
    }

    // ------------------------------------------------------------------ banner

    private StackPane portada() {
        Label chip = new Label("Festividad Virgen de la Candelaria  •  Disponibilidad en tiempo real",
                UtilidadesInterfaz.icono(Feather.STAR));
        chip.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-portada");

        Text t1 = new Text("Luce tu mejor traje\nen la ");
        t1.getStyleClass().addAll("portada-titulo", "portada-blanco");
        Text t2 = new Text("Candelaria");
        t2.getStyleClass().addAll("portada-titulo", "portada-dorado");
        TextFlow titulo = new TextFlow(t1, t2);
        titulo.setLineSpacing(-8);

        TextFlow parrafo = new TextFlow(
                textoNormal("Más de "), negrita("50 trajes de luces y autóctonos"),
                textoNormal(" para diablada, morenada, caporales y más. Elige tus fechas, mira qué está "),
                negrita("libre en tiempo real"), textoNormal(" y resérvalo con "), negrita("garantía devuelta"),
                textoNormal(" si el traje vuelve sin daños."));
        parrafo.setLineSpacing(4);
        parrafo.setMaxWidth(560);

        VBox textos = new VBox(20, chip, titulo, parrafo);
        textos.setMaxWidth(660);
        textos.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(textos, Pos.CENTER_LEFT);

        // Chakana y grecas doradas a la derecha (opacidad 0.33). Sin fotos de personas.
        Group chakana = Adornos.chakanaEnGrupo(196, "adorno");
        Group grecaArriba = Adornos.greca(5, 9, "adorno-linea", "adorno");
        Group grecaAbajo = Adornos.greca(5, 9, "adorno-linea", "adorno");
        grecaAbajo.setScaleY(-1);
        VBox adornos = new VBox(24, grecaArriba, chakana, grecaAbajo);
        adornos.setAlignment(Pos.CENTER);
        adornos.setOpacity(0.33);
        adornos.setMouseTransparent(true);
        adornos.setManaged(false);

        StackPane banner = new StackPane(adornos, textos);
        banner.getStyleClass().add("portada");
        Runnable ubicar = () -> {
            adornos.autosize();
            double x = banner.getWidth() - adornos.getWidth() - Math.max(40, banner.getWidth() * 0.06);
            // centrado en la parte del banner que no tapa el buscador (los 92 px de abajo)
            adornos.relocate(x, Math.max(24, (banner.getHeight() - 92 - adornos.getHeight()) / 2));
            // En ventanas angostas los adornos chocarían con el título: se ocultan.
            adornos.setVisible(x > 700);
        };
        banner.widthProperty().addListener((o, a, b) -> ubicar.run());
        banner.heightProperty().addListener((o, a, b) -> ubicar.run());
        return banner;
    }

    private static Text textoNormal(String t) {
        Text x = new Text(t);
        x.getStyleClass().add("portada-parrafo");
        return x;
    }

    private static Text negrita(String t) {
        Text x = new Text(t);
        x.getStyleClass().add("portada-negrita");
        return x;
    }

    // ------------------------------------------------------------------ buscador

    private VBox buscador() {
        ToggleButton enTienda = pestana(RECOJO_TIENDA, Feather.SHOPPING_BAG);
        ToggleButton pestanaTienda = pestana(ENTREGA_EN_TIENDA, Feather.TRUCK);
        enTienda.setSelected(true);
        modalidad.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                modalidad.selectToggle(antes);   // siempre hay una elegida
            } else {
                actualizarUbicacion();
            }
        });
        HBox pestanas = new HBox(6, enTienda, pestanaTienda);
        Region linea = new Region();
        linea.getStyleClass().add("buscador-linea");

        GridPane fila = new GridPane();
        fila.setHgap(14);
        // Anchos preferidos proporcionales + crecimiento (sin percentWidth, que infla el ancho mínimo).
        for (double a : new double[]{240, 260, 180, 180}) {
            ColumnConstraints c = new ColumnConstraints();
            c.setMinWidth(90);
            c.setPrefWidth(a);
            c.setHgrow(Priority.ALWAYS);
            fila.getColumnConstraints().add(c);
        }
        ColumnConstraints cBoton = new ColumnConstraints();
        cBoton.setMinWidth(Region.USE_PREF_SIZE);
        fila.getColumnConstraints().add(cBoton);

        Button ver = UtilidadesInterfaz.estilo(new Button("Ver disponibles", UtilidadesInterfaz.icono(Feather.SEARCH)), "boton-primario");
        ver.setPrefHeight(44);
        ver.setMinWidth(Region.USE_PREF_SIZE);
        ver.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::buscar));

        fila.add(campo("Danza", danza), 0, 0);
        if (tienda) {
            fila.add(campo("Conjunto", conjunto), 1, 0);
        } else {
            // el cliente no reserva a nombre de un conjunto: la columna queda vacía y sin ancho
            fila.getColumnConstraints().get(1).setPrefWidth(0);
            fila.getColumnConstraints().get(1).setMinWidth(0);
            fila.getColumnConstraints().get(1).setHgrow(javafx.scene.layout.Priority.NEVER);
            fila.setHgap(14);
        }
        fila.add(campo("Fecha de recojo", recojo), 2, 0);
        fila.add(campo("Devolución", devolucion), 3, 0);
        VBox cajaBoton = new VBox(ver);
        cajaBoton.setAlignment(Pos.BOTTOM_LEFT);
        fila.add(cajaBoton, 4, 0);

        recojo.valueProperty().addListener((o, a, b) -> actualizarDuracion());
        devolucion.valueProperty().addListener((o, a, b) -> actualizarDuracion());

        Label nota = new Label("Se excluyen los trajes en mantenimiento y los solicitados, reservados o entregados en esas "
                + "fechas. Conjuntos que alquilan 5 trajes o más: 10% de descuento.");
        nota.getStyleClass().add("nota");
        nota.setWrapText(true);
        nota.setMinHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(nota, Priority.ALWAYS);
        duracion.setMinWidth(Region.USE_PREF_SIZE);
        HBox pie = new HBox(14, duracion, nota);
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox caja = new VBox(pestanas, linea, fila, pie);
        caja.getStyleClass().add("buscador");
        return caja;
    }

    private ToggleButton pestana(String texto, Feather ikon) {
        ToggleButton b = new ToggleButton(texto, UtilidadesInterfaz.icono(ikon));
        b.getStyleClass().add("pestana");
        b.setToggleGroup(modalidad);
        return b;
    }

    private static VBox campo(String titulo, Region control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.setMaxWidth(Double.MAX_VALUE);
        control.setMinWidth(90);   // que el nombre más largo de la lista no ensanche la página
        control.setPrefWidth(120);
        return new VBox(7, l, control);
    }

    private static long dias(LocalDate a, LocalDate b) {
        return Math.max(1, ChronoUnit.DAYS.between(a, b));
    }

    private static String textoDias(long n) {
        return n + (n == 1 ? " día" : " días");
    }

    private void actualizarDuracion() {
        LocalDate a = recojo.getValue();
        LocalDate b = devolucion.getValue();
        duracion.getStyleClass().removeAll("etiqueta-opcion-verde", "etiqueta-opcion-rojo");
        if (!duracion.getStyleClass().contains("etiqueta-opcion")) {
            duracion.getStyleClass().add("etiqueta-opcion");
        }
        if (a == null || b == null || b.isBefore(a)) {
            duracion.setText(a == null || b == null ? "Elige las fechas" : "La devolución es antes del recojo");
            duracion.setGraphic(UtilidadesInterfaz.icono(Feather.ALERT_CIRCLE));
            duracion.getStyleClass().add("etiqueta-opcion-rojo");
        } else {
            duracion.setText("Duración: " + textoDias(dias(a, b)));
            duracion.setGraphic(UtilidadesInterfaz.icono(Feather.CLOCK));
            duracion.getStyleClass().add("etiqueta-opcion-verde");
        }
    }

    // ------------------------------------------------------------------ resultados

    private VBox resultados(List<Opcion> danzas) {
        ubicacion.getStyleClass().add("sobretitulo");
        ubicacion.setGraphic(UtilidadesInterfaz.icono(Feather.MAP_PIN));
        ubicacion.setGraphicTextGap(8);
        actualizarUbicacion();
        tituloResultados.getStyleClass().add("resultados-titulo");
        subResultados.setMinHeight(Region.USE_PREF_SIZE);

        etiquetasDanza.getChildren().add(etiquetaFiltro("Todas", null));
        for (Opcion d : danzas) {
            etiquetasDanza.getChildren().add(etiquetaFiltro(d.nombre(), d));
        }
        ((ToggleButton) etiquetasDanza.getChildren().get(0)).setSelected(true);
        filtroDanza.selectedToggleProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                filtroDanza.selectToggle(antes);
            } else {
                pintar();
            }
        });
        HBox.setHgrow(etiquetasDanza, Priority.ALWAYS);

        orden.setValue(ORDEN_RECOMENDADO);
        orden.setPrefWidth(200);
        orden.setMinWidth(200);
        orden.setOnAction(e -> pintar());
        Label lOrden = UtilidadesInterfaz.etiqueta("Ordenar por");
        lOrden.setMinWidth(Region.USE_PREF_SIZE);
        HBox ordenCaja = new HBox(10, lOrden, orden);
        ordenCaja.setAlignment(Pos.CENTER_RIGHT);
        ordenCaja.setMinWidth(Region.USE_PREF_SIZE);
        HBox filtros = new HBox(20, etiquetasDanza, ordenCaja);
        filtros.setAlignment(Pos.TOP_LEFT);

        VBox caja = new VBox(8, ubicacion, tituloResultados, subResultados, filtros, cuadricula);
        VBox.setMargin(filtros, new Insets(10, 0, 8, 0));
        caja.getStyleClass().add("inicio-seccion");
        return caja;
    }

    private ToggleButton etiquetaFiltro(String texto, Opcion d) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("etiqueta-opcion-filtro");
        b.setUserData(d);
        b.setToggleGroup(filtroDanza);
        return b;
    }

    private boolean esEntregaEnTienda() {
        return modalidad.getSelectedToggle() != null
                && ENTREGA_EN_TIENDA.equals(((ToggleButton) modalidad.getSelectedToggle()).getText());
    }

    private void actualizarUbicacion() {
        ubicacion.setText(UtilidadesInterfaz.espaciado("Juliaca – Tienda principal") + "  •  "
                + UtilidadesInterfaz.espaciado(esEntregaEnTienda() ? "Entrega en local del conjunto" : "Recojo en tienda"));
    }

    /** Busca con la función fn_trajes_disponibles de la base y prepara los datos de las tarjetas. */
    private void buscar() {
        LocalDate desde = UtilidadesInterfaz.requerido(recojo, "Fecha de recojo");
        LocalDate hasta = UtilidadesInterfaz.requerido(devolucion, "Devolución");
        if (hasta.isBefore(desde)) {
            throw new ExcepcionReglaNegocio("La fecha de devolución no puede ser anterior a la de recojo.");
        }
        Opcion d = danza.getValue();
        Set<String> codigos = new HashSet<>(
                s.alquileres().codigosDisponibles(desde, hasta, d == null ? null : d.nombre()));

        List<TrajeFila> inventario = s.trajes().listar(null, null, null, null);
        encontrados = inventario.stream().filter(t -> codigos.contains(t.codigo())).toList();
        masAlquilados = new HashSet<>();
        inventario.stream()
                .filter(t -> t.vecesAlquilado() > 0 && !"BAJA".equals(t.estado()))
                .sorted(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed())
                .limit(CANTIDAD_MAS_ALQUILADOS)
                .forEach(t -> masAlquilados.add(t.idTraje()));
        libreDesde = TarjetaTraje.proximaFechaLibre(s.alquileres().ocupaciones(), LocalDate.now());
        buscadoDesde = desde;
        buscadoHasta = hasta;

        // El chip de danza sigue a la danza buscada.
        for (Node n : etiquetasDanza.getChildren()) {
            Opcion o = (Opcion) n.getUserData();
            if ((d == null && o == null) || (d != null && o != null && o.id().equals(d.id()))) {
                ((ToggleButton) n).setSelected(true);
            }
        }
        pintar();
    }

    private void pintar() {
        if (buscadoDesde == null) {
            return;
        }
        Opcion filtro = filtroDanza.getSelectedToggle() == null ? null
                : (Opcion) filtroDanza.getSelectedToggle().getUserData();
        List<TrajeFila> lista = new ArrayList<>(encontrados.stream()
                .filter(t -> filtro == null || t.danza().equals(filtro.nombre())).toList());

        Comparator<TrajeFila> porPrecio = Comparator.comparing(TrajeFila::precioAlquiler);
        switch (orden.getValue() == null ? ORDEN_RECOMENDADO : orden.getValue()) {
            case ORDEN_MENOR -> lista.sort(porPrecio);
            case ORDEN_MAYOR -> lista.sort(porPrecio.reversed());
            case ORDEN_POPULAR -> lista.sort(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed());
            default -> lista.sort(Comparator.comparing((TrajeFila t) -> !masAlquilados.contains(t.idTraje()))
                    .thenComparing(t -> !listoHoy(t))
                    .thenComparing(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed()));
        }

        long n = lista.size();
        long dias = dias(buscadoDesde, buscadoHasta);
        tituloResultados.setText(n + (n == 1 ? " traje disponible" : " trajes disponibles") + " para la Candelaria");
        subResultados.setText("Libres del " + UtilidadesInterfaz.fecha(buscadoDesde) + " al " + UtilidadesInterfaz.fecha(buscadoHasta) + " · "
                + textoDias(dias) + ". El precio es por todo el periodo; la garantía se paga aparte y se devuelve.");

        if (lista.isEmpty()) {
            VBox caja = new VBox(UtilidadesInterfaz.seccion("No hay trajes libres con esos filtros"),
                    UtilidadesInterfaz.subtitulo("Prueba con otras fechas u otra danza."));
            caja.getStyleClass().add("vacio-catalogo");
            cuadricula.mostrar(List.of(caja));
            return;
        }
        String sufijo = "/ " + textoDias(dias);
        List<Node> tarjetasTrajes = new ArrayList<>();
        for (TrajeFila t : lista) {
            boolean hoy = listoHoy(t);
            LocalDate libre = libreDesde.getOrDefault(t.idTraje(), LocalDate.now());
            tarjetasTrajes.add(TarjetaTraje.crear(new TarjetaTraje.Datos(t, masAlquilados.contains(t.idTraje()),
                    hoy ? "Listo hoy en tienda" : "Disponible desde " + libre.format(DIA_MES),
                    hoy ? TarjetaTraje.Punto.VERDE : TarjetaTraje.Punto.AMBAR,
                    piezas(t.idTraje()), TarjetaTraje.tallas(t, encontrados), sufijo, true,
                    () -> reservar(t),
                    // el cliente abre el detalle del portal con un clic; la tienda, el diálogo con doble clic
                    tienda ? () -> new TrajeDetalleDialogo(s, t.idTraje()).mostrar() : () -> reservar(t),
                    tienda ? "Reservar" : "Ver traje", !tienda)));
        }
        cuadricula.mostrar(tarjetasTrajes);
    }

    private boolean listoHoy(TrajeFila t) {
        return "DISPONIBLE".equals(t.estado())
                && !libreDesde.getOrDefault(t.idTraje(), LocalDate.now()).isAfter(LocalDate.now());
    }

    private List<String> piezas(int idTraje) {
        return piezas.computeIfAbsent(idTraje,
                id -> s.trajes().piezas(id).stream().map(PiezaFila::tipo).toList());
    }

    private void reservar(TrajeFila t) {
        accion.reservar(t, buscadoDesde, buscadoHasta, tienda ? conjunto.getValue() : null,
                esEntregaEnTienda() ? "Entrega en local del conjunto" : null);
    }

    // ------------------------------------------------------------------ próximos movimientos

    private VBox movimientos() {
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("N°", AlquilerResumen::idAlquiler, 60));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Movimiento",
                a -> switch (a.estado()) {
                    case "SOLICITADO" -> "Por aprobar";
                    case "RESERVADO" -> "Sale";
                    default -> "Debe volver";
                }, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colFecha("Fecha",
                a -> "ENTREGADO".equals(a.estado()) ? a.fechaDevolucionPrevista() : a.fechaSalida(), 100));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Cliente", AlquilerResumen::cliente, 230));
        tabla.getColumns().add(UtilidadesInterfaz.colTexto("Conjunto", AlquilerResumen::conjunto, 180));
        tabla.getColumns().add(UtilidadesInterfaz.colNumero("Trajes", AlquilerResumen::numTrajes, 70));
        tabla.getColumns().add(UtilidadesInterfaz.colSoles("Saldo", AlquilerResumen::saldo, 110));
        tabla.getColumns().add(UtilidadesInterfaz.colEstadoAlquiler(200));
        UtilidadesInterfaz.alDobleClic(tabla, a -> {
            new AlquilerDetalleDialogo(s, a.idAlquiler()).mostrar();
            UtilidadesInterfaz.ejecutar(this::cargarMovimientos);
        });
        tabla.setPrefHeight(440);
        tabla.setMinHeight(320);

        Button actualizar = new Button("Actualizar", UtilidadesInterfaz.icono(Feather.REFRESH_CW));
        actualizar.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarMovimientos));
        Label titulo = new Label("Próximos movimientos");
        titulo.getStyleClass().add("resultados-titulo");
        VBox titulos = new VBox(6, UtilidadesInterfaz.sobretitulo("Agenda de la tienda"), titulo,
                UtilidadesInterfaz.subtitulo("Solicitudes de clientes por aprobar, reservas que salen en los próximos 30 días "
                        + "y alquileres en curso. Doble clic para ver el detalle."));
        HBox cabecera = new HBox(10, titulos, UtilidadesInterfaz.espacio(), actualizar);
        cabecera.setAlignment(Pos.BOTTOM_LEFT);

        VBox caja = new VBox(18, cabecera, tarjetas, tabla);
        caja.getStyleClass().add("inicio-seccion");
        return caja;
    }

    private void cargarMovimientos() {
        Resumen r = s.alquileres().resumen();
        List<AlquilerResumen> proximos = s.alquileres().proximosMovimientos();
        LocalDate hoy = LocalDate.now();
        long devolucionesHoy = proximos.stream()
                .filter(a -> "ENTREGADO".equals(a.estado()) && hoy.equals(a.fechaDevolucionPrevista()))
                .count();
        int solicitudes = s.alquileres().contarSolicitudes();
        javafx.scene.layout.HBox tSolicitudes = UtilidadesInterfaz.tarjeta("Solicitudes pendientes", String.valueOf(solicitudes),
                solicitudes > 0 ? "granate" : "gris", Feather.INBOX);
        if (verSolicitudes != null) {
            tSolicitudes.getStyleClass().add("tarjeta-clic");
            tSolicitudes.setOnMouseClicked(e -> verSolicitudes.run());
        }
        tarjetas.getChildren().setAll(tSolicitudes,
                UtilidadesInterfaz.tarjeta("Trajes disponibles", String.valueOf(r.disponibles()), "verde", Feather.CHECK_CIRCLE),
                UtilidadesInterfaz.tarjeta("Alquileres activos", String.valueOf(r.fuera()), "azul", Feather.SHOPPING_BAG),
                UtilidadesInterfaz.tarjeta("Devoluciones para hoy", String.valueOf(devolucionesHoy), "naranja", Feather.ROTATE_CCW),
                UtilidadesInterfaz.tarjeta("Ingresos del mes",
                        UtilidadesInterfaz.soles(r.ingresosMes() == null ? BigDecimal.ZERO : r.ingresosMes()), "dorado",
                        Feather.TRENDING_UP));
        tabla.getItems().setAll(proximos);
    }
}
