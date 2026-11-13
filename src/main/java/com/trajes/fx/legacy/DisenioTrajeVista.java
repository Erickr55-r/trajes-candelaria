package com.trajes.fx.legacy;

import com.trajes.fx.legacy.tresd.Maniqui3D;
import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;

import com.trajes.fx.PanelVendedorView;
import com.trajes.fx.legacy.tresd.Visor3D;
import com.trajes.modelo.ColorTraje;
import com.trajes.modelo.Implemento;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.ParteTraje;
import com.trajes.servicio.PersonalizadorService;
import com.trajes.servicio.ReglaNegocioException;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LEGADO: personalizador 3D con figuras básicas (TriangleMesh). Ya no aparece en ningún menú;
 * se reemplazó por el personalizador por capas (fx/portal/DisenaTuTrajeVista). Se conserva por si acaso.
 */
public class DisenioTrajeVista extends VBox {

    private static final String VARON = "Varón";
    private static final String CHOLITA = "Cholita (pollera)";

    /** Color con el que empieza cada parte. */
    private static final Map<String, String> COLOR_INICIAL = Map.of(
            "chaqueta", "Rojo", "pantalon", "Negro", "pollera", "Rosado", "botas", "Blanco",
            "sombrero", "Blanco", "capa", "Morado", "mascara", "Rojo", "faja", "Dorado");

    private final Servicios s;
    private final PanelVendedorView ventana;
    private final List<ColorTraje> colores;

    private final ComboBox<Opcion> cliente;
    private final ComboBox<Opcion> danza;
    private final ComboBox<String> figura = Ui.comboTexto(List.of(VARON, CHOLITA), null);
    private final ComboBox<String> talla = Ui.comboTexto(Ui.TALLAS, null);
    private final ComboBox<String> tipo = Ui.comboTexto(PersonalizadorService.TIPOS, null);
    private final DatePicker entrega = Ui.fechaPicker(LocalDate.now().plusDays(15));
    private final TextField observacion = Ui.campo("", "Opcional");

    private final Visor3D visor = new Visor3D(460, 470);
    private final VBox cajaPartes = new VBox(8);
    private final List<FilaParte> filasPartes = new ArrayList<>();
    private final List<FilaImplemento> filasImplementos = new ArrayList<>();

    private final Label lPartes = new Label();
    private final Label lImplementos = new Label();
    private final Label lCosto = new Label();
    private final Label lAlquiler = new Label();
    private final Label lTotal = new Label();
    private final TextField adelanto = Ui.campo("", "0.00");

    /** Una parte del traje: si se incluye y con qué color. */
    private static final class FilaParte {
        ParteTraje parte;
        String clave;
        CheckBox incluir;
        ColorTraje color;
        Label lColor;
        final List<Button> botones = new ArrayList<>();
        VBox nodo;

        boolean visible() {
            return nodo.isVisible();
        }
    }

    private static final class FilaImplemento {
        Implemento implemento;
        CheckBox marca;
        Spinner<Integer> cantidad;
    }

    /** Cliente fijo del portal (null en la tienda, donde se elige de la lista). */
    private final Integer idClienteFijo;
    /** Portal: qué hacer después de enviar el diseño (recibe el N° de pedido). */
    private final java.util.function.IntConsumer alEnviar;

    public DisenioTrajeVista(Servicios s, PanelVendedorView ventana) {
        this(s, ventana, null, null);
    }

    /** Personalizador del portal: el pedido se crea a nombre del cliente de la sesión, sin adelanto. */
    public static DisenioTrajeVista paraCliente(Servicios s, java.util.function.IntConsumer alEnviar) {
        return new DisenioTrajeVista(s, null, com.trajes.fx.Sesion.get().getCliente(), alEnviar);
    }

    private DisenioTrajeVista(Servicios s, PanelVendedorView ventana, Integer idClienteFijo,
                              java.util.function.IntConsumer alEnviar) {
        this.s = s;
        this.ventana = ventana;
        this.idClienteFijo = idClienteFijo;
        this.alEnviar = alEnviar;
        getStyleClass().add("contenido");

        colores = s.personalizador().colores();
        // En el portal no se carga la lista de clientes: el cliente es siempre el de la sesión.
        cliente = Ui.combo(idClienteFijo == null ? s.clientes().opciones() : List.of(), Opcion::nombre, null);
        cliente.getSelectionModel().clearSelection();
        cliente.setPromptText("Elige un cliente...");
        danza = Ui.combo(s.personalizador().danzas(), Opcion::nombre, null);
        talla.setValue("M");
        danza.setOnAction(e -> Ui.ejecutar(this::cargarPartes));
        figura.setOnAction(e -> {
            aplicarFigura();
            actualizar();
        });
        tipo.setOnAction(e -> recalcular());

        HBox columnas = new HBox(14, columnaDatos(), columnaVisor(), columnaPersonalizar());
        VBox.setVgrow(columnas, Priority.ALWAYS);
        getChildren().addAll(Ui.encabezado("Personalizador 3D", "Diseña tu traje",
                        idClienteFijo == null
                                ? "Elige el color de cada parte y los implementos: el maniquí cambia al instante."
                                : "Elige el color de cada parte y los implementos. Cuando te guste, envíalo a la "
                                        + "tienda y te enviaremos el precio para que lo apruebes."),
                columnas, totales(), acciones());

        // Al salir de la pantalla se detiene el giro automático.
        sceneProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                visor.detener();
            }
        });
        cargarPartes();
    }

    // ------------------------------------------------------------------ columnas

    private VBox columnaDatos() {
        observacion.setPromptText("Ej.: con iniciales bordadas");
        VBox caja = Ui.panel(Ui.seccion("Datos del pedido"),
                Ui.etiqueta("Cliente *"), cliente,
                Ui.etiqueta("Danza *"), danza,
                Ui.etiqueta("Figura"), figura,
                Ui.etiqueta("Talla *"), talla,
                Ui.etiqueta("Tipo *"), tipo,
                Ui.etiqueta("Fecha de entrega *"), entrega,
                Ui.etiqueta("Observación"), observacion,
                Ui.subtitulo("ALQUILER: el traje se confecciona para la fecha y se paga el 30% del costo. "
                        + "CONFECCION: el traje queda para el cliente."));
        if (idClienteFijo != null) {
            caja.getChildren().subList(1, 3).clear();   // sin "Cliente *" ni su lista
        }
        caja.setSpacing(6);
        caja.setPrefWidth(250);
        caja.setMinWidth(230);
        return caja;
    }

    private VBox columnaVisor() {
        VBox caja = Ui.panel(Ui.seccion("Vista 3D"), visor);
        VBox.setVgrow(visor, Priority.ALWAYS);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    private ScrollPane columnaPersonalizar() {
        VBox cajaImplementos = new VBox(6);
        for (Implemento imp : s.personalizador().implementos()) {
            FilaImplemento f = new FilaImplemento();
            f.implemento = imp;
            f.marca = new CheckBox(imp.nombre() + "  ·  " + Ui.soles(imp.precio()) + " c/u");
            f.marca.setTooltip(new Tooltip("Va en: " + imp.parteDestino()));
            f.cantidad = new Spinner<>(1, 999, cantidadSugerida(imp.nombre()));
            f.cantidad.setEditable(true);
            f.cantidad.setPrefWidth(78);
            f.cantidad.setDisable(true);
            // Lo que se escribe a mano también cuenta, sin tener que pulsar Enter.
            f.cantidad.getEditor().textProperty().addListener((o, a, b) -> {
                try {
                    int n = Integer.parseInt(b.trim());
                    if (n >= 1 && n <= 999) {
                        f.cantidad.getValueFactory().setValue(n);
                    }
                } catch (NumberFormatException ignorado) {
                    // se queda con el último valor válido
                }
            });
            f.marca.setOnAction(e -> {
                f.cantidad.setDisable(!f.marca.isSelected());
                actualizarImplementos();
            });
            f.cantidad.valueProperty().addListener((o, a, b) -> actualizarImplementos());
            HBox fila = new HBox(8, f.marca, Ui.espacio(), f.cantidad);
            fila.setAlignment(Pos.CENTER_LEFT);
            cajaImplementos.getChildren().add(fila);
            filasImplementos.add(f);
        }

        VBox contenido = new VBox(10, Ui.seccion("Colores por parte"),
                Ui.subtitulo("Desmarca una parte si el traje no la lleva."), cajaPartes,
                Ui.seccion("Implementos"), Ui.subtitulo("Marca un implemento para verlo en el maniquí."),
                cajaImplementos);
        contenido.getStyleClass().add("panel");
        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(340);
        scroll.setMinWidth(300);
        return scroll;
    }

    private HBox totales() {
        lTotal.getStyleClass().add("total-grande");
        lAlquiler.getStyleClass().add("descuento");
        HBox caja = new HBox(24, lPartes, lImplementos, lCosto, lAlquiler, Ui.espacio(), lTotal);
        caja.getStyleClass().add("totales");
        return caja;
    }

    private HBox acciones() {
        adelanto.setPrefWidth(110);
        Button imagen = new Button("Guardar diseño como imagen");
        imagen.setOnAction(e -> Ui.ejecutar(this::guardarImagen));
        Button registrar = Ui.estilo(new Button(idClienteFijo == null ? "Registrar pedido" : "Enviar mi diseño a la tienda",
                com.trajes.fx.Ui.icono(org.kordamp.ikonli.feather.Feather.SEND)), "boton-primario");
        registrar.setOnAction(e -> Ui.ejecutar(this::registrar));
        HBox caja = idClienteFijo == null
                ? new HBox(10, Ui.etiqueta("Adelanto S/"), adelanto, Ui.espacio(), imagen, registrar)
                : new HBox(10, Ui.subtitulo("El pedido queda PENDIENTE: la tienda revisa tu diseño y te cotiza."),
                        Ui.espacio(), imagen, registrar);
        if (idClienteFijo != null) {
            registrar.getStyleClass().add("boton-grande");
        }
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("barra-filtros");
        return caja;
    }

    // ------------------------------------------------------------------ partes y colores

    /** Carga las partes de la danza elegida y arma el maniquí con los colores iniciales. */
    private void cargarPartes() {
        filasPartes.clear();
        cajaPartes.getChildren().clear();
        Opcion d = danza.getValue();
        if (d == null) {
            cajaPartes.getChildren().add(Ui.subtitulo("No hay danzas con partes: ejecuta db/personalizador.sql."));
            actualizar();
            return;
        }
        for (ParteTraje p : s.personalizador().partesDe(d.id())) {
            FilaParte f = new FilaParte();
            f.parte = p;
            f.clave = Maniqui3D.clave(p.nombre());
            f.incluir = new CheckBox(p.nombre() + "  ·  " + Ui.soles(p.precioBase()));
            f.incluir.setSelected(true);
            f.incluir.setOnAction(e -> actualizar());
            f.lColor = Ui.subtitulo("");
            HBox cabecera = new HBox(8, f.incluir, Ui.espacio(), f.lColor);
            cabecera.setAlignment(Pos.CENTER_LEFT);

            FlowPane paleta = new FlowPane(6, 6);
            for (ColorTraje c : colores) {
                Button b = new Button();
                b.getStyleClass().add("color-boton");
                b.setStyle("-fx-background-color: " + c.hex() + ";");
                b.setTooltip(new Tooltip(c.nombre()));
                b.setOnAction(e -> elegirColor(f, c));
                f.botones.add(b);
                paleta.getChildren().add(b);
            }
            f.nodo = new VBox(6, cabecera, paleta);
            f.nodo.getStyleClass().add("parte-fila");
            filasPartes.add(f);
            cajaPartes.getChildren().add(f.nodo);

            String inicial = COLOR_INICIAL.getOrDefault(f.clave, "Granate");
            elegirColor(f, colores.stream().filter(c -> c.nombre().equals(inicial)).findFirst()
                    .orElse(colores.get(filasPartes.size() % colores.size())));
        }

        boolean tienePollera = filasPartes.stream().anyMatch(f -> f.clave.equals("pollera"));
        figura.setDisable(!tienePollera);
        if (!tienePollera) {
            figura.setValue(VARON);
        }
        aplicarFigura();
        actualizar();
    }

    /** Varón usa pantalón; cholita usa pollera y no lleva máscara. Lo oculto no se cobra. */
    private void aplicarFigura() {
        boolean cholita = CHOLITA.equals(figura.getValue());
        for (FilaParte f : filasPartes) {
            boolean visible = cholita
                    ? !f.clave.equals("pantalon") && !f.clave.equals("mascara")
                    : !f.clave.equals("pollera");
            f.nodo.setVisible(visible);
            f.nodo.setManaged(visible);
        }
    }

    private void elegirColor(FilaParte f, ColorTraje c) {
        f.color = c;
        f.lColor.setText(c.nombre());
        for (int i = 0; i < colores.size(); i++) {
            f.botones.get(i).getStyleClass().remove("elegido");
            if (colores.get(i).idColor().equals(c.idColor())) {
                f.botones.get(i).getStyleClass().add("elegido");
            }
        }
        visor.maniqui().colorear(f.parte.nombre(), Color.web(c.hex()));
    }

    private List<FilaParte> partesIncluidas() {
        return filasPartes.stream().filter(f -> f.visible() && f.incluir.isSelected()).toList();
    }

    private List<FilaImplemento> implementosMarcados() {
        return filasImplementos.stream().filter(f -> f.marca.isSelected()).toList();
    }

    /** Vuelve a armar el maniquí con las partes incluidas y recalcula el precio. */
    private void actualizar() {
        Opcion d = danza.getValue();
        visor.maniqui().armar(d == null ? null : d.nombre(),
                partesIncluidas().stream().map(f -> f.parte.nombre()).toList());
        actualizarImplementos();
    }

    private void actualizarImplementos() {
        visor.maniqui().mostrarImplementos(implementosMarcados().stream()
                .map(f -> new Maniqui3D.Adorno(f.implemento.nombre(), f.implemento.parteDestino(),
                        f.cantidad.getValue()))
                .toList());
        recalcular();
    }

    /** Precio en vivo: mismo cálculo que el trigger trg_recalcular_pedido_* de la base de datos. */
    private void recalcular() {
        BigDecimal partes = partesIncluidas().stream().map(f -> f.parte.precioBase())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal implementos = implementosMarcados().stream()
                .map(f -> f.implemento.precio().multiply(BigDecimal.valueOf(f.cantidad.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costo = partes.add(implementos);
        boolean alquiler = "ALQUILER".equals(tipo.getValue());
        BigDecimal total = alquiler
                ? costo.multiply(PersonalizadorService.FACTOR_ALQUILER).setScale(2, RoundingMode.HALF_UP)
                : costo;

        lPartes.setText("Partes (" + partesIncluidas().size() + "): " + Ui.soles(partes));
        lImplementos.setText("Implementos: " + Ui.soles(implementos));
        lCosto.setText("Costo de confección: " + Ui.soles(costo));
        lAlquiler.setText(alquiler ? "Alquiler = 30% del costo" : "");
        lTotal.setText("Total: " + Ui.soles(total));
    }

    private static int cantidadSugerida(String implemento) {
        String k = Maniqui3D.clave(implemento);
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

    // ------------------------------------------------------------------ acciones

    private void guardarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar diseño como imagen");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
        Opcion d = danza.getValue();
        fc.setInitialFileName("diseno_" + (d == null ? "traje" : Maniqui3D.clave(d.nombre()).replace(' ', '_')) + ".png");
        File archivo = fc.showSaveDialog(getScene().getWindow());
        if (archivo == null) {
            return;
        }
        if (!archivo.getName().toLowerCase().endsWith(".png")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".png");
        }
        visor.guardarPng(archivo);
        Ui.info("Diseño guardado en:\n" + archivo.getAbsolutePath());
    }

    private void registrar() {
        int idCliente = idClienteFijo != null ? idClienteFijo : Ui.requerido(cliente, "Cliente").id();
        Opcion d = Ui.requerido(danza, "Danza");
        String t = Ui.requerido(talla, "Talla");
        String tp = Ui.requerido(tipo, "Tipo");
        LocalDate fecha = Ui.requerido(entrega, "Fecha de entrega");
        if (fecha.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de entrega no puede ser anterior a hoy.");
        }
        Map<Integer, Integer> partes = new LinkedHashMap<>();
        for (FilaParte f : partesIncluidas()) {
            partes.put(f.parte.idParte(), f.color.idColor());
        }
        if (partes.isEmpty()) {
            throw new ReglaNegocioException("Incluye al menos una parte del traje.");
        }
        Map<Integer, Integer> implementos = new LinkedHashMap<>();
        for (FilaImplemento f : implementosMarcados()) {
            implementos.put(f.implemento.idImplemento(), f.cantidad.getValue());
        }

        if (idClienteFijo != null) {
            // Portal: sin adelanto; se guarda la imagen del diseño para la tienda.
            int id = s.personalizador().crearPedido(idCliente, d.id(), t, tp, fecha, partes, implementos,
                    null, observacion.getText());
            s.personalizador().guardarImagen(id, idCliente, Ui.png(visor.captura(1.5)));
            alEnviar.accept(id);
            return;
        }
        int id = s.personalizador().crearPedido(idCliente, d.id(), t, tp, fecha, partes, implementos,
                Ui.monto(adelanto.getText()), observacion.getText());
        Ui.info("Pedido personalizado N° " + id + " registrado.\n\nTotal: "
                + Ui.soles(s.personalizador().buscarPedido(id).montoTotal()));
        new com.trajes.fx.vistas.PedidoDisenoDialogo(s, id).mostrar();
        ventana.mostrar(PanelVendedorView.PEDIDOS);
    }
}
