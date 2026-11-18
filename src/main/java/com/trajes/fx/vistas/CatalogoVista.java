package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.fx.PanelVendedorView;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.PiezaFila;
import com.trajes.modelo.TrajeFila;
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

public class CatalogoVista extends VBox {

    private static final int TOP_MAS_ALQUILADOS = 3;
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

    private final Servicios s;
     
    private final java.util.function.BiConsumer<TrajeFila, LocalDate> alReservar;
     
    private final java.util.function.Consumer<TrajeFila> alAbrir;
    private final boolean tienda;

    private final ComboBox<Opcion> fDanza;
    private final ComboBox<String> fTalla = Ui.comboTexto(Ui.TALLAS, "Todas las tallas");
    private final ComboBox<String> fEstado = Ui.comboTexto(List.of(EST_HOY, EST_LUEGO, EST_MANT), "Todos los estados");
    private final TextField fTexto = Ui.campo("", "Código, personaje o color");
    private final ComboBox<String> orden = Ui.comboTexto(
            List.of(ORDEN_RECOMENDADO, ORDEN_MENOR, ORDEN_MAYOR, ORDEN_POPULAR), null);

    private final Label titulo = Ui.titulo("");
    private final TarjetaTraje.Cuadricula cuadricula = new TarjetaTraje.Cuadricula();
    private final Map<Integer, List<String>> piezas = new HashMap<>();

    private List<TrajeFila> lista = List.of();
    private List<TrajeFila> todos = List.of();
    private Set<Integer> masAlquilados = Set.of();
    private Map<Integer, LocalDate> libreDesde = Map.of();

    public static CatalogoVista paraTienda(Servicios s, PanelVendedorView ventana) {
        return new CatalogoVista(s, (t, libre) -> {
            LocalDate desde = libre.isBefore(LocalDate.now()) ? LocalDate.now() : libre;
            ventana.reservar(t.idTraje(), desde, desde.plusDays(DIAS_POR_DEFECTO), null, null);
        }, t -> new TrajeDetalleDialogo(s, t.idTraje()).mostrar(), true);
    }

    public static CatalogoVista paraCliente(Servicios s, java.util.function.Consumer<TrajeFila> verTraje) {
        return new CatalogoVista(s, (t, libre) -> verTraje.accept(t), verTraje, false);
    }

    private CatalogoVista(Servicios s, java.util.function.BiConsumer<TrajeFila, LocalDate> alReservar,
                          java.util.function.Consumer<TrajeFila> alAbrir, boolean tienda) {
        this.s = s;
        this.alReservar = alReservar;
        this.alAbrir = alAbrir;
        this.tienda = tienda;
        getStyleClass().add("contenido");
        fDanza = Ui.combo(s.trajes().danzas(), Opcion::nombre, "Todas las danzas");

        getChildren().addAll(cabecera(), filtros(), cuadricula);
        VBox.setMargin(cuadricula, new Insets(6, 0, 10, 0));
        Ui.ejecutar(this::cargar);
    }

    private HBox cabecera() {
        Label sub = Ui.subtitulo(tienda
                ? "Todo el vestuario de trajes de luces y autóctonos, con su estado de hoy. "
                        + "Doble clic en una tarjeta para ver piezas, mantenimiento e historial."
                : "Todo nuestro vestuario de trajes de luces y autóctonos. Elige uno para ver sus piezas, "
                        + "las fechas libres y solicitar tu reserva.");
        sub.setMinHeight(Region.USE_PREF_SIZE);
        VBox textos = new VBox(6, Ui.sobretitulo("Juliaca – Tienda principal", Feather.MAP_PIN), titulo, sub);
        HBox.setHgrow(textos, Priority.ALWAYS);

        orden.setValue(ORDEN_RECOMENDADO);
        orden.setPrefWidth(200);
        orden.setMinWidth(200);
        orden.setOnAction(e -> pintar());
        Label lOrden = Ui.etiqueta("Ordenar por");
        lOrden.setMinWidth(Region.USE_PREF_SIZE);
        HBox acciones = new HBox(10, lOrden, orden);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        acciones.setMinWidth(Region.USE_PREF_SIZE);

        HBox caja = new HBox(16, textos, acciones);
        caja.setAlignment(Pos.BOTTOM_LEFT);
        return caja;
    }

    private HBox filtros() {
        fDanza.setPrefWidth(210);
        fTalla.setPrefWidth(160);
        fEstado.setPrefWidth(220);
        HBox.setHgrow(fTexto, Priority.ALWAYS);
        Button aplicar = Ui.estilo(new Button("Filtrar", Ui.icono(Feather.SLIDERS)), "boton-primario");
        aplicar.setMinWidth(Region.USE_PREF_SIZE);
        aplicar.setOnAction(e -> Ui.ejecutar(this::cargar));
        fTexto.setOnAction(e -> Ui.ejecutar(this::cargar));
        fEstado.setOnAction(e -> pintar());
        Button limpiar = new Button("Limpiar");
        limpiar.setMinWidth(Region.USE_PREF_SIZE);
        limpiar.setOnAction(e -> {
            fDanza.getSelectionModel().selectFirst();
            fTalla.getSelectionModel().selectFirst();
            fEstado.getSelectionModel().selectFirst();
            fTexto.clear();
            Ui.ejecutar(this::cargar);
        });
        HBox barra = new HBox(12, fDanza, fTalla, fEstado, fTexto, limpiar, aplicar);
        barra.getStyleClass().add("barra-filtros");
        return barra;
    }

    private void cargar() {
        Opcion d = fDanza.getValue();
        todos = s.trajes().listar(null, null, null, null);
        lista = s.trajes().listar(d == null ? null : d.id(), null, fTalla.getValue(), fTexto.getText())
                .stream().filter(t -> !"BAJA".equals(t.estado())).toList();
        masAlquilados = new HashSet<>();
        todos.stream()
                .filter(t -> t.vecesAlquilado() > 0 && !"BAJA".equals(t.estado()))
                .sorted(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed())
                .limit(TOP_MAS_ALQUILADOS)
                .forEach(t -> masAlquilados.add(t.idTraje()));
        libreDesde = TarjetaTraje.proximaFechaLibre(s.alquileres().ocupaciones(), LocalDate.now());
        pintar();
    }

    private Estado estado(TrajeFila t) {
        if ("MANTENIMIENTO".equals(t.estado())) {
            return Estado.MANTENIMIENTO;
        }
        return libreDesde.getOrDefault(t.idTraje(), LocalDate.now()).isAfter(LocalDate.now())
                ? Estado.LUEGO : Estado.HOY;
    }

    private void pintar() {
        List<TrajeFila> visibles = new ArrayList<>(lista);
        if (fEstado.getValue() != null) {
            Estado buscado = switch (fEstado.getValue()) {
                case EST_HOY -> Estado.HOY;
                case EST_LUEGO -> Estado.LUEGO;
                default -> Estado.MANTENIMIENTO;
            };
            visibles.removeIf(t -> estado(t) != buscado);
        }
        Comparator<TrajeFila> porPrecio = Comparator.comparing(TrajeFila::precioAlquiler);
        switch (orden.getValue() == null ? ORDEN_RECOMENDADO : orden.getValue()) {
            case ORDEN_MENOR -> visibles.sort(porPrecio);
            case ORDEN_MAYOR -> visibles.sort(porPrecio.reversed());
            case ORDEN_POPULAR -> visibles.sort(Comparator.comparingLong(TrajeFila::vecesAlquilado).reversed());
            default -> visibles.sort(Comparator.comparing((TrajeFila t) -> estado(t).ordinal())
                    .thenComparing(t -> !masAlquilados.contains(t.idTraje())));
        }

        long hoy = visibles.stream().filter(t -> estado(t) == Estado.HOY).count();
        titulo.setText(visibles.size() + " trajes en catálogo · " + hoy + " listos hoy");

        if (visibles.isEmpty()) {
            VBox caja = new VBox(Ui.seccion("No hay trajes con esos filtros"),
                    Ui.subtitulo("Cambia o limpia los filtros."));
            caja.getStyleClass().add("vacio-catalogo");
            cuadricula.mostrar(List.of(caja));
            return;
        }
        List<Node> cards = new ArrayList<>();
        for (TrajeFila t : visibles) {
            Estado e = estado(t);
            LocalDate libre = libreDesde.getOrDefault(t.idTraje(), LocalDate.now());
            String texto = switch (e) {
                case HOY -> EST_HOY;
                case LUEGO -> "Disponible desde " + libre.format(DIA_MES);
                case MANTENIMIENTO -> EST_MANT;
            };
            TarjetaTraje.Punto punto = switch (e) {
                case HOY -> TarjetaTraje.Punto.VERDE;
                case LUEGO -> TarjetaTraje.Punto.AMBAR;
                case MANTENIMIENTO -> TarjetaTraje.Punto.ROJO;
            };
            cards.add(TarjetaTraje.crear(new TarjetaTraje.Datos(t, masAlquilados.contains(t.idTraje()), texto,
                    punto, piezas(t.idTraje()), TarjetaTraje.tallas(t, todos), "/ alquiler",
                    e != Estado.MANTENIMIENTO, () -> alReservar.accept(t, libre), () -> alAbrir.accept(t),
                    tienda ? "Reservar" : "Ver traje", !tienda)));
        }
        cuadricula.mostrar(cards);
    }

    private List<String> piezas(int idTraje) {
        return piezas.computeIfAbsent(idTraje,
                id -> s.trajes().piezas(id).stream().map(PiezaFila::tipo).toList());
    }

}
