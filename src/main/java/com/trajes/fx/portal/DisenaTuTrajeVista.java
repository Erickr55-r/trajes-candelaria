package com.trajes.fx.portal;

import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.Ui;
import com.trajes.fx.foto.CatalogoRender;
import com.trajes.fx.foto.PrendaTenible;
import com.trajes.fx.foto.RenderCapas;
import com.trajes.modelo.ColorTraje;
import com.trajes.modelo.Implemento;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.ParteElegida;
import com.trajes.modelo.ParteTraje;
import com.trajes.servicio.PersonalizadorService;
import com.trajes.servicio.ReglaNegocioException;
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
 */
public class DisenaTuTrajeVista extends HBox {

    /** Igual que el trigger fn_recalcular_pedido: ALQUILER paga el 30% del costo de confección. */
    private static final BigDecimal FACTOR_ALQUILER = PersonalizadorService.FACTOR_ALQUILER;
    private static final Path CARPETA_DISENOS = Path.of(System.getProperty("user.dir"), "disenos");

    /** Fondo de estudio para la imagen que se guarda (en pantalla lo pone el CSS .estudio). */
    private static final Background FONDO_ESTUDIO = new Background(new BackgroundFill(
            new RadialGradient(0, 0, 0.5, 0.42, 0.75, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#5A2A34")), new Stop(0.55, Color.web("#2B0F15")), new Stop(1, Color.web("#12070A"))),
            null, null));

    /** Color elegido para una prenda: de la paleta (con id) o libre (ColorPicker). */
    private record ColorElegido(Color color, Integer idColor, String nombre) {
        String hex() {
            return String.format("#%02X%02X%02X", Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255),
                    Math.round(color.getBlue() * 255));
        }
    }

    private final Servicios s;
    private final PortalClienteView portal;
    private final Map<String, List<String>> renders = CatalogoRender.danzas();
    private final List<ColorTraje> paleta;
    private final List<Implemento> implementos;

    // estado del diseño
    private Opcion danza;
    private final Map<String, RenderCapas> angulos = new LinkedHashMap<>();
    private List<String> nombresAngulos = List.of();
    private int angulo;
    /** clave de prenda ("pantalon") → parte_traje de la danza (null si la tienda no la cobra). */
    private final Map<String, ParteTraje> partePorPrenda = new LinkedHashMap<>();
    private final Map<String, String> nombrePrenda = new LinkedHashMap<>();
    private final Map<String, ColorElegido> colores = new LinkedHashMap<>();
    private String prendaActual;
    private final Map<Integer, CheckBox> marcasImplementos = new LinkedHashMap<>();

    // nodos
    private final StackPane escenario = new StackPane();
    private final StackPane capas = new StackPane();
    private final Label lAngulo = new Label();
    private final Button antes = new Button(null, Ui.icono(Feather.CHEVRON_LEFT, 22));
    private final Button despues = new Button(null, Ui.icono(Feather.CHEVRON_RIGHT, 22));
    private final Label aviso = new Label();
    private final FlowPane chipsDanza = new FlowPane(8, 8);
    private final FlowPane chipsPrenda = new FlowPane(8, 8);
    private final FlowPane circulos = new FlowPane(10, 10);
    private final ColorPicker picker = new ColorPicker(Color.WHITE);
    private final Label lColor = new Label();
    private final ToggleGroup modalidad = new ToggleGroup();
    private final ToggleGroup tallas = new ToggleGroup();
    private final DatePicker entrega = Ui.fechaPicker(LocalDate.now().plusDays(21));
    private final VBox lineasResumen = new VBox(6);
    private final Label total = new Label();
    private final Button enviar = new Button("Enviar mi diseño a la tienda", Ui.icono(Feather.SEND));
    private boolean pintando;

    public DisenaTuTrajeVista(Servicios s, PortalClienteView portal) {
        this.s = s;
        this.portal = portal;
        this.paleta = s.personalizador().colores();
        this.implementos = s.personalizador().implementos();
        getStyleClass().addAll("contenido", "disena");
        setSpacing(26);

        VBox izquierda = new VBox(14, Ui.encabezado("Personalizador", "Diseña tu traje",
                "Elige la danza, el color de cada prenda y los implementos. Te mostramos el precio estimado al instante."),
                estudio());
        HBox.setHgrow(izquierda, Priority.ALWAYS);
        VBox.setVgrow(escenario, Priority.ALWAYS);

        ScrollPane pasos = new ScrollPane(tarjetaPasos());
        pasos.setFitToWidth(true);
        pasos.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pasos.getStyleClass().add("pasos-scroll");
        pasos.setMinWidth(430);
        pasos.setPrefWidth(460);
        pasos.setMaxWidth(480);
        getChildren().addAll(izquierda, pasos);

        picker.valueProperty().addListener((o, a, b) -> {
            if (!pintando && b != null) {
                elegirColor(new ColorElegido(b, null, "Personalizado"));
            }
        });
        // primera danza con render
        Opcion primera = null;
        for (Node n : chipsDanza.getChildren()) {
            if (!n.isDisabled()) {
                primera = (Opcion) n.getUserData();
                ((ToggleButton) n).setSelected(true);
                break;
            }
        }
        if (primera != null) {
            elegirDanza(primera);
        } else {
            aviso.setText("Todavía no hay renders de trajes para personalizar.");
            aviso.setVisible(true);
        }
    }

    // ------------------------------------------------------------------ estudio (foto grande)

    private StackPane estudio() {
        escenario.getStyleClass().add("estudio");
        escenario.setMinSize(0, 0);
        escenario.setPrefSize(500, 560);
        escenario.setPadding(new Insets(24));
        escenario.getChildren().add(capas);

        antes.getStyleClass().add("flecha-angulo");
        despues.getStyleClass().add("flecha-angulo");
        antes.setOnAction(e -> girar(-1));
        despues.setOnAction(e -> girar(1));
        StackPane.setAlignment(antes, Pos.CENTER_LEFT);
        StackPane.setAlignment(despues, Pos.CENTER_RIGHT);
        lAngulo.getStyleClass().addAll("chip", "chip-banner");
        StackPane.setAlignment(lAngulo, Pos.BOTTOM_CENTER);
        aviso.getStyleClass().add("aviso-capa");
        aviso.setWrapText(true);
        aviso.setMaxWidth(420);
        aviso.setVisible(false);
        StackPane.setAlignment(aviso, Pos.TOP_CENTER);
        escenario.getChildren().addAll(antes, despues, lAngulo, aviso);
        return escenario;
    }

    private void mostrarAngulo() {
        capas.getChildren().clear();
        if (nombresAngulos.isEmpty()) {
            return;
        }
        RenderCapas r = angulos.get(nombresAngulos.get(angulo));
        capas.getChildren().add(vista(new ImageView(r.base())));
        for (PrendaTenible p : r.prendas()) {
            capas.getChildren().add(vista(new ImageView(p.imagen())));
        }
        boolean varios = nombresAngulos.size() > 1;
        antes.setVisible(varios);
        despues.setVisible(varios);
        String a = nombresAngulos.get(angulo);
        lAngulo.setText(a.substring(0, 1).toUpperCase(Locale.ROOT) + a.substring(1)
                + (varios ? "  ·  " + (angulo + 1) + " de " + nombresAngulos.size() : ""));
        // render sin transparencia: se avisa (se teñiría el cuadro entero)
        boolean opaco = r.prendas().stream().anyMatch(p -> p.fraccionTransparente() < 0.05);
        aviso.setText("Vista previa aproximada: este render aún no tiene fondo transparente.");
        aviso.setVisible(opaco);
    }

    private ImageView vista(ImageView iv) {
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.fitWidthProperty().bind(escenario.widthProperty().subtract(48));
        iv.fitHeightProperty().bind(escenario.heightProperty().subtract(48));
        return iv;
    }

    private void girar(int paso) {
        if (nombresAngulos.isEmpty()) {
            return;
        }
        angulo = Math.floorMod(angulo + paso, nombresAngulos.size());
        mostrarAngulo();
    }

    // ------------------------------------------------------------------ pasos

    private VBox tarjetaPasos() {
        for (Opcion d : s.trajes().danzas()) {
            ToggleButton b = new ToggleButton(d.nombre());
            b.getStyleClass().add("chip-filtro");
            b.setUserData(d);
            boolean hay = CatalogoRender.carpetaDe(d.nombre(), renders) != null;
            b.setDisable(!hay);
            if (!hay) {
                b.setTooltip(new Tooltip("Pronto podrás personalizar esta danza."));
            }
            b.setOnAction(e -> {
                b.setSelected(true);
                chipsDanza.getChildren().forEach(n -> {
                    if (n != b) {
                        ((ToggleButton) n).setSelected(false);
                    }
                });
                Ui.ejecutar(() -> elegirDanza(d));
            });
            chipsDanza.getChildren().add(b);
        }

        // paleta de la tabla color
        for (ColorTraje c : paleta) {
            Circle circ = new Circle(15, Color.web(c.hex()));
            circ.getStyleClass().add("circulo-color");
            StackPane boton = new StackPane(circ);
            boton.getStyleClass().add("muestra");
            boton.setUserData(c);
            Tooltip.install(boton, new Tooltip(c.nombre() + "  " + c.hex()));
            boton.setOnMouseClicked(e -> elegirColor(new ColorElegido(Color.web(c.hex()), c.idColor(), c.nombre())));
            circulos.getChildren().add(boton);
        }
        picker.setPrefWidth(200);
        lColor.getStyleClass().add("nota");
        Label otro = Ui.etiqueta("Otro color");
        otro.setMinWidth(Region.USE_PREF_SIZE);
        HBox filaPicker = new HBox(12, otro, picker);
        filaPicker.setAlignment(Pos.CENTER_LEFT);
        lColor.setWrapText(true);
        VBox lineaPicker = new VBox(8, filaPicker, lColor);

        // implementos
        VBox cajaImpl = new VBox(8);
        for (Implemento imp : implementos) {
            int n = cantidadSugerida(imp.nombre());
            CheckBox cb = new CheckBox(imp.nombre() + (n > 1 ? "  ×" + n : ""));
            cb.setUserData(n);
            Label precio = new Label(Ui.soles(imp.precio()) + (n > 1 ? " c/u" : ""));
            precio.getStyleClass().add("nota");
            cb.setOnAction(e -> recalcular());
            if (imp.parteDestino() != null) {
                cb.setTooltip(new Tooltip("Va en: " + imp.parteDestino()));
            }
            HBox fila = new HBox(8, cb, Ui.espacio(), precio);
            fila.setAlignment(Pos.CENTER_LEFT);
            cajaImpl.getChildren().add(fila);
            marcasImplementos.put(imp.idImplemento(), cb);
        }

        // modalidad, talla y entrega
        ToggleButton confeccion = tarjetaModalidad("CONFECCION", "Confeccionar", "El traje es tuyo", Feather.SCISSORS);
        ToggleButton alquilar = tarjetaModalidad("ALQUILER", "Alquilar", "30% del costo de confección", Feather.REPEAT);
        confeccion.setSelected(true);
        modalidad.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                modalidad.selectToggle(a);
            } else {
                recalcular();
            }
        });
        HBox cajaModalidad = new HBox(10, confeccion, alquilar);
        cajaModalidad.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        HBox cajaTallas = new HBox(6);
        for (String t : Ui.TALLAS) {
            ToggleButton tb = new ToggleButton(t);
            tb.getStyleClass().add("talla-chip");
            tb.setToggleGroup(tallas);
            tb.setUserData(t);
            if ("M".equals(t)) {
                tb.setSelected(true);
            }
            cajaTallas.getChildren().add(tb);
        }
        tallas.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) {
                tallas.selectToggle(a);
            }
        });
        entrega.setPrefWidth(170);
        HBox tallaEntrega = new HBox(16, new VBox(6, Ui.etiqueta("TALLA"), cajaTallas),
                new VBox(6, Ui.etiqueta("ENTREGA"), entrega));

        // resumen
        Label tituloResumen = new Label("Precio estimado");
        tituloResumen.getStyleClass().add("resumen-titulo");
        total.getStyleClass().add("resumen-total");
        enviar.getStyleClass().addAll("boton-primario", "boton-solicitar");
        enviar.setMaxWidth(Double.MAX_VALUE);
        enviar.setOnAction(e -> Ui.ejecutar(this::enviarDiseno));
        Label nota = new Label("Es un estimado: la tienda revisa tu diseño, te envía el precio final y tú lo apruebas.");
        nota.getStyleClass().add("nota");
        nota.setWrapText(true);
        Region sep = new Region();
        sep.getStyleClass().add("traje-separador");
        HBox lineaTotal = new HBox(10, new Label("Total estimado"), Ui.espacio(), total);
        lineaTotal.setAlignment(Pos.CENTER_LEFT);
        lineaTotal.getChildren().get(0).getStyleClass().add("resumen-total-texto");
        VBox resumen = new VBox(10, tituloResumen, lineasResumen, sep, lineaTotal, enviar, nota);
        resumen.getStyleClass().add("resumen-disena");

        VBox tarjeta = new VBox(18,
                paso(1, "Danza", chipsDanza),
                paso(2, "Prenda", chipsPrenda),
                paso(3, "Color", new VBox(12, circulos, lineaPicker)),
                paso(4, "Implementos", cajaImpl),
                paso(5, "Modalidad", new VBox(14, cajaModalidad, tallaEntrega)),
                resumen);
        tarjeta.getStyleClass().add("pasos-tarjeta");
        return tarjeta;
    }

    private ToggleButton tarjetaModalidad(String tipo, String titulo, String texto, Feather icono) {
        Label t = new Label(titulo, Ui.icono(icono, 16));
        t.getStyleClass().add("modalidad-titulo");
        Label x = new Label(texto);
        x.getStyleClass().add("nota");
        ToggleButton b = new ToggleButton();
        b.setGraphic(new VBox(4, t, x));
        b.getStyleClass().add("modalidad");
        b.setUserData(tipo);
        b.setToggleGroup(modalidad);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private static VBox paso(int n, String titulo, Node contenido) {
        Label num = new Label(String.valueOf(n));
        num.getStyleClass().add("paso-num");
        Label t = new Label(titulo);
        t.getStyleClass().add("paso-titulo");
        HBox cab = new HBox(10, num, t);
        cab.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(12, cab, contenido);
        v.getStyleClass().add("paso");
        return v;
    }

    // ------------------------------------------------------------------ lógica

    private void elegirDanza(Opcion d) {
        String carpeta = CatalogoRender.carpetaDe(d.nombre(), renders);
        if (carpeta == null) {
            throw new ReglaNegocioException("No hay render para " + d.nombre() + ".");
        }
        danza = d;
        angulos.clear();
        for (String a : renders.get(carpeta)) {
            angulos.put(a, RenderCapas.cargar("render/" + carpeta + "/" + a));
        }
        nombresAngulos = List.copyOf(angulos.keySet());
        angulo = 0;

        // prendas del primer ángulo ↔ partes de la danza (por nombre, sin tildes)
        partePorPrenda.clear();
        nombrePrenda.clear();
        colores.clear();
        List<ParteTraje> partes = s.personalizador().partesDe(d.id());
        ColorTraje inicial = paleta.isEmpty() ? null : paleta.get(0);
        for (PrendaTenible p : angulos.get(nombresAngulos.get(0)).prendas()) {
            String k = CatalogoRender.clave(p.nombre());
            nombrePrenda.put(k, p.nombre());
            partePorPrenda.put(k, partes.stream().filter(pt -> CatalogoRender.clave(pt.nombre()).equals(k))
                    .findFirst().orElse(null));
            colores.put(k, inicial == null ? new ColorElegido(Color.WHITE, null, "Blanco")
                    : new ColorElegido(Color.web(inicial.hex()), inicial.idColor(), inicial.nombre()));
        }
        colores.keySet().forEach(this::tenirPrenda);

        chipsPrenda.getChildren().clear();
        for (String k : nombrePrenda.keySet()) {
            ToggleButton b = new ToggleButton(nombrePrenda.get(k));
            b.getStyleClass().add("chip-filtro");
            b.setUserData(k);
            if (partePorPrenda.get(k) == null) {
                b.setDisable(true);
                b.setTooltip(new Tooltip("La tienda aún no confecciona esta prenda para " + d.nombre() + "."));
            }
            b.setOnAction(e -> elegirPrenda(k));
            chipsPrenda.getChildren().add(b);
        }
        String primera = nombrePrenda.keySet().stream().filter(k -> partePorPrenda.get(k) != null).findFirst().orElse(null);
        prendaActual = null;
        if (primera != null) {
            elegirPrenda(primera);
        }
        mostrarAngulo();
        recalcular();
    }

    private void elegirPrenda(String k) {
        prendaActual = k;
        chipsPrenda.getChildren().forEach(n -> ((ToggleButton) n).setSelected(k.equals(n.getUserData())));
        marcarColor();
    }

    private void elegirColor(ColorElegido c) {
        if (prendaActual == null) {
            return;
        }
        colores.put(prendaActual, c);
        tenirPrenda(prendaActual);
        marcarColor();
        recalcular();
    }

    /** Tiñe la prenda en todos los ángulos (así al girar se ve igual). */
    private void tenirPrenda(String clave) {
        Color c = colores.get(clave).color();
        for (RenderCapas r : angulos.values()) {
            for (PrendaTenible p : r.prendas()) {
                if (CatalogoRender.clave(p.nombre()).equals(clave)) {
                    p.aplicarColor(c);
                }
            }
        }
    }

    private void marcarColor() {
        ColorElegido c = prendaActual == null ? null : colores.get(prendaActual);
        for (Node n : circulos.getChildren()) {
            ColorTraje ct = (ColorTraje) n.getUserData();
            boolean igual = c != null && ct.idColor().equals(c.idColor());
            n.getStyleClass().remove("elegida");
            if (igual) {
                n.getStyleClass().add("elegida");
            }
        }
        if (c != null) {
            pintando = true;
            picker.setValue(c.color());
            pintando = false;
            lColor.setText(nombrePrenda.get(prendaActual) + ": " + c.nombre() + " " + c.hex());
        }
    }

    private String tipo() {
        return modalidad.getSelectedToggle() == null ? "CONFECCION" : (String) modalidad.getSelectedToggle().getUserData();
    }

    /** Misma fórmula que el trigger fn_recalcular_pedido. */
    private void recalcular() {
        lineasResumen.getChildren().clear();
        BigDecimal partes = BigDecimal.ZERO;
        for (Map.Entry<String, ParteTraje> e : partePorPrenda.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            ColorElegido c = colores.get(e.getKey());
            Circle muestra = new Circle(6, c.color());
            muestra.getStyleClass().add("circulo-color");
            lineasResumen.getChildren().add(linea(muestra, e.getValue().nombre() + " · " + c.nombre(),
                    Ui.soles(e.getValue().precioBase())));
            partes = partes.add(e.getValue().precioBase());
        }
        BigDecimal impl = BigDecimal.ZERO;
        for (Implemento imp : implementos) {
            CheckBox cb = marcasImplementos.get(imp.idImplemento());
            if (cb != null && cb.isSelected()) {
                int n = (Integer) cb.getUserData();
                BigDecimal sub = imp.precio().multiply(BigDecimal.valueOf(n));
                lineasResumen.getChildren().add(linea(null, imp.nombre() + (n > 1 ? " ×" + n : ""), Ui.soles(sub)));
                impl = impl.add(sub);
            }
        }
        BigDecimal costo = partes.add(impl);
        boolean alquiler = "ALQUILER".equals(tipo());
        BigDecimal t = (alquiler ? costo.multiply(FACTOR_ALQUILER) : costo).setScale(2, RoundingMode.HALF_UP);
        if (alquiler) {
            lineasResumen.getChildren().add(linea(null, "Costo de confección " + Ui.soles(costo) + " · alquiler 30%", ""));
        }
        total.setText(Ui.soles(t));
        enviar.setDisable(danza == null || partePorPrenda.values().stream().allMatch(java.util.Objects::isNull));
    }

    private static HBox linea(Node icono, String texto, String monto) {
        Label l = new Label(texto);
        l.getStyleClass().add("resumen-concepto");
        Label m = new Label(monto);
        m.getStyleClass().add("resumen-monto");
        HBox h = icono == null ? new HBox(8, l, Ui.espacio(), m) : new HBox(8, icono, l, Ui.espacio(), m);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void enviarDiseno() {
        int idCliente = Sesion.get().getCliente();
        List<ParteElegida> partes = new ArrayList<>();
        partePorPrenda.forEach((k, parte) -> {
            if (parte != null) {
                ColorElegido c = colores.get(k);
                partes.add(new ParteElegida(parte.idParte(), c.idColor(), c.hex()));
            }
        });
        Map<Integer, Integer> impl = new LinkedHashMap<>();
        marcasImplementos.forEach((id, cb) -> {
            if (cb.isSelected()) {
                impl.put(id, (Integer) cb.getUserData());
            }
        });
        String talla = tallas.getSelectedToggle() == null ? null : (String) tallas.getSelectedToggle().getUserData();
        int id = s.personalizador().crearPedidoDiseno(idCliente, danza.id(), talla, tipo(), entrega.getValue(),
                partes, impl, null);

        // imagen final: el ángulo que se está viendo, a la resolución del render
        byte[] png = Ui.png(componer());
        s.personalizador().guardarImagen(id, idCliente, png);
        try {
            Files.createDirectories(CARPETA_DISENOS);
            Path archivo = CARPETA_DISENOS.resolve("pedido_" + id + ".png");
            Files.write(archivo, png);
            s.personalizador().guardarRutaImagen(id, idCliente, "disenos/" + archivo.getFileName());
        } catch (IOException e) {
            // la imagen ya quedó en la base; la copia en disco es un respaldo
        }
        portal.confirmar("¡Diseño enviado!",
                "Tu diseño N° " + id + " llegó a la tienda con un precio estimado de " + total.getText()
                        + ". Te enviaremos el precio final para que lo apruebes en «Mis diseños».",
                new ConfirmacionCheck.Opcion("Ver mis diseños", true, () -> portal.ir(PortalClienteView.MIS_DISENOS)),
                new ConfirmacionCheck.Opcion("Diseñar otro", false, () -> portal.ir(PortalClienteView.DISENIO)));
    }

    private WritableImage componer() {
        RenderCapas r = angulos.get(nombresAngulos.get(angulo));
        StackPane composicion = new StackPane(new ImageView(r.base()));
        for (PrendaTenible p : r.prendas()) {
            composicion.getChildren().add(new ImageView(p.imagen()));
        }
        composicion.setBackground(FONDO_ESTUDIO);
        composicion.resize(r.ancho(), r.alto());
        composicion.layout();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return composicion.snapshot(params, null);
    }

    private static int cantidadSugerida(String implemento) {
        String k = CatalogoRender.clave(implemento);
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
