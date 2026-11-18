package com.trajes.fx.portal;

import com.trajes.fx.Adornos;
import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.modelo.Ocupacion;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.PiezaFila;
import com.trajes.modelo.TrajeFila;
import com.trajes.servicio.ReglaNegocioException;
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

public class DetalleTrajeVista extends BorderPane {

    private static final BigDecimal DESCUENTO_CONJUNTO = new BigDecimal("0.90");
    private static final int MINIMO_DESCUENTO = 5;
    private static final Locale ES = new Locale("es", "PE");

    private final Servicios s;
    private final PortalClienteView portal;
    private final TrajeFila traje;
     
    private final List<TrajeFila> modelo;
    private final Set<LocalDate> ocupados = new HashSet<>();

    private final DatePicker recojo;
    private final DatePicker devolucion;
    private final Spinner<Integer> cantidad = new Spinner<>();
    private final ComboBox<Opcion> conjunto;
    private final TextField nota = Ui.campo("", "Ej.: necesito probármelo antes (opcional)");
    private final Label duracion = new Label();
    private final Label disponibles = new Label();
    private final GridPane calendario = new GridPane();
    private final Label mesTitulo = new Label();
    private YearMonth mes;

    private final Label rAlquilerTexto = new Label();
    private final Label rAlquiler = new Label();
    private final Label rDescuentoTexto = new Label();
    private final Label rDescuento = new Label();
    private final Label rGarantia = new Label();
    private final Label rTotal = new Label();
    private final Label rApartan = new Label();
    private final Label rAviso = new Label();
    private final Button solicitar = new Button("Solicitar reserva");

    private List<TrajeFila> elegidos = List.of();

    public DetalleTrajeVista(Servicios s, PortalClienteView portal, TrajeFila traje, LocalDate desde, LocalDate hasta) {
        this.s = s;
        this.portal = portal;
        this.traje = traje;
        getStyleClass().add("detalle");

        List<TrajeFila> todos = s.trajes().listar(null, null, null, null);
        modelo = todos.stream()
                .filter(t -> "DISPONIBLE".equals(t.estado()) && mismoModelo(t, traje))
                .sorted(Comparator.comparing((TrajeFila t) -> !t.idTraje().equals(traje.idTraje()))
                        .thenComparing(t -> Ui.TALLAS.indexOf(t.talla())))
                .toList();
        for (Ocupacion o : s.alquileres().ocupaciones()) {
            if (o.idTraje() == traje.idTraje()) {
                LocalDate fin = "ENTREGADO".equals(o.estado()) && o.devolucionPrevista().isBefore(LocalDate.now())
                        ? LocalDate.now() : o.devolucionPrevista();
                for (LocalDate d = o.salida(); !d.isAfter(fin); d = d.plusDays(1)) {
                    ocupados.add(d);
                }
            }
        }
        LocalDate inicio = desde != null && !desde.isBefore(LocalDate.now()) ? desde : LocalDate.now().plusDays(1);
        recojo = Ui.fechaPicker(inicio);
        devolucion = Ui.fechaPicker(hasta != null && !hasta.isBefore(inicio) ? hasta : inicio.plusDays(3));
        mes = YearMonth.from(inicio);
        conjunto = Ui.combo(s.clientes().conjuntosDelDelegado(Sesion.get().getCliente()), Opcion::nombre,
                "A mi nombre (sin conjunto)");

        Hyperlink volver = new Hyperlink("Volver a los trajes", Ui.icono(Feather.ARROW_LEFT));
        volver.getStyleClass().add("enlace");
        volver.setOnAction(e -> portal.volver());
        HBox migas = new HBox(volver);
        migas.setPadding(new Insets(22, 38, 0, 38));
        setTop(migas);

        HBox cuerpo = new HBox(34, columnaFotos(), columnaInfo());
        cuerpo.setPadding(new Insets(18, 10, 40, 38));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("detalle-scroll");
        setCenter(scroll);

        VBox resumen = resumen();
        BorderPane.setMargin(resumen, new Insets(18, 38, 30, 6));
        setRight(resumen);   

        recojo.valueProperty().addListener((o, a, b) -> {
            if (b != null) {
                mes = YearMonth.from(b);
            }
            recalcular();
        });
        devolucion.valueProperty().addListener((o, a, b) -> recalcular());
        cantidad.valueProperty().addListener((o, a, b) -> recalcularResumen());
        conjunto.valueProperty().addListener((o, a, b) -> recalcularResumen());
        recalcular();
    }

    private static boolean mismoModelo(TrajeFila a, TrajeFila b) {
        return a.danza().equals(b.danza()) && a.personaje().equals(b.personaje())
                && Objects.equals(a.color(), b.color()) && Objects.equals(a.genero(), b.genero());
    }

    private VBox columnaFotos() {
        List<URL> fotos = new ArrayList<>();
        for (String sufijo : new String[]{"", "_2", "_3", "_4", "_5"}) {
            for (String ext : new String[]{"png", "jpg"}) {
                URL u = DetalleTrajeVista.class.getResource("/img/trajes/" + traje.idTraje() + sufijo + "." + ext);
                if (u != null) {
                    fotos.add(u);
                    break;
                }
            }
        }
        StackPane grande = new StackPane();
        grande.getStyleClass().add("detalle-foto");
        grande.setPrefSize(440, 540);
        grande.setMinSize(440, 540);
        grande.setMaxSize(440, 540);
        Rectangle clip = new Rectangle(440, 540);
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        grande.setClip(clip);

        VBox caja = new VBox(14, grande);
        if (fotos.isEmpty()) {
            pintarPlaceholder(grande);
        } else {
            pintarFoto(grande, fotos.get(0));
            if (fotos.size() > 1) {
                HBox miniaturas = new HBox(10);
                for (URL u : fotos) {
                    StackPane m = new StackPane();
                    m.getStyleClass().add("miniatura");
                    m.setPrefSize(78, 92);
                    m.setStyle("-fx-background-image: url('" + u.toExternalForm() + "');"
                            + "-fx-background-size: cover; -fx-background-position: center;");
                    m.setOnMouseClicked(e -> {
                        pintarFoto(grande, u);
                        miniaturas.getChildren().forEach(n -> n.getStyleClass().remove("elegida"));
                        m.getStyleClass().add("elegida");
                    });
                    miniaturas.getChildren().add(m);
                }
                miniaturas.getChildren().get(0).getStyleClass().add("elegida");
                caja.getChildren().add(miniaturas);
            }
        }
        caja.setMinWidth(440);
        return caja;
    }

    private static void pintarFoto(StackPane p, URL u) {
        p.getChildren().clear();
        p.setStyle("-fx-background-image: url('" + u.toExternalForm() + "');"
                + "-fx-background-size: cover; -fx-background-position: center; -fx-background-radius: 18;");
    }

    private void pintarPlaceholder(StackPane p) {
        String color = Adornos.colorDanza(traje.danza());
        p.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + color + ", 18%), derive("
                + color + ", -45%)); -fx-background-radius: 18;");
        Group chakana = Adornos.chakanaEnGrupo(380, "chakana-tenue");
        StackPane.setAlignment(chakana, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(chakana, new Insets(0, -120, -100, 0));
        Label danza = new Label(traje.danza());
        danza.getStyleClass().add("placeholder-danza");
        danza.setStyle("-fx-font-size: 42px;");
        Label sub = new Label(Ui.espaciado(traje.personaje()));
        sub.getStyleClass().add("placeholder-sub");
        VBox t = new VBox(6, danza, sub);
        t.setAlignment(Pos.CENTER);
        p.getChildren().setAll(chakana, t);
    }

    private VBox columnaInfo() {
        Label meta = new Label(Ui.espaciado(traje.danza()) + "  •  " + Ui.espaciado(traje.color() == null ? "—" : traje.color()));
        meta.getStyleClass().add("traje-meta");
        Label nombre = new Label(traje.personaje());
        nombre.getStyleClass().add("detalle-nombre");
        long n = traje.vecesAlquilado();
        Label veces = new Label(traje.codigo() + "  ·  Alquilado " + n + (n == 1 ? " vez" : " veces"));
        veces.getStyleClass().add("traje-veces");

        String genero = switch (traje.genero() == null ? "" : traje.genero()) {
            case "VARON" -> "para varón";
            case "MUJER" -> "para dama";
            default -> "unisex";
        };
        Label desc = new Label("Traje completo de " + traje.danza().toLowerCase(ES) + " " + genero
                + (traje.color() == null ? "" : " en " + traje.color().toLowerCase(ES))
                + ". Se entrega limpio y revisado pieza por pieza, listo para la Festividad de la Virgen de la "
                + "Candelaria. La garantía se devuelve completa si el traje vuelve sin daños.");
        desc.getStyleClass().add("detalle-desc");
        desc.setWrapText(true);
        desc.setMinHeight(Region.USE_PREF_SIZE);

        VBox piezas = new VBox(8);
        List<PiezaFila> lista = s.trajes().piezas(traje.idTraje());
        if (lista.isEmpty()) {
            piezas.getChildren().add(Ui.subtitulo("Sin piezas registradas."));
        }
        for (PiezaFila p : lista) {
            Label l = new Label(p.tipo() + (p.descripcion() == null || p.descripcion().isBlank() ? "" : " · " + p.descripcion()),
                    Ui.icono(Feather.CHECK_CIRCLE));
            l.getStyleClass().add("pieza-item");
            l.setGraphicTextGap(10);
            piezas.getChildren().add(l);
        }

        FlowPane tallas = new FlowPane(8, 8);
        for (String t : Ui.TALLAS) {
            List<TrajeFila> deTalla = modelo.stream().filter(m -> t.equals(m.talla())).toList();
            if (deTalla.isEmpty()) {
                continue;
            }
            Button b = new Button(t + (deTalla.size() > 1 ? " ×" + deTalla.size() : ""));
            b.getStyleClass().add("talla-chip");
            if (t.equals(traje.talla())) {
                b.getStyleClass().add("elegida");
            } else {
                b.setOnAction(e -> portal.verTraje(deTalla.get(0), recojo.getValue(), devolucion.getValue()));
            }
            tallas.getChildren().add(b);
        }

        GridPane fechas = new GridPane();
        fechas.setHgap(14);
        fechas.setVgap(6);
        fechas.addRow(0, Ui.etiqueta("FECHA DE RECOJO"), Ui.etiqueta("DEVOLUCIÓN"), Ui.etiqueta("CANTIDAD"));
        recojo.setPrefWidth(170);
        devolucion.setPrefWidth(170);
        cantidad.setPrefWidth(110);
        cantidad.setEditable(false);
        fechas.addRow(1, recojo, devolucion, cantidad);
        duracion.getStyleClass().addAll("chip", "chip-verde");
        disponibles.getStyleClass().add("nota");
        HBox lineaFechas = new HBox(12, duracion, disponibles);
        lineaFechas.setAlignment(Pos.CENTER_LEFT);
        VBox cajaConjunto = new VBox(6, Ui.etiqueta("RESERVAR A NOMBRE DE"), conjunto);
        conjunto.setMaxWidth(420);
        if (conjunto.getItems().size() <= 1) {
            cajaConjunto.setVisible(false);
            cajaConjunto.setManaged(false);
        }

        VBox info = new VBox(14,
                meta, nombre, veces, desc,
                subtitulo("Incluye"), piezas,
                subtitulo("Talla"), tallas,
                subtitulo("Disponibilidad"), calendarioCaja(),
                subtitulo("Tus fechas"), fechas, lineaFechas, cajaConjunto,
                new VBox(6, Ui.etiqueta("NOTA PARA LA TIENDA"), nota));
        VBox.setMargin(nombre, new Insets(-6, 0, 0, 0));
        HBox.setHgrow(info, Priority.ALWAYS);
        info.setMinWidth(380);
        info.setMaxWidth(620);
        return info;
    }

    private static Label subtitulo(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("detalle-seccion");
        VBox.setMargin(l, new Insets(10, 0, 0, 0));
        return l;
    }

    private VBox calendarioCaja() {
        Button antes = new Button(null, Ui.icono(Feather.CHEVRON_LEFT));
        Button despues = new Button(null, Ui.icono(Feather.CHEVRON_RIGHT));
        antes.getStyleClass().add("cal-nav");
        despues.getStyleClass().add("cal-nav");
        antes.setOnAction(e -> {
            mes = mes.minusMonths(1);
            pintarCalendario();
        });
        despues.setOnAction(e -> {
            mes = mes.plusMonths(1);
            pintarCalendario();
        });
        mesTitulo.getStyleClass().add("cal-mes");
        HBox cabecera = new HBox(8, mesTitulo, Ui.espacio(), antes, despues);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        calendario.setHgap(4);
        calendario.setVgap(4);
        HBox leyenda = new HBox(16, leyenda("cal-ocupado", "Ocupado"), leyenda("cal-elegido", "Tus fechas"),
                leyenda("cal-libre", "Libre"));
        VBox caja = new VBox(12, cabecera, calendario, leyenda);
        caja.getStyleClass().add("cal-caja");
        caja.setMaxWidth(380);
        return caja;
    }

    private static HBox leyenda(String clase, String texto) {
        Region r = new Region();
        r.getStyleClass().addAll("cal-muestra", clase);
        Label l = new Label(texto);
        l.getStyleClass().add("nota");
        HBox h = new HBox(6, r, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void pintarCalendario() {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, ES);
        mesTitulo.setText(nombreMes.substring(0, 1).toUpperCase(ES) + nombreMes.substring(1) + " " + mes.getYear());
        calendario.getChildren().clear();
        String[] dias = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label d = new Label(dias[i]);
            d.getStyleClass().add("cal-semana");
            calendario.add(d, i, 0);
        }
        LocalDate a = recojo.getValue();
        LocalDate b = devolucion.getValue();
        int desfase = mes.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int dia = 1; dia <= mes.lengthOfMonth(); dia++) {
            LocalDate fecha = mes.atDay(dia);
            Label c = new Label(String.valueOf(dia));
            c.getStyleClass().add("cal-dia");
            if (ocupados.contains(fecha)) {
                c.getStyleClass().add("cal-ocupado");
            } else if (a != null && b != null && !fecha.isBefore(a) && !fecha.isAfter(b)) {
                c.getStyleClass().add("cal-elegido");
            } else if (fecha.isBefore(LocalDate.now())) {
                c.getStyleClass().add("cal-pasado");
            } else {
                c.getStyleClass().add("cal-libre");
            }
            if (fecha.equals(LocalDate.now())) {
                c.getStyleClass().add("cal-hoy");
            }
            int pos = desfase + dia - 1;
            calendario.add(c, pos % 7, 1 + pos / 7);
        }
    }

    private VBox resumen() {
        Label titulo = new Label("Resumen de tu reserva");
        titulo.getStyleClass().add("resumen-titulo");
        rTotal.getStyleClass().add("resumen-total");
        rApartan.getStyleClass().add("nota");
        rApartan.setWrapText(true);
        rAviso.getStyleClass().add("error-suave");
        rAviso.setWrapText(true);
        rAviso.setGraphic(Ui.icono(Feather.ALERT_CIRCLE));
        rDescuentoTexto.setWrapText(true);
        rDescuentoTexto.setMaxWidth(190);

        Region sep = new Region();
        sep.getStyleClass().add("traje-separador");
        Label totalTexto = new Label("Total a pagar");
        totalTexto.getStyleClass().add("resumen-total-texto");

        solicitar.getStyleClass().addAll("boton-primario", "boton-solicitar");
        solicitar.setGraphic(Ui.icono(Feather.SEND));
        solicitar.setMaxWidth(Double.MAX_VALUE);
        solicitar.setOnAction(e -> Ui.ejecutar(this::enviar));
        Label pie = new Label("No pagas nada ahora: la tienda revisa tu solicitud y te confirma. "
                + "El traje queda apartado para ti mientras tanto.");
        pie.getStyleClass().add("nota");
        pie.setWrapText(true);

        VBox caja = new VBox(12, titulo,
                linea(rAlquilerTexto, rAlquiler), linea(rDescuentoTexto, rDescuento),
                linea(new Label("Garantía reembolsable"), rGarantia),
                sep, linea(totalTexto, rTotal), rApartan, rAviso, solicitar, pie);
        caja.getStyleClass().add("resumen-tarjeta");
        caja.setPrefWidth(340);
        caja.setMinWidth(320);
        caja.setMaxHeight(Region.USE_PREF_SIZE);
        return caja;
    }

    private static HBox linea(Label izquierda, Label derecha) {
        izquierda.getStyleClass().add("resumen-concepto");
        derecha.getStyleClass().add("resumen-monto");
        derecha.setMinWidth(Region.USE_PREF_SIZE);
        HBox h = new HBox(10, izquierda, Ui.espacio(), derecha);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void recalcular() {
        pintarCalendario();
        LocalDate a = recojo.getValue();
        LocalDate b = devolucion.getValue();
        List<TrajeFila> libres = List.of();
        if (a != null && b != null && !b.isBefore(a)) {
            long dias = Math.max(1, ChronoUnit.DAYS.between(a, b));
            duracion.setText("Duración: " + dias + (dias == 1 ? " día" : " días"));
            Set<String> codigos = new HashSet<>(s.alquileres().codigosDisponibles(a, b, traje.danza()));
            libres = modelo.stream().filter(t -> codigos.contains(t.codigo())).toList();
        } else {
            duracion.setText("Revisa las fechas");
        }
        boolean esteLibre = libres.stream().anyMatch(t -> t.idTraje().equals(traje.idTraje()));
        
        List<TrajeFila> orden = new ArrayList<>(libres);
        orden.sort(Comparator.comparing((TrajeFila t) -> !t.idTraje().equals(traje.idTraje())));
        elegidos = orden;
        int max = Math.max(1, orden.size());
        Integer actual = cantidad.getValue();
        cantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, max,
                actual == null ? 1 : Math.min(actual, max)));
        disponibles.setText(orden.size() == 0 ? "Ningún traje de este modelo está libre en esas fechas."
                : orden.size() + (orden.size() == 1 ? " traje de este modelo libre" : " trajes de este modelo libres")
                        + " en esas fechas" + (esteLibre ? "." : " (esta talla no; se apartan otras tallas)."));
        recalcularResumen();
    }

    private void recalcularResumen() {
        LocalDate a = recojo.getValue();
        LocalDate b = devolucion.getValue();
        int n = cantidad.getValue() == null ? 1 : cantidad.getValue();
        List<TrajeFila> sel = elegidos.stream().limit(n).toList();
        boolean fechasOk = a != null && b != null && !b.isBefore(a) && !a.isBefore(LocalDate.now());
        boolean descuento = conjunto.getValue() != null && sel.size() >= MINIMO_DESCUENTO;

        BigDecimal alquiler = BigDecimal.ZERO;
        BigDecimal conDescuento = BigDecimal.ZERO;
        BigDecimal garantia = BigDecimal.ZERO;
        for (TrajeFila t : sel) {
            alquiler = alquiler.add(t.precioAlquiler());
            conDescuento = conDescuento.add(descuento
                    ? t.precioAlquiler().multiply(DESCUENTO_CONJUNTO).setScale(2, RoundingMode.HALF_UP)
                    : t.precioAlquiler());
            garantia = garantia.add(t.garantia());
        }
        long dias = fechasOk ? Math.max(1, ChronoUnit.DAYS.between(a, b)) : 0;
        rAlquilerTexto.setText("Alquiler (" + sel.size() + (sel.size() == 1 ? " traje" : " trajes")
                + (dias > 0 ? " · " + dias + (dias == 1 ? " día)" : " días)") : ")"));
        rAlquiler.setText(Ui.soles(alquiler));
        if (descuento) {
            rDescuentoTexto.setText("Descuento por conjunto (10%)");
            rDescuento.setText("− " + Ui.soles(alquiler.subtract(conDescuento)));
        } else {
            rDescuentoTexto.setText(conjunto.getValue() == null
                    ? "Descuento por conjunto (10% desde 5 trajes a nombre de tu conjunto)"
                    : "Descuento por conjunto (desde " + MINIMO_DESCUENTO + " trajes)");
            rDescuento.setText("—");
        }
        rGarantia.setText(Ui.soles(garantia));
        rTotal.setText(Ui.soles(conDescuento.add(garantia)));
        rApartan.setText(sel.isEmpty() ? "" : "Se apartan: " + String.join(", ",
                sel.stream().map(t -> t.codigo() + " (talla " + t.talla() + ")").toList()));

        String aviso = !fechasOk ? "Elige una fecha de recojo desde hoy y una devolución posterior."
                : sel.isEmpty() ? "No hay trajes libres de este modelo en esas fechas. Prueba otras fechas."
                : null;
        rAviso.setText(aviso);
        rAviso.setVisible(aviso != null);
        rAviso.setManaged(aviso != null);
        solicitar.setDisable(aviso != null);
    }

    private void enviar() {
        LocalDate a = Ui.requerido(recojo, "Fecha de recojo");
        LocalDate b = Ui.requerido(devolucion, "Devolución");
        int n = cantidad.getValue() == null ? 1 : cantidad.getValue();
        List<Integer> ids = elegidos.stream().limit(n).map(TrajeFila::idTraje).toList();
        if (ids.isEmpty()) {
            throw new ReglaNegocioException("No hay trajes libres de este modelo en esas fechas.");
        }
        Opcion cj = conjunto.getValue();
        int id = s.alquileres().solicitar(Sesion.get().getCliente(), cj == null ? null : cj.id(), a, b, ids,
                nota.getText());
        portal.confirmar("¡Solicitud enviada!",
                "Tu solicitud N° " + id + " quedó registrada y " + (ids.size() == 1 ? "el traje quedó apartado"
                        : "los " + ids.size() + " trajes quedaron apartados") + " para ti. La tienda la revisará y "
                        + "verás la respuesta en «Mis reservas».",
                new ConfirmacionCheck.Opcion("Ver mis reservas", true, () -> portal.ir(PortalClienteView.MIS_RESERVAS)),
                new ConfirmacionCheck.Opcion("Seguir explorando", false, () -> portal.ir(PortalClienteView.CATALOGO)));
    }
}
