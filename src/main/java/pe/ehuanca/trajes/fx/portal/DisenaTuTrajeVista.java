/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.fx.foto.CatalogoRenderizados;
import pe.ehuanca.trajes.fx.foto.PrendaTenible;
import pe.ehuanca.trajes.fx.foto.RenderizadoCapas;
import pe.ehuanca.trajes.modelo.ColorTraje;
import pe.ehuanca.trajes.modelo.Implemento;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.ParteElegida;
import pe.ehuanca.trajes.modelo.ParteTraje;
import pe.ehuanca.trajes.servicio.PersonalizadorServicio;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.feather.Feather;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diseña tu traje (portal del cliente): personalizador por capas sobre los renders de render/&lt;danza&gt;/&lt;ángulo&gt;/.
 * Pasos: ① Danza ② Prenda ③ Color ④ Implementos ⑤ Modalidad. Al enviar se crea el pedido PENDIENTE con cada prenda
 * y su color (hex), los implementos y la imagen del diseño (en la base y en la carpeta disenos/).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class DisenaTuTrajeVista extends HBox {

    private static final BigDecimal FACTOR_ALQUILER = PersonalizadorServicio.FACTOR_ALQUILER;
    private static final Path CARPETA_DISENOS = Path.of(System.getProperty("user.dir"), "disenos");

    private static final Background FONDO_ESTUDIO = new Background(new BackgroundFill(
            new RadialGradient(0, 0, 0.5, 0.42, 0.75, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5A2A34")), new Stop(0.55, Color.web("#2B0F15")), new Stop(1, Color.web("#12070A"))),
            null, null));

    private record ColorElegido(Color color, Integer idColor, String nombre) {
        String colorHex() {
            return String.format("#%02X%02X%02X", Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255),
                    Math.round(color.getBlue() * 255));
        }
    }

    private final Servicios sErht;
    private final VistaPortalCliente portalErht;
    private final Map<String, List<String>> rendersErht = CatalogoRenderizados.danzas();
    private final List<ColorTraje> paletaErht;
    private final List<Implemento> implementosErht;

    private Opcion danzaErht;
    private final Map<String, RenderizadoCapas> angulosErht = new LinkedHashMap<>();
    private List<String> nombresAngulosErht = List.of();
    private int anguloErht;
    private final Map<String, ParteTraje> partePorPrendaErht = new LinkedHashMap<>();
    private final Map<String, String> nombrePrendaErht = new LinkedHashMap<>();
    private final Map<String, ColorElegido> coloresErht = new LinkedHashMap<>();
    private String prendaActualErht;
    private final Map<Integer, CheckBox> marcasImplementosErht = new LinkedHashMap<>();

    private final StackPane escenarioErht = new StackPane();
    private final StackPane capasErht = new StackPane();
    private final Label lAnguloErht = new Label();
    private final Button antesErht = new Button(null, UtilidadesInterfaz.icono(Feather.CHEVRON_LEFT, 22));
    private final Button despuesErht = new Button(null, UtilidadesInterfaz.icono(Feather.CHEVRON_RIGHT, 22));
    private final Label avisoErht = new Label();
    private final FlowPane opcionesDanzaErht = new FlowPane(8, 8);
    private final FlowPane opcionesPrendaErht = new FlowPane(8, 8);
    private final FlowPane circulosErht = new FlowPane(10, 10);
    private final ColorPicker selectorColorErht = new ColorPicker(Color.WHITE);
    private final Label lColorErht = new Label();
    private final ToggleGroup modalidadErht = new ToggleGroup();
    private final ToggleGroup tallasErht = new ToggleGroup();
    private final DatePicker entregaErht = UtilidadesInterfaz.selectorFecha(LocalDate.now().plusDays(21));
    private final VBox lineasResumenErht = new VBox(6);
    private final Label totalErht = new Label();
    private final Button enviarErht = new Button("Enviar mi diseño a la tienda", UtilidadesInterfaz.icono(Feather.SEND));
    private boolean pintandoErht;

    public DisenaTuTrajeVista(Servicios s, VistaPortalCliente portal) {
        this.sErht = s;
        this.portalErht = portal;
        this.paletaErht = s.personalizador().colores();
        this.implementosErht = s.personalizador().implementos();
        getStyleClass().addAll("contenido", "disena");
        setSpacing(26);

        VBox izquierdaErht = new VBox(14, UtilidadesInterfaz.encabezado("Personalizador", "Diseña tu traje",
                "Elige la danza, el color de cada prenda y los implementos. Te mostramos el precio estimado al instante."),
                estudioErht());
        HBox.setHgrow(izquierdaErht, Priority.ALWAYS);
        VBox.setVgrow(escenarioErht, Priority.ALWAYS);

        ScrollPane pasosErht = new ScrollPane(tarjetaPasosErht());
        pasosErht.setFitToWidth(true);
        pasosErht.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pasosErht.getStyleClass().add("pasos-scroll");
        pasosErht.setMinWidth(430);
        pasosErht.setPrefWidth(460);
        pasosErht.setMaxWidth(480);
        getChildren().addAll(izquierdaErht, pasosErht);

        selectorColorErht.valueProperty().addListener((o, a, b) -> {
            if (!pintandoErht && b != null) {
                elegirColorErht(new ColorElegido(b, null, "Personalizado"));
            }
        });
        Opcion primeraErht = null;
        for (Node n : opcionesDanzaErht.getChildren()) {
            if (!n.isDisabled()) {
                primeraErht = (Opcion) n.getUserData();
                ((ToggleButton) n).setSelected(true);
                break;
            }
        }
        if (primeraErht != null) {
            elegirDanzaErht(primeraErht);
        } else {
            avisoErht.setText("Todavía no hay renders de trajes para personalizar.");
            avisoErht.setVisible(true);
        }
    }

    private StackPane estudioErht() {
        escenarioErht.getStyleClass().add("estudio");
        escenarioErht.setMinSize(0, 0);
        escenarioErht.setPrefSize(500, 560);
        escenarioErht.setPadding(new Insets(24));
        escenarioErht.getChildren().add(capasErht);

        antesErht.getStyleClass().add("flecha-angulo");
        despuesErht.getStyleClass().add("flecha-angulo");
        antesErht.setOnAction(e -> girarErht(-1));
        despuesErht.setOnAction(e -> girarErht(1));
        StackPane.setAlignment(antesErht, Pos.CENTER_LEFT);
        StackPane.setAlignment(despuesErht, Pos.CENTER_RIGHT);
        lAnguloErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-portada");
        StackPane.setAlignment(lAnguloErht, Pos.BOTTOM_CENTER);
        avisoErht.getStyleClass().add("aviso-capa");
        avisoErht.setWrapText(true);
        avisoErht.setMaxWidth(420);
        avisoErht.setVisible(false);
        StackPane.setAlignment(avisoErht, Pos.TOP_CENTER);
        escenarioErht.getChildren().addAll(antesErht, despuesErht, lAnguloErht, avisoErht);
        return escenarioErht;
    }

    private void mostrarAnguloErht() {
        capasErht.getChildren().clear();
        if (nombresAngulosErht.isEmpty()) {
            return;
        }
        RenderizadoCapas r = angulosErht.get(nombresAngulosErht.get(anguloErht));
        capasErht.getChildren().add(vistaErht(new ImageView(r.base())));
        for (PrendaTenible p : r.prendas()) {
            capasErht.getChildren().add(vistaErht(new ImageView(p.imagen())));
        }
        boolean variosErht = nombresAngulosErht.size() > 1;
        antesErht.setVisible(variosErht);
        despuesErht.setVisible(variosErht);
        String a = nombresAngulosErht.get(anguloErht);
        lAnguloErht.setText(a.substring(0, 1).toUpperCase(Locale.ROOT) + a.substring(1)
                + (variosErht ? "  ·  " + (anguloErht + 1) + " de " + nombresAngulosErht.size() : ""));
        boolean opacoErht = r.prendas().stream().anyMatch(p -> p.fraccionTransparente() < 0.05);
        avisoErht.setText("Vista previa aproximada: este render aún no tiene fondo transparente.");
        avisoErht.setVisible(opacoErht);
    }

    private ImageView vistaErht(ImageView iv) {
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.fitWidthProperty().bind(escenarioErht.widthProperty().subtract(48));
        iv.fitHeightProperty().bind(escenarioErht.heightProperty().subtract(48));
        return iv;
    }

    private void girarErht(int paso) {
        if (nombresAngulosErht.isEmpty()) {
            return;
        }
        anguloErht = Math.floorMod(anguloErht + paso, nombresAngulosErht.size());
        mostrarAnguloErht();
    }

    private VBox tarjetaPasosErht() {
        for (Opcion d : sErht.trajes().danzas()) {
            ToggleButton b = new ToggleButton(d.nombre());
            b.getStyleClass().add("etiqueta-opcion-filtro");
            b.setUserData(d);
            boolean hayErht = CatalogoRenderizados.carpetaDe(d.nombre(), rendersErht) != null;
            b.setDisable(!hayErht);
            if (!hayErht) {
                b.setTooltip(new Tooltip("Pronto podrás personalizar esta danza."));
            }
            b.setOnAction(e -> {
                b.setSelected(true);
                opcionesDanzaErht.getChildren().forEach(n -> {
                    if (n != b) {
                        ((ToggleButton) n).setSelected(false);
                    }
                });
                UtilidadesInterfaz.ejecutar(() -> elegirDanzaErht(d));
            });
            opcionesDanzaErht.getChildren().add(b);
        }

        for (ColorTraje c : paletaErht) {
            Circle circErht = new Circle(15, Color.web(c.colorHex()));
            circErht.getStyleClass().add("circulo-color");
            StackPane botonErht = new StackPane(circErht);
            botonErht.getStyleClass().add("muestra");
            botonErht.setUserData(c);
            Tooltip.install(botonErht, new Tooltip(c.nombre() + "  " + c.colorHex()));
            botonErht.setOnMouseClicked(e -> elegirColorErht(new ColorElegido(Color.web(c.colorHex()), c.idColor(), c.nombre())));
            circulosErht.getChildren().add(botonErht);
        }
        selectorColorErht.setPrefWidth(200);
        lColorErht.getStyleClass().add("nota");
        Label otroErht = UtilidadesInterfaz.etiqueta("Otro color");
        otroErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox filaSelectorErht = new HBox(12, otroErht, selectorColorErht);
        filaSelectorErht.setAlignment(Pos.CENTER_LEFT);
        lColorErht.setWrapText(true);
        VBox lineaSelectorErht = new VBox(8, filaSelectorErht, lColorErht);

        VBox cajaImplementosErht = new VBox(8);
        for (Implemento impErht : implementosErht) {
            int n = cantidadSugeridaErht(impErht.nombre());
            CheckBox cb = new CheckBox(impErht.nombre() + (n > 1 ? "  ×" + n : ""));
            cb.setUserData(n);
            Label precioErht = new Label(UtilidadesInterfaz.soles(impErht.precio()) + (n > 1 ? " c/u" : ""));
            precioErht.getStyleClass().add("nota");
            cb.setOnAction(e -> recalcularErht());
            if (impErht.parteDestino() != null) {
                cb.setTooltip(new Tooltip("Va en: " + impErht.parteDestino()));
            }
            HBox filaErht = new HBox(8, cb, UtilidadesInterfaz.espacio(), precioErht);
            filaErht.setAlignment(Pos.CENTER_LEFT);
            cajaImplementosErht.getChildren().add(filaErht);
            marcasImplementosErht.put(impErht.idImplemento(), cb);
        }

        ToggleButton confeccionErht = tarjetaModalidadErht("CONFECCION", "Confeccionar", "El traje es tuyo", Feather.SCISSORS);
        ToggleButton alquilarErht = tarjetaModalidadErht("ALQUILER", "Alquilar", "30% del costo de confección", Feather.REPEAT);
        confeccionErht.setSelected(true);
        modalidadErht.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                modalidadErht.selectToggle(a);
            } else {
                recalcularErht();
            }
        });
        HBox cajaModalidadErht = new HBox(10, confeccionErht, alquilarErht);
        cajaModalidadErht.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        HBox cajaTallasErht = new HBox(6);
        for (String t : UtilidadesInterfaz.TALLAS) {
            ToggleButton tb = new ToggleButton(t);
            tb.getStyleClass().add("talla-etiqueta");
            tb.setToggleGroup(tallasErht);
            tb.setUserData(t);
            tb.setMinWidth(Region.USE_PREF_SIZE);
            if ("M".equals(t)) {
                tb.setSelected(true);
            }
            cajaTallasErht.getChildren().add(tb);
        }
        tallasErht.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                tallasErht.selectToggle(a);
            }
        });
        entregaErht.setPrefWidth(170);
        VBox tallaEntregaErht = new VBox(12, new VBox(6, UtilidadesInterfaz.etiqueta("TALLA"), cajaTallasErht),
                new VBox(6, UtilidadesInterfaz.etiqueta("ENTREGA"), entregaErht));

        Label tituloResumenErht = new Label("Precio estimado");
        tituloResumenErht.getStyleClass().add("resumen-titulo");
        totalErht.getStyleClass().add("resumen-total");
        enviarErht.getStyleClass().addAll("boton-primario", "boton-solicitar");
        enviarErht.setMaxWidth(Double.MAX_VALUE);
        enviarErht.setOnAction(e -> UtilidadesInterfaz.ejecutar(this::enviarDisenoErht));
        Label notaErht = new Label("Es un estimado: la tienda revisa tu diseño, te envía el precio final y tú lo apruebas.");
        notaErht.getStyleClass().add("nota");
        notaErht.setWrapText(true);
        Region sepErht = new Region();
        sepErht.getStyleClass().add("traje-separador");
        HBox lineaTotalErht = new HBox(10, new Label("Total estimado"), UtilidadesInterfaz.espacio(), totalErht);
        lineaTotalErht.setAlignment(Pos.CENTER_LEFT);
        lineaTotalErht.getChildren().get(0).getStyleClass().add("resumen-total-texto");
        VBox resumenErht = new VBox(10, tituloResumenErht, lineasResumenErht, sepErht, lineaTotalErht, enviarErht, notaErht);
        resumenErht.getStyleClass().add("resumen-disena");

        VBox tarjetaErht = new VBox(18,
                pasoErht(1, "Danza", opcionesDanzaErht),
                pasoErht(2, "Prenda", opcionesPrendaErht),
                pasoErht(3, "Color", new VBox(12, circulosErht, lineaSelectorErht)),
                pasoErht(4, "Implementos", cajaImplementosErht),
                pasoErht(5, "Modalidad", new VBox(14, cajaModalidadErht, tallaEntregaErht)),
                resumenErht);
        tarjetaErht.getStyleClass().add("pasos-tarjeta");
        return tarjetaErht;
    }

    private ToggleButton tarjetaModalidadErht(String tipo, String titulo, String texto, Feather icono) {
        Label t = new Label(titulo, UtilidadesInterfaz.icono(icono, 16));
        t.getStyleClass().add("modalidad-titulo");
        Label x = new Label(texto);
        x.getStyleClass().add("nota");
        ToggleButton b = new ToggleButton();
        b.setGraphic(new VBox(4, t, x));
        b.getStyleClass().add("modalidad");
        b.setUserData(tipo);
        b.setToggleGroup(modalidadErht);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private static VBox pasoErht(int n, String titulo, Node contenido) {
        Label numErht = new Label(String.valueOf(n));
        numErht.getStyleClass().add("paso-num");
        Label t = new Label(titulo);
        t.getStyleClass().add("paso-titulo");
        HBox cabErht = new HBox(10, numErht, t);
        cabErht.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(12, cabErht, contenido);
        v.getStyleClass().add("paso");
        return v;
    }

    private void elegirDanzaErht(Opcion d) {
        String carpetaErht = CatalogoRenderizados.carpetaDe(d.nombre(), rendersErht);
        if (carpetaErht == null) {
            throw new ExcepcionReglaNegocio("No hay render para " + d.nombre() + ".");
        }
        danzaErht = d;
        angulosErht.clear();
        for (String a : rendersErht.get(carpetaErht)) {
            angulosErht.put(a, RenderizadoCapas.cargar("render/" + carpetaErht + "/" + a));
        }
        nombresAngulosErht = List.copyOf(angulosErht.keySet());
        anguloErht = 0;

        partePorPrendaErht.clear();
        nombrePrendaErht.clear();
        coloresErht.clear();
        List<ParteTraje> partesErht = sErht.personalizador().partesDe(d.id());
        ColorTraje inicialErht = paletaErht.isEmpty() ? null : paletaErht.get(0);
        for (PrendaTenible p : angulosErht.get(nombresAngulosErht.get(0)).prendas()) {
            String k = CatalogoRenderizados.clave(p.nombre());
            nombrePrendaErht.put(k, p.nombre());
            partePorPrendaErht.put(k, partesErht.stream().filter(pt -> CatalogoRenderizados.clave(pt.nombre()).equals(k))
                    .findFirst().orElse(null));
            coloresErht.put(k, inicialErht == null ? new ColorElegido(Color.WHITE, null, "Blanco")
                    : new ColorElegido(Color.web(inicialErht.colorHex()), inicialErht.idColor(), inicialErht.nombre()));
        }
        coloresErht.keySet().forEach(this::tenirPrendaErht);

        opcionesPrendaErht.getChildren().clear();
        for (String k : nombrePrendaErht.keySet()) {
            ToggleButton b = new ToggleButton(nombrePrendaErht.get(k));
            b.getStyleClass().add("etiqueta-opcion-filtro");
            b.setUserData(k);
            if (partePorPrendaErht.get(k) == null) {
                b.setDisable(true);
                b.setTooltip(new Tooltip("La tienda aún no confecciona esta prenda para " + d.nombre() + "."));
            }
            b.setOnAction(e -> elegirPrendaErht(k));
            opcionesPrendaErht.getChildren().add(b);
        }
        String primeraErht = nombrePrendaErht.keySet().stream().filter(k -> partePorPrendaErht.get(k) != null).findFirst().orElse(null);
        prendaActualErht = null;
        if (primeraErht != null) {
            elegirPrendaErht(primeraErht);
        }
        mostrarAnguloErht();
        recalcularErht();
    }

    private void elegirPrendaErht(String k) {
        prendaActualErht = k;
        opcionesPrendaErht.getChildren().forEach(n -> ((ToggleButton) n).setSelected(k.equals(n.getUserData())));
        marcarColorErht();
    }

    private void elegirColorErht(ColorElegido c) {
        if (prendaActualErht == null) {
            return;
        }
        coloresErht.put(prendaActualErht, c);
        tenirPrendaErht(prendaActualErht);
        marcarColorErht();
        recalcularErht();
    }

    private void tenirPrendaErht(String clave) {
        Color c = coloresErht.get(clave).color();
        for (RenderizadoCapas r : angulosErht.values()) {
            for (PrendaTenible p : r.prendas()) {
                if (CatalogoRenderizados.clave(p.nombre()).equals(clave)) {
                    p.aplicarColor(c);
                }
            }
        }
    }

    private void marcarColorErht() {
        ColorElegido c = prendaActualErht == null ? null : coloresErht.get(prendaActualErht);
        for (Node n : circulosErht.getChildren()) {
            ColorTraje ct = (ColorTraje) n.getUserData();
            boolean igualErht = c != null && ct.idColor().equals(c.idColor());
            n.getStyleClass().remove("elegida");
            if (igualErht) {
                n.getStyleClass().add("elegida");
            }
        }
        if (c != null) {
            pintandoErht = true;
            selectorColorErht.setValue(c.color());
            pintandoErht = false;
            lColorErht.setText(nombrePrendaErht.get(prendaActualErht) + ": " + c.nombre() + " " + c.colorHex());
        }
    }

    private String tipoErht() {
        return modalidadErht.getSelectedToggle() == null ? "CONFECCION" : (String) modalidadErht.getSelectedToggle().getUserData();
    }

    private void recalcularErht() {
        lineasResumenErht.getChildren().clear();
        BigDecimal partesErht = BigDecimal.ZERO;
        for (Map.Entry<String, ParteTraje> e : partePorPrendaErht.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            ColorElegido c = coloresErht.get(e.getKey());
            Circle muestraErht = new Circle(6, c.color());
            muestraErht.getStyleClass().add("circulo-color");
            lineasResumenErht.getChildren().add(lineaErht(muestraErht, e.getValue().nombre() + " · " + c.nombre(),
                    UtilidadesInterfaz.soles(e.getValue().precioBase())));
            partesErht = partesErht.add(e.getValue().precioBase());
        }
        BigDecimal montoImplementosErht = BigDecimal.ZERO;
        for (Implemento impErht : implementosErht) {
            CheckBox cb = marcasImplementosErht.get(impErht.idImplemento());
            if (cb != null && cb.isSelected()) {
                int n = (Integer) cb.getUserData();
                BigDecimal subErht = impErht.precio().multiply(BigDecimal.valueOf(n));
                lineasResumenErht.getChildren().add(lineaErht(null, impErht.nombre() + (n > 1 ? " ×" + n : ""), UtilidadesInterfaz.soles(subErht)));
                montoImplementosErht = montoImplementosErht.add(subErht);
            }
        }
        BigDecimal costoErht = partesErht.add(montoImplementosErht);
        boolean alquilerErht = "ALQUILER".equals(tipoErht());
        BigDecimal t = (alquilerErht ? costoErht.multiply(FACTOR_ALQUILER) : costoErht).setScale(2, RoundingMode.HALF_UP);
        if (alquilerErht) {
            lineasResumenErht.getChildren().add(lineaErht(null, "Costo de confección " + UtilidadesInterfaz.soles(costoErht) + " · alquiler 30%", ""));
        }
        totalErht.setText(UtilidadesInterfaz.soles(t));
        enviarErht.setDisable(danzaErht == null || partePorPrendaErht.values().stream().allMatch(java.util.Objects::isNull));
    }

    private static HBox lineaErht(Node icono, String texto, String monto) {
        Label l = new Label(texto);
        l.getStyleClass().add("resumen-concepto");
        Label m = new Label(monto);
        m.getStyleClass().add("resumen-monto");
        HBox h = icono == null ? new HBox(8, l, UtilidadesInterfaz.espacio(), m) : new HBox(8, icono, l, UtilidadesInterfaz.espacio(), m);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void enviarDisenoErht() {
        int idClienteErht = Sesion.actual().idCliente();
        List<ParteElegida> partesErht = new ArrayList<>();
        partePorPrendaErht.forEach((k, parte) -> {
            if (parte != null) {
                ColorElegido c = coloresErht.get(k);
                partesErht.add(new ParteElegida(parte.idParte(), c.idColor(), c.colorHex()));
            }
        });
        Map<Integer, Integer> cantidadesImplementosErht = new LinkedHashMap<>();
        marcasImplementosErht.forEach((id, cb) -> {
            if (cb.isSelected()) {
                cantidadesImplementosErht.put(id, (Integer) cb.getUserData());
            }
        });
        String tallaErht = tallasErht.getSelectedToggle() == null ? null : (String) tallasErht.getSelectedToggle().getUserData();
        int id = sErht.personalizador().crearPedidoDiseno(idClienteErht, danzaErht.id(), tallaErht, tipoErht(), entregaErht.getValue(),
                partesErht, cantidadesImplementosErht, null);

        byte[] pngErht = UtilidadesInterfaz.aBytesPng(componerErht());
        sErht.personalizador().guardarImagen(id, idClienteErht, pngErht);
        try {
            Files.createDirectories(CARPETA_DISENOS);
            Path archivoErht = CARPETA_DISENOS.resolve("pedido_" + id + ".png");
            Files.write(archivoErht, pngErht);
            sErht.personalizador().guardarRutaImagen(id, idClienteErht, "disenos/" + archivoErht.getFileName());
        } catch (IOException e) {
        }
        portalErht.confirmar("¡Diseño enviado!",
                "Tu diseño N° " + id + " llegó a la tienda con un precio estimado de " + totalErht.getText()
                        + ". Te enviaremos el precio final para que lo apruebes en «Mis diseños».",
                new AnimacionConfirmacion.Opcion("Ver mis diseños", true, () -> portalErht.ir(VistaPortalCliente.MIS_DISENOS)),
                new AnimacionConfirmacion.Opcion("Diseñar otro", false, () -> portalErht.ir(VistaPortalCliente.DISENIO)));
    }

    private WritableImage componerErht() {
        RenderizadoCapas r = angulosErht.get(nombresAngulosErht.get(anguloErht));
        StackPane composicionErht = new StackPane(new ImageView(r.base()));
        for (PrendaTenible p : r.prendas()) {
            composicionErht.getChildren().add(new ImageView(p.imagen()));
        }
        composicionErht.setBackground(FONDO_ESTUDIO);
        composicionErht.resize(r.ancho(), r.alto());
        composicionErht.layout();
        SnapshotParameters parametrosErht = new SnapshotParameters();
        parametrosErht.setFill(Color.TRANSPARENT);
        return composicionErht.snapshot(parametrosErht, null);
    }

    private static int cantidadSugeridaErht(String implemento) {
        String k = CatalogoRenderizados.clave(implemento);
        if (k.contains("cascabel")) {
            return 24;
        }
        if (k.contains("lentejuela")) {
            return 200;
        }
        if (k.contains("espejo")) {
            return 10;
        }
        if (k.contains("pluma")) {
            return 6;
        }
        return 1;
    }
}
