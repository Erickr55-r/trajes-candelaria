/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import pe.ehuanca.trajes.fx.Adornos;
import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Ocupacion;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.PiezaFila;
import pe.ehuanca.trajes.modelo.TrajeFila;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Detalle de un traje en el portal del cliente: fotos, piezas, tallas, calendario de ocupación,
 * fechas y cantidad, y una tarjeta resumen fija con el botón "Solicitar reserva" (crea el alquiler SOLICITADO).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class DetalleTrajeVista extends BorderPane {

    /** Igual que AlquilerServicio: 10% para conjuntos que alquilan 5 trajes o más. */
    private static final BigDecimal DESCUENTO_CONJUNTO = new BigDecimal("0.90");
    private static final int MINIMO_DESCUENTO = 5;
    private static final Locale ES = new Locale("es", "PE");

    private final Servicios sErht;
    private final VistaPortalCliente portalErht;
    private final TrajeFila trajeErht;
    /** Trajes del mismo modelo (misma danza, personaje, color y género), activos. */
    private final List<TrajeFila> modeloErht;
    private final Set<LocalDate> ocupadosErht = new HashSet<>();

    private final DatePicker recojoErht;
    private final DatePicker devolucionErht;
    private final Spinner<Integer> cantidadErht = new Spinner<>();
    private final ComboBox<Opcion> conjuntoErht;
    private final TextField notaErht = UtilidadesInterfaz.campo("", "Ej.: necesito probármelo antes (opcional)");
    private final Label duracionErht = new Label();
    private final Label disponiblesErht = new Label();
    private final GridPane calendarioErht = new GridPane();
    private final Label mesTituloErht = new Label();
    private YearMonth mesErht;

    // resumen
    private final Label rAlquilerTextoErht = new Label();
    private final Label rAlquilerErht = new Label();
    private final Label rDescuentoTextoErht = new Label();
    private final Label rDescuentoErht = new Label();
    private final Label rGarantiaErht = new Label();
    private final Label rTotalErht = new Label();
    private final Label rApartanErht = new Label();
    private final Label rAvisoErht = new Label();
    private final Button solicitarErht = new Button("Solicitar reserva");

    /** Trajes que se apartarían con la selección actual. */
    private List<TrajeFila> elegidosErht = List.of();

    public DetalleTrajeVista(Servicios s, VistaPortalCliente portal, TrajeFila traje, LocalDate desde, LocalDate hasta) {
        this.sErht = s;
        this.portalErht = portal;
        this.trajeErht = traje;
        getStyleClass().add("detalle");

        List<TrajeFila> todosErht = s.trajes().listar(null, null, null, null);
        modeloErht = todosErht.stream()
                .filter(t -> "DISPONIBLE".equals(t.estado()) && mismoModeloErht(t, traje))
                .sorted(Comparator.comparing((TrajeFila t) -> !t.idTraje().equals(traje.idTraje()))
                        .thenComparing(t -> UtilidadesInterfaz.TALLAS.indexOf(t.talla())))
                .toList();
        for (Ocupacion o : s.alquileres().ocupaciones()) {
            if (o.idTraje() == traje.idTraje()) {
                LocalDate finErht = "ENTREGADO".equals(o.estado()) && o.devolucionPrevista().isBefore(LocalDate.now())
                        ? LocalDate.now() : o.devolucionPrevista();
                for (LocalDate d = o.salida(); !d.isAfter(finErht); d = d.plusDays(1)) {
                    ocupadosErht.add(d);
                }
            }
        }
        LocalDate inicioErht = desde != null && !desde.isBefore(LocalDate.now()) ? desde : LocalDate.now().plusDays(1);
        recojoErht = UtilidadesInterfaz.selectorFecha(inicioErht);
        devolucionErht = UtilidadesInterfaz.selectorFecha(hasta != null && !hasta.isBefore(inicioErht) ? hasta : inicioErht.plusDays(3));
        mesErht = YearMonth.from(inicioErht);
        conjuntoErht = UtilidadesInterfaz.combo(s.clientes().conjuntosDelDelegado(Sesion.actual().idCliente()), Opcion::nombre,
                "A mi nombre (sin conjunto)");

        Hyperlink volverErht = new Hyperlink("Volver a los trajes", UtilidadesInterfaz.icono(Feather.ARROW_LEFT));
        volverErht.getStyleClass().add("enlace");
        volverErht.setOnAction(e -> portal.volver());
        HBox migasErht = new HBox(volverErht);
        migasErht.setPadding(new Insets(22, 38, 0, 38));
        setTop(migasErht);

        HBox cuerpoErht = new HBox(34, columnaFotosErht(), columnaDatosErht());
        cuerpoErht.setPadding(new Insets(18, 10, 40, 38));
        ScrollPane desplazableErht = new ScrollPane(cuerpoErht);
        desplazableErht.setFitToWidth(true);
        desplazableErht.getStyleClass().add("detalle-scroll");
        setCenter(desplazableErht);

        VBox resumenErht = resumenErht();
        BorderPane.setMargin(resumenErht, new Insets(18, 38, 30, 6));
        setRight(resumenErht);   // fija: no se mueve con el scroll

        recojoErht.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                mesErht = YearMonth.from(b);
            }
            recalcularErht();
        });
        devolucionErht.valueProperty().addListener((o, a, b) -> recalcularErht());
        cantidadErht.valueProperty().addListener((o, a, b) -> recalcularResumenErht());
        conjuntoErht.valueProperty().addListener((o, a, b) -> recalcularResumenErht());
        recalcularErht();
    }

    private static boolean mismoModeloErht(TrajeFila a, TrajeFila b) {
        return a.danza().equals(b.danza()) && a.personaje().equals(b.personaje())
                && Objects.equals(a.color(), b.color()) && Objects.equals(a.genero(), b.genero());
    }

    // ------------------------------------------------------------------ fotos

    private VBox columnaFotosErht() {
        List<URL> fotosErht = new ArrayList<>();
        for (String sufijoErht : new String[]{"", "_2", "_3", "_4", "_5"}) {
            for (String extensionErht : new String[]{"png", "jpg"}) {
                URL u = DetalleTrajeVista.class.getResource("/img/trajes/" + trajeErht.idTraje() + sufijoErht + "." + extensionErht);
                if (u != null) {
                    fotosErht.add(u);
                    break;
                }
            }
        }
        StackPane grandeErht = new StackPane();
        grandeErht.getStyleClass().add("detalle-foto");
        grandeErht.setPrefSize(440, 540);
        grandeErht.setMinSize(440, 540);
        grandeErht.setMaxSize(440, 540);
        Rectangle recorteErht = new Rectangle(440, 540);
        recorteErht.setArcWidth(36);
        recorteErht.setArcHeight(36);
        grandeErht.setClip(recorteErht);

        VBox cajaErht = new VBox(14, grandeErht);
        if (fotosErht.isEmpty()) {
            pintarMarcadorFotoErht(grandeErht);
        } else {
            pintarFotoErht(grandeErht, fotosErht.get(0));
            if (fotosErht.size() > 1) {
                HBox miniaturasErht = new HBox(10);
                for (URL u : fotosErht) {
                    StackPane m = new StackPane();
                    m.getStyleClass().add("miniatura");
                    m.setPrefSize(78, 92);
                    m.setStyle("-fx-background-image: url('" + u.toExternalForm() + "');"
                            + "-fx-background-size: cover; -fx-background-position: center;");
                    m.setOnMouseClicked(e -> {
                        pintarFotoErht(grandeErht, u);
                        miniaturasErht.getChildren().forEach(n -> n.getStyleClass().remove("elegida"));
                        m.getStyleClass().add("elegida");
                    });
                    miniaturasErht.getChildren().add(m);
                }
                miniaturasErht.getChildren().get(0).getStyleClass().add("elegida");
                cajaErht.getChildren().add(miniaturasErht);
            }
        }
        cajaErht.setMinWidth(440);
        return cajaErht;
    }

    private static void pintarFotoErht(StackPane p, URL u) {
        p.getChildren().clear();
        p.setStyle("-fx-background-image: url('" + u.toExternalForm() + "');"
                + "-fx-background-size: cover; -fx-background-position: center; -fx-background-radius: 18;");
    }

    private void pintarMarcadorFotoErht(StackPane p) {
        String colorErht = Adornos.colorDanza(trajeErht.danza());
        p.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + colorErht + ", 18%), derive("
                + colorErht + ", -45%)); -fx-background-radius: 18;");
        Group chakanaErht = Adornos.chakanaEnGrupo(380, "chakana-tenue");
        StackPane.setAlignment(chakanaErht, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chakanaErht, new Insets(0, -120, -100, 0));
        Label danzaErht = new Label(trajeErht.danza());
        danzaErht.getStyleClass().add("marcador-danza");
        danzaErht.setStyle("-fx-font-size: 42px;");
        Label subErht = new Label(UtilidadesInterfaz.espaciado(trajeErht.personaje()));
        subErht.getStyleClass().add("marcador-sub");
        VBox t = new VBox(6, danzaErht, subErht);
        t.setAlignment(Pos.CENTER);
        p.getChildren().setAll(chakanaErht, t);
    }

    // ------------------------------------------------------------------ información

    private VBox columnaDatosErht() {
        Label metaErht = new Label(UtilidadesInterfaz.espaciado(trajeErht.danza()) + "  •  " + UtilidadesInterfaz.espaciado(trajeErht.color() == null ? "—" : trajeErht.color()));
        metaErht.getStyleClass().add("traje-datos");
        Label nombreErht = new Label(trajeErht.personaje());
        nombreErht.getStyleClass().add("detalle-nombre");
        long n = trajeErht.vecesAlquilado();
        Label vecesErht = new Label(trajeErht.codigo() + "  ·  Alquilado " + n + (n == 1 ? " vez" : " veces"));
        vecesErht.getStyleClass().add("traje-veces");

        String generoErht = switch (trajeErht.genero() == null ? "" : trajeErht.genero()) {
            case "VARON" -> "para varón";
            case "MUJER" -> "para dama";
            default -> "unisex";
        };
        Label descErht = new Label("Traje completo de " + trajeErht.danza().toLowerCase(ES) + " " + generoErht
                + (trajeErht.color() == null ? "" : " en " + trajeErht.color().toLowerCase(ES))
                + ". Se entrega limpio y revisado pieza por pieza, listo para la Festividad de la Virgen de la "
                + "Candelaria. La garantía se devuelve completa si el traje vuelve sin daños.");
        descErht.getStyleClass().add("detalle-descripcion");
        descErht.setWrapText(true);
        descErht.setMinHeight(Region.USE_PREF_SIZE);

        // Piezas
        VBox piezasErht = new VBox(8);
        List<PiezaFila> listaErht = sErht.trajes().piezas(trajeErht.idTraje());
        if (listaErht.isEmpty()) {
            piezasErht.getChildren().add(UtilidadesInterfaz.subtitulo("Sin piezas registradas."));
        }
        for (PiezaFila p : listaErht) {
            Label l = new Label(p.tipo() + (p.descripcion() == null || p.descripcion().isBlank() ? "" : " · " + p.descripcion()),
                    UtilidadesInterfaz.icono(Feather.CHECK_CIRCLE));
            l.getStyleClass().add("pieza-item");
            l.setGraphicTextGap(10);
            piezasErht.getChildren().add(l);
        }

        // Tallas del modelo (clic para ver otra talla)
        FlowPane tallasErht = new FlowPane(8, 8);
        for (String t : UtilidadesInterfaz.TALLAS) {
            List<TrajeFila> deTallaErht = modeloErht.stream().filter(m -> t.equals(m.talla())).toList();
            if (deTallaErht.isEmpty()) {
                continue;
            }
            Button b = new Button(t + (deTallaErht.size() > 1 ? " ×" + deTallaErht.size() : ""));
            b.getStyleClass().add("talla-etiqueta");
            if (t.equals(trajeErht.talla())) {
                b.getStyleClass().add("elegida");
            } else {
                b.setOnAction(e -> portalErht.verTraje(deTallaErht.get(0), recojoErht.getValue(), devolucionErht.getValue()));
            }
            tallasErht.getChildren().add(b);
        }

        // Fechas, cantidad, conjunto
        GridPane fechasErht = new GridPane();
        fechasErht.setHgap(14);
        fechasErht.setVgap(6);
        fechasErht.addRow(0, UtilidadesInterfaz.etiqueta("FECHA DE RECOJO"), UtilidadesInterfaz.etiqueta("DEVOLUCIÓN"), UtilidadesInterfaz.etiqueta("CANTIDAD"));
        recojoErht.setPrefWidth(170);
        devolucionErht.setPrefWidth(170);
        cantidadErht.setPrefWidth(110);
        cantidadErht.setEditable(false);
        fechasErht.addRow(1, recojoErht, devolucionErht, cantidadErht);
        duracionErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-verde");
        disponiblesErht.getStyleClass().add("nota");
        HBox lineaFechasErht = new HBox(12, duracionErht, disponiblesErht);
        lineaFechasErht.setAlignment(Pos.CENTER_LEFT);
        VBox cajaConjuntoErht = new VBox(6, UtilidadesInterfaz.etiqueta("RESERVAR A NOMBRE DE"), conjuntoErht);
        conjuntoErht.setMaxWidth(420);
        if (conjuntoErht.getItems().size() <= 1) {
            cajaConjuntoErht.setVisible(false);
            cajaConjuntoErht.setManaged(false);
        }

        VBox infoErht = new VBox(14,
                metaErht, nombreErht, vecesErht, descErht,
                subtituloErht("Incluye"), piezasErht,
                subtituloErht("Talla"), tallasErht,
                subtituloErht("Disponibilidad"), calendarioCajaErht(),
                subtituloErht("Tus fechas"), fechasErht, lineaFechasErht, cajaConjuntoErht,
                new VBox(6, UtilidadesInterfaz.etiqueta("NOTA PARA LA TIENDA"), notaErht));
        VBox.setMargin(nombreErht, new Insets(-6, 0, 0, 0));
        HBox.setHgrow(infoErht, Priority.ALWAYS);
        infoErht.setMinWidth(380);
        infoErht.setMaxWidth(620);
        return infoErht;
    }

    private static Label subtituloErht(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("detalle-seccion");
        VBox.setMargin(l, new Insets(10, 0, 0, 0));
        return l;
    }

    // ------------------------------------------------------------------ calendario

    private VBox calendarioCajaErht() {
        Button antesErht = new Button(null, UtilidadesInterfaz.icono(Feather.CHEVRON_LEFT));
        Button despuesErht = new Button(null, UtilidadesInterfaz.icono(Feather.CHEVRON_RIGHT));
        antesErht.getStyleClass().add("cal-nav");
        despuesErht.getStyleClass().add("cal-nav");
        antesErht.setOnAction(e -> {
            mesErht = mesErht.minusMonths(1);
            pintarCalendarioErht();
        });
        despuesErht.setOnAction(e -> {
            mesErht = mesErht.plusMonths(1);
            pintarCalendarioErht();
        });
        mesTituloErht.getStyleClass().add("cal-mes");
        HBox cabeceraErht = new HBox(8, mesTituloErht, UtilidadesInterfaz.espacio(), antesErht, despuesErht);
        cabeceraErht.setAlignment(Pos.CENTER_LEFT);

        calendarioErht.setHgap(4);
        calendarioErht.setVgap(4);
        HBox leyendaErht = new HBox(16, leyendaErht("cal-ocupado", "Ocupado"), leyendaErht("cal-elegido", "Tus fechas"),
                leyendaErht("cal-libre", "Libre"));
        VBox cajaErht = new VBox(12, cabeceraErht, calendarioErht, leyendaErht);
        cajaErht.getStyleClass().add("cal-caja");
        cajaErht.setMaxWidth(380);
        return cajaErht;
    }

    private static HBox leyendaErht(String clase, String texto) {
        Region r = new Region();
        r.getStyleClass().addAll("cal-muestra", clase);
        Label l = new Label(texto);
        l.getStyleClass().add("nota");
        HBox h = new HBox(6, r, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void pintarCalendarioErht() {
        String nombreMesErht = mesErht.getMonth().getDisplayName(TextStyle.FULL, ES);
        mesTituloErht.setText(nombreMesErht.substring(0, 1).toUpperCase(ES) + nombreMesErht.substring(1) + " " + mesErht.getYear());
        calendarioErht.getChildren().clear();
        String[] diasErht = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(diasErht[i]);
            d.getStyleClass().add("cal-semana");
            calendarioErht.add(d, i, 0);
        }
        LocalDate a = recojoErht.getValue();
        LocalDate b = devolucionErht.getValue();
        int desfaseErht = mesErht.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int diaErht = 1; diaErht <= mesErht.lengthOfMonth(); diaErht++) {
            LocalDate fechaErht = mesErht.atDay(diaErht);
            Label c = new Label(String.valueOf(diaErht));
            c.getStyleClass().add("cal-dia");
            if (ocupadosErht.contains(fechaErht)) {
                c.getStyleClass().add("cal-ocupado");
            } else if (a != null && b != null && !fechaErht.isBefore(a) && !fechaErht.isAfter(b)) {
                c.getStyleClass().add("cal-elegido");
            } else if (fechaErht.isBefore(LocalDate.now())) {
                c.getStyleClass().add("cal-pasado");
            } else {
                c.getStyleClass().add("cal-libre");
            }
            if (fechaErht.equals(LocalDate.now())) {
                c.getStyleClass().add("cal-hoy");
            }
            int posErht = desfaseErht + diaErht - 1;
            calendarioErht.add(c, posErht % 7, 1 + posErht / 7);
        }
    }

    // ------------------------------------------------------------------ resumen

    private VBox resumenErht() {
        Label tituloErht = new Label("Resumen de tu reserva");
        tituloErht.getStyleClass().add("resumen-titulo");
        rTotalErht.getStyleClass().add("resumen-total");
        rApartanErht.getStyleClass().add("nota");
        rApartanErht.setWrapText(true);
        rAvisoErht.getStyleClass().add("error-suave");
        rAvisoErht.setWrapText(true);
        rAvisoErht.setGraphic(UtilidadesInterfaz.icono(Feather.ALERT_CIRCLE));
        rDescuentoTextoErht.setWrapText(true);
        rDescuentoTextoErht.setMaxWidth(190);

        Region sepErht = new Region();
        sepErht.getStyleClass().add("traje-separador");
        Label totalTextoErht = new Label("Total a pagar");
        totalTextoErht.getStyleClass().add("resumen-total-texto");

        solicitarErht.getStyleClass().addAll("boton-primario", "boton-solicitar");
        solicitarErht.setGraphic(UtilidadesInterfaz.icono(Feather.SEND));
        solicitarErht.setMaxWidth(Double.MAX_VALUE);
        solicitarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::enviarErht));
        Label pieErht = new Label("No pagas nada ahora: la tienda revisa tu solicitud y te confirma. "
                + "El traje queda apartado para ti mientras tanto.");
        pieErht.getStyleClass().add("nota");
        pieErht.setWrapText(true);

        VBox cajaErht = new VBox(12, tituloErht,
                lineaErht(rAlquilerTextoErht, rAlquilerErht), lineaErht(rDescuentoTextoErht, rDescuentoErht),
                lineaErht(new Label("Garantía reembolsable"), rGarantiaErht),
                sepErht, lineaErht(totalTextoErht, rTotalErht), rApartanErht, rAvisoErht, solicitarErht, pieErht);
        cajaErht.getStyleClass().add("resumen-tarjeta");
        cajaErht.setPrefWidth(340);
        cajaErht.setMinWidth(320);
        cajaErht.setMaxHeight(Region.USE_PREF_SIZE);
        return cajaErht;
    }

    private static HBox lineaErht(Label izquierda, Label derecha) {
        izquierda.getStyleClass().add("resumen-concepto");
        derecha.getStyleClass().add("resumen-monto");
        derecha.setMinWidth(Region.USE_PREF_SIZE);
        HBox h = new HBox(10, izquierda, UtilidadesInterfaz.espacio(), derecha);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Fechas cambiaron: vuelve a ver cuántos trajes del modelo están libres (fn_trajes_disponibles). */
    private void recalcularErht() {
        pintarCalendarioErht();
        LocalDate a = recojoErht.getValue();
        LocalDate b = devolucionErht.getValue();
        List<TrajeFila> libresErht = List.of();
        if (a != null && b != null && !b.isBefore(a)) {
            long diasErht = Math.max(1, ChronoUnit.DAYS.between(a, b));
            duracionErht.setText("Duración: " + diasErht + (diasErht == 1 ? " día" : " días"));
            Set<String> codigosErht = new HashSet<>(sErht.alquileres().codigosDisponibles(a, b, trajeErht.danza()));
            libresErht = modeloErht.stream().filter(t -> codigosErht.contains(t.codigo())).toList();
        } else {
            duracionErht.setText("Revisa las fechas");
        }
        boolean esteLibreErht = libresErht.stream().anyMatch(t -> t.idTraje().equals(trajeErht.idTraje()));
        // el traje elegido va primero; después los del mismo modelo
        List<TrajeFila> ordenErht = new ArrayList<>(libresErht);
        ordenErht.sort(Comparator.comparing((TrajeFila t) -> !t.idTraje().equals(trajeErht.idTraje())));
        elegidosErht = ordenErht;
        int maxErht = Math.max(1, ordenErht.size());
        Integer actualErht = cantidadErht.getValue();
        cantidadErht.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, maxErht,
                actualErht == null ? 1 : Math.min(actualErht, maxErht)));
        disponiblesErht.setText(ordenErht.size() == 0 ? "Ningún traje de este modelo está libre en esas fechas."
                : ordenErht.size() + (ordenErht.size() == 1 ? " traje de este modelo libre" : " trajes de este modelo libres")
                        + " en esas fechas" + (esteLibreErht ? "." : " (esta talla no; se apartan otras tallas)."));
        recalcularResumenErht();
    }

    /**
     * Recalcula el resumen de la reserva: alquiler, descuento por conjunto (10% desde 5 trajes), garantía
     * reembolsable y total. Usa el mismo redondeo que el servicio para que el monto mostrado sea el cobrado.
     */
    private void recalcularResumenErht() {
        LocalDate a = recojoErht.getValue();
        LocalDate b = devolucionErht.getValue();
        int n = cantidadErht.getValue() == null ? 1 : cantidadErht.getValue();
        List<TrajeFila> selErht = elegidosErht.stream().limit(n).toList();
        boolean fechasValidasErht = a != null && b != null && !b.isBefore(a) && !a.isBefore(LocalDate.now());
        boolean descuentoErht = conjuntoErht.getValue() != null && selErht.size() >= MINIMO_DESCUENTO;

        BigDecimal alquilerErht = BigDecimal.ZERO;
        BigDecimal conDescuentoErht = BigDecimal.ZERO;
        BigDecimal garantiaErht = BigDecimal.ZERO;
        // El descuento se aplica traje por traje; la garantía se suma completa y se devuelve si no hay daños.
        for (TrajeFila t : selErht) {
            alquilerErht = alquilerErht.add(t.precioAlquiler());
            conDescuentoErht = conDescuentoErht.add(descuentoErht
                    ? t.precioAlquiler().multiply(DESCUENTO_CONJUNTO).setScale(2, RoundingMode.HALF_UP)
                    : t.precioAlquiler());
            garantiaErht = garantiaErht.add(t.garantia());
        }
        long diasErht = fechasValidasErht ? Math.max(1, ChronoUnit.DAYS.between(a, b)) : 0;
        rAlquilerTextoErht.setText("Alquiler (" + selErht.size() + (selErht.size() == 1 ? " traje" : " trajes")
                + (diasErht > 0 ? " · " + diasErht + (diasErht == 1 ? " día)" : " días)") : ")"));
        rAlquilerErht.setText(UtilidadesInterfaz.soles(alquilerErht));
        if (descuentoErht) {
            rDescuentoTextoErht.setText("Descuento por conjunto (10%)");
            rDescuentoErht.setText("− " + UtilidadesInterfaz.soles(alquilerErht.subtract(conDescuentoErht)));
        } else {
            rDescuentoTextoErht.setText(conjuntoErht.getValue() == null
                    ? "Descuento por conjunto (10% desde 5 trajes a nombre de tu conjunto)"
                    : "Descuento por conjunto (desde " + MINIMO_DESCUENTO + " trajes)");
            rDescuentoErht.setText("—");
        }
        rGarantiaErht.setText(UtilidadesInterfaz.soles(garantiaErht));
        rTotalErht.setText(UtilidadesInterfaz.soles(conDescuentoErht.add(garantiaErht)));
        rApartanErht.setText(selErht.isEmpty() ? "" : "Se apartan: " + String.join(", ",
                selErht.stream().map(t -> t.codigo() + " (talla " + t.talla() + ")").toList()));

        String avisoErht = !fechasValidasErht ? "Elige una fecha de recojo desde hoy y una devolución posterior."
                : selErht.isEmpty() ? "No hay trajes libres de este modelo en esas fechas. Prueba otras fechas."
                : null;
        rAvisoErht.setText(avisoErht);
        rAvisoErht.setVisible(avisoErht != null);
        rAvisoErht.setManaged(avisoErht != null);
        solicitarErht.setDisable(avisoErht != null);
    }

    private void enviarErht() {
        LocalDate a = UtilidadesInterfaz.requerido(recojoErht, "Fecha de recojo");
        LocalDate b = UtilidadesInterfaz.requerido(devolucionErht, "Devolución");
        int n = cantidadErht.getValue() == null ? 1 : cantidadErht.getValue();
        List<Integer> idsErht = elegidosErht.stream().limit(n).map(TrajeFila::idTraje).toList();
        if (idsErht.isEmpty()) {
            throw new ExcepcionReglaNegocio("No hay trajes libres de este modelo en esas fechas.");
        }
        Opcion cj = conjuntoErht.getValue();
        int id = sErht.alquileres().solicitar(Sesion.actual().idCliente(), cj == null ? null : cj.id(), a, b, idsErht,
                notaErht.getText());
        portalErht.confirmar("¡Solicitud enviada!",
                "Tu solicitud N° " + id + " quedó registrada y " + (idsErht.size() == 1 ? "el traje quedó apartado"
                        : "los " + idsErht.size() + " trajes quedaron apartados") + " para ti. La tienda la revisará y "
                        + "verás la respuesta en «Mis reservas».",
                new AnimacionConfirmacion.Opcion("Ver mis reservas", true, () -> portalErht.ir(VistaPortalCliente.MIS_RESERVAS)),
                new AnimacionConfirmacion.Opcion("Seguir explorando", false, () -> portalErht.ir(VistaPortalCliente.CATALOGO)));
    }
}
