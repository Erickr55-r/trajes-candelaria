package com.trajes.fx;

import com.trajes.modelo.AlquilerResumen;
import com.trajes.servicio.ReglaNegocioException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** Ayudas comunes de la interfaz: formatos, tablas, estados, formularios y mensajes. */
public final class Ui {

    /** Temas disponibles: nombre visible -> hoja de estilos (cada tema importa estilos.css). */
    public static final java.util.Map<String, String> TEMAS = new java.util.LinkedHashMap<>();

    static {
        TEMAS.put("Guindo", Ui.class.getResource("/tema-guindo.css").toExternalForm());
        TEMAS.put("Marino", Ui.class.getResource("/tema-marino.css").toExternalForm());
    }

    /** Hoja de estilos del tema activo (la usan la ventana principal y todos los diálogos). */
    public static String CSS = TEMAS.get("Guindo");
    private static String temaActual = "Guindo";

    public static String temaActual() {
        return temaActual;
    }

    /** Cambia el tema en todas las ventanas abiertas y en las que se abran después. */
    public static void cambiarTema(String nombre) {
        String nueva = TEMAS.get(nombre);
        if (nueva == null || nueva.equals(CSS)) {
            return;
        }
        String anterior = CSS;
        CSS = nueva;
        temaActual = nombre;
        for (Window w : Window.getWindows()) {
            if (w.getScene() != null) {
                w.getScene().getStylesheets().replaceAll(h -> h.equals(anterior) ? nueva : h);
            }
        }
    }

    public static final List<String> METODOS = List.of("EFECTIVO", "YAPE", "PLIN", "TRANSFERENCIA");
    public static final List<String> TALLAS = List.of("XS", "S", "M", "L", "XL");

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Ui() {
    }

    // ------------------------------------------------------------------ formatos

    /** S/ 1,234.50 */
    public static String soles(BigDecimal monto) {
        if (monto == null) {
            return "—";
        }
        return "S/ " + String.format(Locale.US, "%,.2f", monto.setScale(2, RoundingMode.HALF_UP));
    }

    /** dd/MM/yyyy */
    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : fecha.format(FECHA);
    }

    /** Texto para cualquier valor (se usa en los reportes). */
    public static String texto(Object valor) {
        if (valor == null) {
            return "—";
        }
        if (valor instanceof BigDecimal bd) {
            return String.format(Locale.US, "%,.2f", bd.setScale(2, RoundingMode.HALF_UP));
        }
        if (valor instanceof java.sql.Date d) {
            return fecha(d.toLocalDate());
        }
        if (valor instanceof LocalDate d) {
            return fecha(d);
        }
        return valor.toString();
    }

    /** Lee un monto escrito por el usuario (acepta "1,234.50", "12,5" o "S/ 30"). Vacío = null. */
    public static BigDecimal monto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replace("S/", "").replace(" ", "").trim();
        limpio = limpio.contains(".") ? limpio.replace(",", "") : limpio.replace(",", ".");
        try {
            return new BigDecimal(limpio).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Monto no válido: " + texto.trim());
        }
    }

    /** Valor obligatorio de un campo de texto. */
    public static String requerido(TextField campo, String nombre) {
        String valor = campo.getText();
        if (valor == null || valor.isBlank()) {
            campo.requestFocus();
            throw new ReglaNegocioException("Completa el campo: " + nombre + ".");
        }
        return valor.trim();
    }

    /** Valor obligatorio de una lista desplegable. */
    public static <T> T requerido(ComboBox<T> combo, String nombre) {
        T valor = combo.getValue();
        if (valor == null) {
            combo.requestFocus();
            throw new ReglaNegocioException("Elige un valor en: " + nombre + ".");
        }
        return valor;
    }

    /** Fecha obligatoria de un DatePicker (confirma lo que se haya escrito a mano). */
    public static LocalDate requerido(DatePicker picker, String nombre) {
        LocalDate valor = fechaDe(picker);
        if (valor == null) {
            picker.requestFocus();
            throw new ReglaNegocioException("Elige la fecha: " + nombre + ".");
        }
        return valor;
    }

    /** Fecha de un DatePicker; si el texto se escribió a mano y no se confirmó, lo lee igual. */
    public static LocalDate fechaDe(DatePicker picker) {
        String escrito = picker.getEditor().getText();
        if (escrito != null && !escrito.isBlank()) {
            try {
                picker.setValue(LocalDate.parse(escrito.trim(), FECHA));
            } catch (DateTimeParseException e) {
                throw new ReglaNegocioException("Fecha no válida: " + escrito.trim() + " (usa dd/mm/aaaa).");
            }
        }
        return picker.getValue();
    }

    // ------------------------------------------------------------------ errores y mensajes

    /** Acción que puede lanzar errores del negocio o de la base de datos. */
    @FunctionalInterface
    public interface Accion {
        void ejecutar();
    }

    /** Ejecuta la acción y, si falla por una regla del negocio o la base de datos, muestra el error. */
    public static boolean ejecutar(Accion accion) {
        try {
            accion.ejecutar();
            return true;
        } catch (ReglaNegocioException | DataAccessException e) {
            error(mensaje(e));
            return false;
        }
    }

    /** Convierte un error en un mensaje legible (para PostgreSQL, la primera línea sin "ERROR:"). */
    public static String mensaje(Exception e) {
        if (e instanceof DataAccessException dae) {
            String m = primeraLinea(dae);
            if (m.contains("duplicate key")) {
                return "Ya existe un registro con ese código o documento.";
            }
            if ((m.contains("does not exist") || m.contains("no existe"))
                    && m.matches("(?s).*[«\"](parte_traje|color|implemento|pedido_\\w+|v_pedidos_resumen)[»\"].*")) {
                return "Faltan las tablas del personalizador: ejecuta db/personalizador.sql sobre la base "
                        + "alquiler_trajes (después de base_datos.sql).";
            }
            if ((m.contains("does not exist") || m.contains("no existe")) && m.contains("usuario")) {
                return "Falta la tabla de usuarios: ejecuta db/login_roles.sql sobre la base alquiler_trajes.";
            }
            if (m.contains("violates foreign key")) {
                return "No se puede eliminar: hay otros registros que dependen de este.";
            }
            return m;
        }
        return e.getMessage();
    }

    /** Primera línea del error más interno, sin el prefijo "ERROR:" de PostgreSQL. */
    public static String primeraLinea(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String m = causa.getMessage();
        if (m == null || m.isBlank()) {
            return causa.getClass().getSimpleName();
        }
        return m.split("\\R")[0].replaceFirst("^ERROR:\\s*", "");
    }

    public static void error(String mensaje) {
        alerta(Alert.AlertType.ERROR, "No se pudo completar la operación", mensaje).showAndWait();
    }

    public static void info(String mensaje) {
        alerta(Alert.AlertType.INFORMATION, null, mensaje).showAndWait();
    }

    public static boolean confirmar(String mensaje) {
        Alert a = alerta(Alert.AlertType.CONFIRMATION, null, mensaje);
        a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /**
     * Pide un texto en un diálogo (por ejemplo, el motivo de un rechazo o un precio). El botón Aceptar
     * solo se habilita cuando hay algo escrito. Devuelve vacío si se cancela.
     */
    public static java.util.Optional<String> pedirTexto(String titulo, String mensaje, String etiqueta,
                                                        String inicial, String ayuda) {
        javafx.scene.control.Dialog<ButtonType> d = new javafx.scene.control.Dialog<>();
        d.setTitle("Trajes Candelaria");
        d.setHeaderText(titulo);
        TextField campo = new TextField(inicial == null ? "" : inicial);
        campo.setPromptText(ayuda);
        campo.setPrefWidth(420);
        Label texto = new Label(mensaje);
        texto.setWrapText(true);
        texto.setMaxWidth(420);
        texto.getStyleClass().add("subtitulo");
        d.getDialogPane().setContent(new VBox(12, texto, etiqueta(etiqueta), campo));
        d.getDialogPane().getStylesheets().add(CSS);
        ButtonType aceptar = new ButtonType("Aceptar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, aceptar);
        javafx.scene.Node ok = d.getDialogPane().lookupButton(aceptar);
        ok.getStyleClass().add("boton-primario");
        ok.disableProperty().bind(campo.textProperty().isEmpty());
        Window activa = ventanaActiva();
        if (activa != null) {
            d.initOwner(activa);
        }
        javafx.application.Platform.runLater(campo::requestFocus);
        return d.showAndWait().filter(b -> b == aceptar).map(b -> campo.getText().trim()).filter(t -> !t.isEmpty());
    }

    private static Alert alerta(Alert.AlertType tipo, String cabecera, String mensaje) {
        Alert a = new Alert(tipo);
        a.setTitle("Alquiler de trajes");
        a.setHeaderText(cabecera);
        a.setContentText(mensaje);
        a.getDialogPane().getStylesheets().add(CSS);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Window activa = ventanaActiva();
        if (activa != null) {
            a.initOwner(activa);
        }
        return a;
    }

    private static Window ventanaActiva() {
        return Window.getWindows().stream().filter(Window::isFocused).findFirst()
                .orElse(Window.getWindows().isEmpty() ? null : Window.getWindows().get(0));
    }

    /** Ventana modal con el estilo de la aplicación. */
    public static Stage ventana(String titulo, Parent raiz, double ancho, double alto) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        Window duenia = ventanaActiva();
        if (duenia != null) {
            stage.initOwner(duenia);
        }
        Scene escena = new Scene(raiz, ancho, alto);
        escena.getStylesheets().add(CSS);
        stage.setTitle(titulo);
        stage.setScene(escena);
        return stage;
    }

    // ------------------------------------------------------------------ estados

    /** Clase de color para cada estado. */
    public static String colorEstado(String estado) {
        if (estado == null) {
            return "badge-gris";
        }
        return switch (estado) {
            case "DISPONIBLE", "DEVUELTO", "BUENO", "LISTO" -> "badge-verde";
            case "RESERVADO", "PENDIENTE", "APROBADO" -> "badge-azul";
            case "SOLICITADO", "COTIZADO", "ADMIN" -> "badge-morado";
            case "VENDEDOR" -> "badge-azul";
            case "ACTIVO" -> "badge-verde";
            case "ENTREGADO", "MANTENIMIENTO", "LEVE", "REGULAR", "EN_CONFECCION" -> "badge-naranja";
            case "DANADO", "GRAVE", "PERDIDA", "PERDIDO", "VENCIDO", "RECHAZADO" -> "badge-rojo";
            default -> "badge-gris";
        };
    }

    public static Label badge(String estado) {
        Label l = new Label(estado == null ? "—" : estado);
        l.getStyleClass().addAll("badge", colorEstado(estado));
        return l;
    }

    public static boolean vencido(AlquilerResumen a) {
        return "ENTREGADO".equals(a.estado()) && a.fechaDevolucionPrevista() != null
                && a.fechaDevolucionPrevista().isBefore(LocalDate.now());
    }

    /** Estado del alquiler; si está entregado y ya pasó la fecha, se agrega VENCIDO. */
    public static HBox badgesAlquiler(AlquilerResumen a) {
        HBox caja = new HBox(4, badge(a.estado()));
        if (vencido(a)) {
            caja.getChildren().add(badge("VENCIDO"));
        }
        caja.setAlignment(Pos.CENTER_LEFT);
        return caja;
    }

    // ------------------------------------------------------------------ tablas

    public static <S, T> TableColumn<S, T> columna(String titulo, Function<S, T> valor,
                                                   Function<? super T, String> formato, double ancho) {
        TableColumn<S, T> col = new TableColumn<>(cabeceraColumna(titulo));
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formato.apply(item));
            }
        });
        if (ancho > 0) {
            col.setPrefWidth(ancho);
        }
        return col;
    }

    public static <S, T> TableColumn<S, T> colTexto(String titulo, Function<S, T> valor, double ancho) {
        return columna(titulo, valor, Ui::texto, ancho);
    }

    public static <S, T extends Number> TableColumn<S, T> colNumero(String titulo, Function<S, T> valor, double ancho) {
        TableColumn<S, T> col = columna(titulo, valor, Ui::texto, ancho);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    public static <S> TableColumn<S, BigDecimal> colSoles(String titulo, Function<S, BigDecimal> valor, double ancho) {
        TableColumn<S, BigDecimal> col = columna(titulo, valor, Ui::soles, ancho);
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }

    public static <S> TableColumn<S, LocalDate> colFecha(String titulo, Function<S, LocalDate> valor, double ancho) {
        return columna(titulo, valor, Ui::fecha, ancho);
    }

    public static <S> TableColumn<S, String> colEstado(String titulo, Function<S, String> valor, double ancho) {
        TableColumn<S, String> col = new TableColumn<>(cabeceraColumna(titulo));
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valor.apply(c.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty ? null : badge(item));
            }
        });
        col.setPrefWidth(ancho);
        return col;
    }

    /** Columna de estado de un alquiler (con VENCIDO si corresponde). */
    public static TableColumn<AlquilerResumen, AlquilerResumen> colEstadoAlquiler(double ancho) {
        TableColumn<AlquilerResumen, AlquilerResumen> col = new TableColumn<>(cabeceraColumna("Estado"));
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(AlquilerResumen item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : badgesAlquiler(item));
            }
        });
        col.setComparator((a, b) -> a.estado().compareTo(b.estado()));
        col.setPrefWidth(Math.max(ancho, 200));
        col.setMinWidth(190);
        return col;
    }

    /** Ejecuta la acción al hacer doble clic en una fila. */
    public static <S> void alDobleClic(TableView<S> tabla, Consumer<S> accion) {
        tabla.setRowFactory(t -> {
            TableRow<S> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) {
                    accion.accept(fila.getItem());
                }
            });
            return fila;
        });
    }

    public static <S> TableView<S> tabla(String sinDatos) {
        TableView<S> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label(sinDatos));
        return t;
    }

    // ------------------------------------------------------------------ controles

    /**
     * Lista desplegable. Si {@code vacio} no es null, la primera opción es null y se muestra con ese texto
     * (por ejemplo "Todas" o "— Sin conjunto —").
     */
    public static <T> ComboBox<T> combo(List<T> items, Function<T, String> texto, String vacio) {
        List<T> lista = new ArrayList<>();
        if (vacio != null) {
            lista.add(null);
        }
        lista.addAll(items);
        ComboBox<T> combo = new ComboBox<>();
        combo.getItems().setAll(lista);
        combo.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? vacio : texto.apply(item));
            }
        });
        combo.setButtonCell(combo.getCellFactory().call(null));
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? (vacio == null ? "" : vacio) : texto.apply(item);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        });
        if (vacio != null) {
            combo.setPromptText(vacio);
            combo.getSelectionModel().selectFirst();
        } else if (!items.isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
        combo.setVisibleRowCount(14);
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    public static ComboBox<String> comboTexto(List<String> items, String vacio) {
        return combo(items, s -> s, vacio);
    }

    /** DatePicker que muestra y lee fechas como dd/MM/yyyy. */
    public static DatePicker fechaPicker(LocalDate inicial) {
        DatePicker p = new DatePicker(inicial);
        p.setPromptText("dd/mm/aaaa");
        p.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.format(FECHA);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(s.trim(), FECHA);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
        p.setMaxWidth(Double.MAX_VALUE);
        return p;
    }

    public static TextField campo(String valor, String ayuda) {
        TextField t = new TextField(valor == null ? "" : valor);
        t.setPromptText(ayuda);
        return t;
    }

    /** Texto de un monto para un campo editable (sin "S/"). */
    public static String montoEditable(BigDecimal monto) {
        return monto == null ? "" : monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ------------------------------------------------------------------ bloques visuales

    /** Imagen de JavaFX a bytes PNG (para guardarla en la base). */
    public static byte[] png(javafx.scene.image.WritableImage imagen) {
        int w = (int) imagen.getWidth();
        int h = (int) imagen.getHeight();
        int[] px = new int[w * h];
        imagen.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, w, h, px, 0, w);
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(bi, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new ReglaNegocioException("No se pudo preparar la imagen del diseño: " + e.getMessage());
        }
    }

    /** Encabezado de columna: mayúsculas pequeñas, como en el resto del diseño. */
    public static String cabeceraColumna(String titulo) {
        return titulo == null ? null : titulo.toUpperCase(Locale.ROOT);
    }

    /** Ícono Feather (Ikonli). El color y el tamaño vienen del CSS. */
    public static FontIcon icono(Ikon ikon) {
        return new FontIcon(ikon);
    }

    public static FontIcon icono(Ikon ikon, int tamanio) {
        FontIcon i = new FontIcon(ikon);
        i.setIconSize(tamanio);
        return i;
    }

    /**
     * Texto en MAYÚSCULAS con espaciado de letras. JavaFX no tiene letter-spacing en CSS, así que se
     * intercala un espacio fino (U+2009) entre letras.
     */
    public static String espaciado(String texto) {
        String mayus = texto.toUpperCase(new Locale("es", "PE"));
        StringBuilder sb = new StringBuilder(mayus.length() * 2);
        for (int i = 0; i < mayus.length(); i++) {
            if (i > 0) {
                sb.append('\u2009');
            }
            sb.append(mayus.charAt(i));
        }
        return sb.toString();
    }

    /** Subtítulo de pantalla: 13 px, mayúsculas, dorado y con espaciado de letras. */
    public static Label sobretitulo(String texto) {
        Label l = new Label(espaciado(texto));
        l.getStyleClass().add("sobretitulo");
        return l;
    }

    public static Label sobretitulo(String texto, Ikon ikon) {
        Label l = sobretitulo(texto);
        l.setGraphic(icono(ikon));
        l.setGraphicTextGap(8);
        return l;
    }

    /** Encabezado de pantalla: subtítulo dorado, título serif y una línea de ayuda opcional. */
    public static VBox encabezado(String sobretitulo, String titulo, String descripcion) {
        VBox caja = new VBox(sobretitulo(sobretitulo), titulo(titulo));
        if (descripcion != null && !descripcion.isBlank()) {
            caja.getChildren().add(subtitulo(descripcion));
        }
        caja.getStyleClass().add("encabezado");
        return caja;
    }

    /** Tarjeta de indicador con ícono (pantalla de inicio). */
    public static HBox tarjeta(String titulo, String valor, String color, Ikon ikon) {
        StackPane cuadro = new StackPane(icono(ikon));
        cuadro.getStyleClass().add("tarjeta-icono");
        Label t = new Label(titulo);
        t.getStyleClass().add("tarjeta-titulo");
        t.setWrapText(true);
        Label v = new Label(valor);
        v.getStyleClass().add("tarjeta-valor");
        VBox textos = new VBox(2, t, v);
        HBox caja = new HBox(14, cuadro, textos);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().addAll("tarjeta", "con-icono", color);
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    public static Label titulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("titulo");
        return l;
    }

    public static Label subtitulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("subtitulo");
        l.setWrapText(true);
        return l;
    }

    public static Label seccion(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("seccion");
        return l;
    }

    public static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta");
        return l;
    }

    /** Tarjeta con un indicador (color: verde, azul, naranja, rojo, gris, dorado, granate). */
    public static VBox tarjeta(String titulo, String valor, String color) {
        Label t = new Label(titulo);
        t.getStyleClass().add("tarjeta-titulo");
        t.setWrapText(true);
        Label v = new Label(valor);
        v.getStyleClass().add("tarjeta-valor");
        VBox caja = new VBox(4, t, v);
        caja.getStyleClass().addAll("tarjeta", color);
        HBox.setHgrow(caja, Priority.ALWAYS);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    /** Panel blanco con borde. */
    public static VBox panel(Node... hijos) {
        VBox caja = new VBox(10, hijos);
        caja.getStyleClass().add("panel");
        return caja;
    }

    /** Rejilla de formulario de dos columnas (etiqueta | control). */
    public static GridPane formulario() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setMinWidth(220);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    public static void fila(GridPane g, int fila, String etiqueta, Node control) {
        g.add(etiqueta(etiqueta), 0, fila);
        g.add(control, 1, fila);
    }

    public static Region espacio() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static <B extends javafx.scene.control.ButtonBase> B estilo(B boton, String clase) {
        boton.getStyleClass().add(clase);
        return boton;
    }
}
