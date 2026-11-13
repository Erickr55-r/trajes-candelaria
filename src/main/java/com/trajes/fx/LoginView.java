package com.trajes.fx;

import com.trajes.modelo.Usuario;
import com.trajes.servicio.ReglaNegocioException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.springframework.dao.DataAccessException;

import java.util.function.Consumer;

/**
 * Inicio de sesión: a la izquierda el panel de marca (degradado guindo, chakana dorada), a la derecha la
 * tarjeta para ingresar o crear una cuenta de cliente.
 */
public class LoginView extends HBox {

    private final Servicios s;
    private final Consumer<Usuario> alIngresar;
    private final StackPane derecha = new StackPane();

    public LoginView(Servicios s, Consumer<Usuario> alIngresar) {
        this.s = s;
        this.alIngresar = alIngresar;
        getStyleClass().add("login");

        StackPane izquierda = panelMarca();
        izquierda.prefWidthProperty().bind(widthProperty().multiply(0.46));
        izquierda.setMinWidth(380);

        derecha.getStyleClass().add("login-der");
        HBox.setHgrow(derecha, Priority.ALWAYS);
        getChildren().addAll(izquierda, derecha);
        mostrarIngreso(null);
    }

    // ------------------------------------------------------------------ panel izquierdo

    private StackPane panelMarca() {
        Group chakana = Adornos.chakanaEnGrupo(420, "login-chakana");
        StackPane.setAlignment(chakana, Pos.CENTER_RIGHT);
        StackPane.setMargin(chakana, new Insets(0, -150, 0, 0));

        Group greca = Adornos.greca(6, 9, "adorno-linea", "adorno");
        greca.setOpacity(0.5);

        StackPane logo = new StackPane(Ui.icono(Feather.SCISSORS));
        logo.getStyleClass().addAll("logo-cuadro", "logo-grande");
        Label nombre = new Label("Trajes Candelaria");
        nombre.getStyleClass().add("login-marca");
        Label frase = new Label("Trajes Candelaria · Alquiler y confección");
        frase.getStyleClass().add("login-frase");
        Label texto = new Label("Trajes de luces y autóctonos para la Festividad de la Virgen de la Candelaria. "
                + "Reserva en línea, recoge en tienda y luce tu mejor traje.");
        texto.getStyleClass().add("login-texto");
        texto.setWrapText(true);
        texto.setMaxWidth(420);

        VBox contenido = new VBox(18, logo, nombre, frase, greca, texto);
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setPadding(new Insets(56));
        contenido.setMaxWidth(560);
        StackPane.setAlignment(contenido, Pos.CENTER_LEFT);

        Label pie = new Label(Ui.espaciado("Juliaca · Puno · Perú"));
        pie.getStyleClass().add("login-pie");
        StackPane.setAlignment(pie, Pos.BOTTOM_LEFT);
        StackPane.setMargin(pie, new Insets(0, 0, 32, 56));

        StackPane panel = new StackPane(chakana, contenido, pie);
        panel.getStyleClass().add("login-izq");
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(panel.widthProperty());
        clip.heightProperty().bind(panel.heightProperty());
        panel.setClip(clip);
        return panel;
    }

    // ------------------------------------------------------------------ ingresar

    private void mostrarIngreso(String usuarioSugerido) {
        TextField usuario = new TextField(usuarioSugerido == null ? "" : usuarioSugerido);
        usuario.setPromptText("Tu usuario");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Tu contraseña");
        Label error = error();

        Button ingresar = Ui.estilo(new Button("Ingresar", Ui.icono(Feather.ARROW_RIGHT)), "boton-primario");
        ingresar.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        ingresar.getStyleClass().add("boton-ancho");
        ingresar.setMaxWidth(Double.MAX_VALUE);
        ingresar.setDefaultButton(true);
        ingresar.setOnAction(e -> intentar(error, () -> s.usuarios().autenticar(usuario.getText(), clave.getText())));

        Hyperlink crear = new Hyperlink("Crear cuenta de cliente");
        crear.getStyleClass().add("enlace");
        crear.setOnAction(e -> mostrarRegistro());
        HBox lineaCrear = new HBox(4, new Label("¿Primera vez?"), crear);
        lineaCrear.setAlignment(Pos.CENTER);
        lineaCrear.getStyleClass().add("login-ayuda");

        VBox tarjeta = tarjeta("Acceso", "Bienvenido", "Ingresa con tu usuario para continuar.",
                campo("Usuario", Feather.USER, usuario),
                campo("Contraseña", Feather.LOCK, clave),
                error, ingresar, lineaCrear);
        mostrar(tarjeta);
        Platform.runLater(() -> (usuario.getText().isEmpty() ? usuario : clave).requestFocus());
    }

    // ------------------------------------------------------------------ registro

    private void mostrarRegistro() {
        TextField nombre = new TextField();
        nombre.setPromptText("Nombres y apellidos");
        TextField dni = new TextField();
        dni.setPromptText("8 dígitos");
        TextField celular = new TextField();
        celular.setPromptText("9 dígitos");
        TextField correo = new TextField();
        correo.setPromptText("Opcional");
        TextField usuario = new TextField();
        usuario.setPromptText("Ej.: maria.quispe");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Mínimo 6 caracteres");
        Label error = error();

        HBox dniCel = new HBox(12, campo("DNI", Feather.CREDIT_CARD, dni), campo("Celular", Feather.PHONE, celular));
        dniCel.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        Button crear = Ui.estilo(new Button("Crear cuenta", Ui.icono(Feather.USER_PLUS)), "boton-primario");
        crear.getStyleClass().add("boton-ancho");
        crear.setMaxWidth(Double.MAX_VALUE);
        crear.setDefaultButton(true);
        crear.setOnAction(e -> intentar(error, () -> s.usuarios().registrarCliente(nombre.getText(), dni.getText(),
                celular.getText(), correo.getText(), usuario.getText(), clave.getText())));

        Hyperlink volver = new Hyperlink("Ingresar");
        volver.getStyleClass().add("enlace");
        volver.setOnAction(e -> mostrarIngreso(null));
        HBox lineaVolver = new HBox(4, new Label("¿Ya tienes cuenta?"), volver);
        lineaVolver.setAlignment(Pos.CENTER);
        lineaVolver.getStyleClass().add("login-ayuda");

        VBox tarjeta = tarjeta("Cuenta de cliente", "Crea tu cuenta",
                "Con tu cuenta puedes ver los trajes libres, solicitar reservas y seguir tus pedidos.",
                campo("Nombre completo", Feather.USER, nombre), dniCel,
                campo("Correo", Feather.MAIL, correo),
                campo("Usuario", Feather.AT_SIGN, usuario),
                campo("Contraseña", Feather.LOCK, clave),
                error, crear, lineaVolver);
        mostrar(tarjeta);
        Platform.runLater(nombre::requestFocus);
    }

    // ------------------------------------------------------------------ piezas

    private void mostrar(VBox tarjeta) {
        StackPane centro = new StackPane(tarjeta);
        centro.setPadding(new Insets(40));
        ScrollPane scroll = new ScrollPane(centro);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("login-scroll");
        derecha.getChildren().setAll(scroll);
    }

    private static VBox tarjeta(String sobre, String titulo, String texto, javafx.scene.Node... hijos) {
        Label t = new Label(titulo);
        t.getStyleClass().add("login-titulo");
        Label sub = new Label(texto);
        sub.getStyleClass().add("subtitulo");
        sub.setWrapText(true);
        VBox cabecera = new VBox(6, Ui.sobretitulo(sobre), t, sub);
        VBox.setMargin(cabecera, new Insets(0, 0, 8, 0));
        VBox caja = new VBox(16);
        caja.getChildren().add(cabecera);
        caja.getChildren().addAll(hijos);
        caja.getStyleClass().add("login-tarjeta");
        caja.setMaxWidth(440);
        caja.setMaxHeight(Region.USE_PREF_SIZE);
        return caja;
    }

    /** Campo de 44 px con ícono a la izquierda; el borde se vuelve dorado al enfocar. */
    private static VBox campo(String titulo, Ikon ikon, TextField control) {
        Label l = new Label(Ui.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.getStyleClass().add("campo-interno");
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox caja = new HBox(10, Ui.icono(ikon), control);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("campo-icono");
        caja.setOnMouseClicked(e -> control.requestFocus());
        return new VBox(7, l, caja);
    }

    private static Label error() {
        Label l = new Label();
        l.getStyleClass().add("error-suave");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setGraphic(Ui.icono(Feather.ALERT_CIRCLE));
        l.setGraphicTextGap(8);
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    @FunctionalInterface
    private interface Intento {
        Usuario ejecutar();
    }

    /** Ejecuta el ingreso o el registro; si falla, muestra el mensaje en rojo suave dentro de la tarjeta. */
    private void intentar(Label error, Intento intento) {
        try {
            Usuario u = intento.ejecutar();
            error.setVisible(false);
            error.setManaged(false);
            alIngresar.accept(u);
        } catch (ReglaNegocioException | DataAccessException ex) {
            error.setText(Ui.mensaje(ex));
            error.setVisible(true);
            error.setManaged(true);
        }
    }
}
