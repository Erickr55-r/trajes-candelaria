/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.fx.VistaPanelVendedor;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.PiezaFila;
import pe.ehuanca.trajes.modelo.TrajeFila;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo completo (todos los trajes activos) en tarjetas, con su estado de hoy:
 * listo en tienda, disponible desde una fecha o en mantenimiento.
 * Fotos: src/main/resources/img/trajes/&lt;id_traje&gt;.png (si no hay, se dibuja un placeholder).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class CatalogoVista extends VBox {

    private static final int CANTIDAD_MAS_ALQUILADOS = 3;
    private static final int DIAS_POR_DEFECTO = 3;
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private static final String ORDEN_RECOMENDADO = "Recomendados";
    private static final String ORDEN_MENOR = "Precio menor";
    private static final String ORDEN_MAYOR = "Precio mayor";
    private static final String ORDEN_POPULAR = "Más alquilados";

    private static final String EST_HOY = "Listo hoy en tienda";
    private static final String EST_LUEGO = "Disponible más adelante";
    private static final String EST_MANT = "En mantenimiento";

    private enum Estado { HOY, LUEGO, MANTENIMIENTO }

    private final Servicios sErht;
    /** Botón de la tarjeta: la tienda abre Nueva reserva; el cliente, el detalle del traje. */
    private final java.util.function.BiConsumer<TrajeFila, LocalDate> alReservarErht;
    /** Clic en la tarjeta: la tienda abre el diálogo del traje (doble clic); el cliente, el detalle (un clic). */
    private final java.util.function.Consumer<TrajeFila> alAbrirErht;
    private final boolean tiendaErht;

    private final ComboBox<Opcion> fDanzaErht;
    private final ComboBox<String> fTallaErht = UtilidadesInterfaz.comboTexto(UtilidadesInterfaz.TALLAS, "Todas las tallas");
    private final ComboBox<String> fEstadoErht = UtilidadesInterfaz.comboTexto(List.of(EST_HOY, EST_LUEGO, EST_MANT), "Todos los estados");
    private final TextField fTextoErht = UtilidadesInterfaz.campo("", "Código, personaje o color");
    private final ComboBox<String> ordenErht = UtilidadesInterfaz.comboTexto(
            List.of(ORDEN_RECOMENDADO, ORDEN_MENOR, ORDEN_MAYOR, ORDEN_POPULAR), null);

    private final Label tituloErht = UtilidadesInterfaz.titulo("");
    private final TarjetaTraje.Cuadricula cuadriculaErht = new TarjetaTraje.Cuadricula();
    private final Map<Integer, List<String>> piezasErht = new HashMap<>();

    private List<TrajeFila> listaErht = List.of();
    private List<TrajeFila> todosErht = List.of();
    private Set<Integer> masAlquiladosErht = Set.of();
    private Map<Integer, LocalDate> libreDesdeErht = Map.of();

    public static CatalogoVista paraTienda(Servicios s, VistaPanelVendedor ventana) {
        return new CatalogoVista(s, (t, libre) -> {
            LocalDate desdeErht = libre.isBefore(LocalDate.now()) ? LocalDate.now() : libre;
            ventana.reservar(t.idTraje(), desdeErht, desdeErht.plusDays(DIAS_POR_DEFECTO), null, null);
        }, t -> new TrajeDetalleDialogo(s, t.idTraje()).mostrar(), true);
    }

    public static CatalogoVista paraCliente(Servicios s, java.util.function.Consumer<TrajeFila> verTraje) {
        return new CatalogoVista(s, (t, libre) -> verTraje.accept(t), verTraje, false);
    }

    private CatalogoVista(Servicios s, java.util.function.BiConsumer<TrajeFila, LocalDate> alReservar,
                          java.util.function.Consumer<TrajeFila> alAbrir, boolean tienda) {
        this.sErht = s;
        this.alReservarErht = alReservar;
        this.alAbrirErht = alAbrir;
        this.tiendaErht = tienda;
        getStyleClass().add("contenido");
        fDanzaErht = UtilidadesInterfaz.combo(s.trajes().danzas(), Opcion::nombre, "Todas las danzas");

        getChildren().addAll(cabeceraErht(), filtrosErht(), cuadriculaErht);
        VBox.setMargin(cuadriculaErht, new Insets(6, 0, 10, 0));
        UtilidadesInterfaz.ejecutar(this::cargarErht);
    }

    private HBox cabeceraErht() {
        Label subErht = UtilidadesInterfaz.subtitulo(tiendaErht
                ? "Todo el vestuario de trajes de luces y autóctonos, con su estado de hoy. "
                        + "Doble clic en una tarjeta para ver piezas, mantenimiento e historial."
                : "Todo nuestro vestuario de trajes de luces y autóctonos. Elige uno para ver sus piezas, "
                        + "las fechas libres y solicitar tu reserva.");
        subErht.setMinHeight(Region.USE_PREF_SIZE);
        VBox textosErht = new VBox(6, UtilidadesInterfaz.sobretitulo("Juliaca – Tienda principal", Feather.MAP_PIN), tituloErht, subErht);
        HBox.setHgrow(textosErht, Priority.ALWAYS);

        ordenErht.setValue(ORDEN_RECOMENDADO);
        ordenErht.setPrefWidth(200);
        ordenErht.setMinWidth(200);
        ordenErht.setOnAction(e -> pintarErht());
        Label lOrdenErht = UtilidadesInterfaz.etiqueta("Ordenar por");
        lOrdenErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox accionesErht = new HBox(10, lOrdenErht, ordenErht);
        accionesErht.setAlignment(Pos.CENTER_RIGHT);
        accionesErht.setMinWidth(Region.USE_PREF_SIZE);

        HBox cajaErht = new HBox(16, textosErht, accionesErht);
        cajaErht.setAlignment(Pos.BOTTOM_LEFT);
        return cajaErht;
    }

    private HBox filtrosErht() {
        fDanzaErht.setPrefWidth(210);
        fTallaErht.setPrefWidth(160);
        fEstadoErht.setPrefWidth(220);
        HBox.setHgrow(fTextoErht, Priority.ALWAYS);
        Button aplicarErht = UtilidadesInterfaz.estilo(new Button("Filtrar", UtilidadesInterfaz.icono(Feather.SLIDERS)), "boton-primario");
        aplicarErht.setMinWidth(Region.USE_PREF_SIZE);
        aplicarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        fTextoErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::cargarErht));
        fEstadoErht.setOnAction(e -> pintarErht());
        Button limpiarErht = new Button("Limpiar");
        limpiarErht.setMinWidth(Region.USE_PREF_SIZE);
        limpiarErht.setOnAction(e -> {
            fDanzaErht.getSelectionModel().selectFirst();
            fTallaErht.getSelectionModel().selectFirst();
            fEstadoErht.getSelectionModel().selectFirst();
            fTextoErht.clear();
            UtilidadesInterfaz.ejecutar(this::cargarErht);
        });
        HBox barraErht = new HBox(12, fDanzaErht, fTallaErht, fEstadoErht, fTextoErht, limpiarErht, aplicarErht);
        barraErht.getStyleClass().add("barra-filtros");
        return barraErht;
    }

    // ------------------------------------------------------------------ datos

    private void cargarErht() {
        Opcion d = fDanzaErht.getValue();
        todosErht = sErht.trajes().listar(null, null, null, null);
        listaErht = sErht.trajes().listar(d == null ? null : d.id(), null, fTallaErht.getValue(), fTextoErht.getText())
                .stream().filter(t -> !"BAJA".equals(t.estado())).toList();
        masAlquiladosErht = new HashSet<>();
        todosErht.stream()
                .filter(t -> t.vecesAlquilado() > 0 && !"BAJA".equals(t.estado()))
                .sorted(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed())
                .limit(CANTIDAD_MAS_ALQUILADOS)
                .forEach(t -> masAlquiladosErht.add(t.idTraje()));
        libreDesdeErht = TarjetaTraje.proximaFechaLibre(sErht.alquileres().ocupaciones(), LocalDate.now());
        pintarErht();
    }

    private Estado estadoErht(TrajeFila t) {
        if ("MANTENIMIENTO".equals(t.estado())) {
            return Estado.MANTENIMIENTO;
        }
        return libreDesdeErht.getOrDefault(t.idTraje(), LocalDate.now()).isAfter(LocalDate.now())
                ? Estado.LUEGO : Estado.HOY;
    }

    private void pintarErht() {
        List<TrajeFila> visiblesErht = new ArrayList<>(listaErht);
        if (fEstadoErht.getValue() != null) {
            Estado buscadoErht = switch (fEstadoErht.getValue()) {
                case EST_HOY -> Estado.HOY;
                case EST_LUEGO -> Estado.LUEGO;
                default -> Estado.MANTENIMIENTO;
            };
            visiblesErht.removeIf(t -> estadoErht(t) != buscadoErht);
        }
        Comparator<TrajeFila> porPrecioErht = Comparator.comparing(TrajeFila::precioAlquiler);
        switch (ordenErht.getValue() == null ? ORDEN_RECOMENDADO : ordenErht.getValue()) {
            case ORDEN_MENOR -> visiblesErht.sort(porPrecioErht);
            case ORDEN_MAYOR -> visiblesErht.sort(porPrecioErht.reversed());
            case ORDEN_POPULAR -> visiblesErht.sort(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed());
            default -> visiblesErht.sort(Comparator.comparing((TrajeFila t) -> estadoErht(t).ordinal())
                    .thenComparing(t -> !masAlquiladosErht.contains(t.idTraje())));
        }

        long hoyErht = visiblesErht.stream().filter(t -> estadoErht(t) == Estado.HOY).count();
        tituloErht.setText(visiblesErht.size() + " trajes en catálogo · " + hoyErht + " listos hoy");

        if (visiblesErht.isEmpty()) {
            VBox cajaErht = new VBox(UtilidadesInterfaz.seccion("No hay trajes con esos filtros"),
                    UtilidadesInterfaz.subtitulo("Cambia o limpia los filtros."));
            cajaErht.getStyleClass().add("vacio-catalogo");
            cuadriculaErht.mostrar(List.of(cajaErht));
            return;
        }
        List<Node> tarjetasErht = new ArrayList<>();
        for (TrajeFila t : visiblesErht) {
            Estado e = estadoErht(t);
            LocalDate libreErht = libreDesdeErht.getOrDefault(t.idTraje(), LocalDate.now());
            String textoErht = switch (e) {
                case HOY -> EST_HOY;
                case LUEGO -> "Disponible desde " + libreErht.format(DIA_MES);
                case MANTENIMIENTO -> EST_MANT;
            };
            TarjetaTraje.Punto puntoErht = switch (e) {
                case HOY -> TarjetaTraje.Punto.VERDE;
                case LUEGO -> TarjetaTraje.Punto.AMBAR;
                case MANTENIMIENTO -> TarjetaTraje.Punto.ROJO;
            };
            tarjetasErht.add(TarjetaTraje.crear(new TarjetaTraje.Datos(t, masAlquiladosErht.contains(t.idTraje()), textoErht,
                    puntoErht, piezasErht(t.idTraje()), TarjetaTraje.tallas(t, todosErht), "/ alquiler",
                    e != Estado.MANTENIMIENTO, () -> alReservarErht.accept(t, libreErht), () -> alAbrirErht.accept(t),
                    tiendaErht ? "Reservar" : "Ver traje", !tiendaErht)));
        }
        cuadriculaErht.mostrar(tarjetasErht);
    }

    private List<String> piezasErht(int idTraje) {
        return piezasErht.computeIfAbsent(idTraje,
                id -> sErht.trajes().piezas(id).stream().map(PiezaFila::tipo).toList());
    }

}
