/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.modelo.Usuario;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
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
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class VistaInicioSesion extends HBox {

    private final Servicios sErht;
    private final Consumer<Usuario> alIngresarErht;
    private final StackPane derechaErht = new StackPane();

    public VistaInicioSesion(Servicios s, Consumer<Usuario> alIngresar) {
        this.sErht = s;
        this.alIngresarErht = alIngresar;
        getStyleClass().add("inicio-sesion");

        StackPane izquierdaErht = panelMarcaErht();
        izquierdaErht.prefWidthProperty().bind(widthProperty().multiply(0.46));
        izquierdaErht.setMinWidth(380);

        derechaErht.getStyleClass().add("inicio-sesion-der");
        HBox.setHgrow(derechaErht, Priority.ALWAYS);
        getChildren().addAll(izquierdaErht, derechaErht);
        mostrarIngresoErht(null);
    }

    private StackPane panelMarcaErht() {
        Group chakanaErht = Adornos.chakanaEnGrupo(420, "inicio-sesion-chakana");
        StackPane.setAlignment(chakanaErht, Pos.CENTER_RIGHT);
        StackPane.setMargin(chakanaErht, new Insets(0, -150, 0, 0));

        Group grecaErht = Adornos.greca(6, 9, "adorno-linea", "adorno");
        grecaErht.setOpacity(0.5);

        StackPane logotipoErht = new StackPane(UtilidadesInterfaz.icono(Feather.SCISSORS));
        logotipoErht.getStyleClass().addAll("logotipo-cuadro", "logotipo-grande");
        Label nombreErht = new Label("Trajes Candelaria");
        nombreErht.getStyleClass().add("inicio-sesion-marca");
        Label fraseErht = new Label("Trajes Candelaria · Alquiler y confección");
        fraseErht.getStyleClass().add("inicio-sesion-frase");
        Label textoErht = new Label("Trajes de luces y autóctonos para la Festividad de la Virgen de la Candelaria. "
                + "Reserva en línea, recoge en tienda y luce tu mejor traje.");
        textoErht.getStyleClass().add("inicio-sesion-texto");
        textoErht.setWrapText(true);
        textoErht.setMaxWidth(420);

        VBox contenidoErht = new VBox(18, logotipoErht, nombreErht, fraseErht, grecaErht, textoErht);
        contenidoErht.setAlignment(Pos.CENTER_LEFT);
        contenidoErht.setPadding(new Insets(56));
        contenidoErht.setMaxWidth(560);
        StackPane.setAlignment(contenidoErht, Pos.CENTER_LEFT);

        Label lugarErht = new Label(UtilidadesInterfaz.espaciado("Juliaca · Puno · Perú"));
        lugarErht.getStyleClass().add("inicio-sesion-pie");
        Label derechosErht = new Label(AcercaDe.PIE_DERECHOS);
        derechosErht.getStyleClass().add("inicio-sesion-pie");
        VBox pieErht = new VBox(4, lugarErht, derechosErht);
        pieErht.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(pieErht, Pos.BOTTOM_LEFT);
        StackPane.setMargin(pieErht, new Insets(0, 0, 32, 56));

        StackPane panelErht = new StackPane(chakanaErht, contenidoErht, pieErht);
        panelErht.getStyleClass().add("inicio-sesion-izq");
        javafx.scene.shape.Rectangle recorteErht = new javafx.scene.shape.Rectangle();
        recorteErht.widthProperty().bind(panelErht.widthProperty());
        recorteErht.heightProperty().bind(panelErht.heightProperty());
        panelErht.setClip(recorteErht);
        return panelErht;
    }

    private void mostrarIngresoErht(String usuarioSugerido) {
        TextField usuarioErht = new TextField(usuarioSugerido == null ? "" : usuarioSugerido);
        usuarioErht.setPromptText("Tu usuario");
        PasswordField claveErht = new PasswordField();
        claveErht.setPromptText("Tu contraseña");
        Label errorErht = etiquetaErrorErht();

        Button ingresarErht = UtilidadesInterfaz.estilo(new Button("Ingresar", UtilidadesInterfaz.icono(Feather.ARROW_RIGHT)), "boton-primario");
        ingresarErht.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        ingresarErht.getStyleClass().add("boton-ancho");
        ingresarErht.setMaxWidth(Double.MAX_VALUE);
        ingresarErht.setDefaultButton(true);
        ingresarErht.setOnAction(e -> intentarErht(errorErht, () -> sErht.usuarios().autenticar(usuarioErht.getText(), claveErht.getText())));

        Hyperlink crearErht = new Hyperlink("Crear cuenta de cliente");
        crearErht.getStyleClass().add("enlace");
        crearErht.setOnAction(e -> mostrarRegistroErht());
        HBox lineaCrearErht = new HBox(4, new Label("¿Primera vez?"), crearErht);
        lineaCrearErht.setAlignment(Pos.CENTER);
        lineaCrearErht.getStyleClass().add("inicio-sesion-ayuda");

        VBox tarjetaErht = tarjetaErht("Acceso", "Bienvenido", "Ingresa con tu usuario para continuar.",
                campoErht("Usuario", Feather.USER, usuarioErht),
                campoErht("Contraseña", Feather.LOCK, claveErht),
                errorErht, ingresarErht, lineaCrearErht);
        mostrarErht(tarjetaErht);
        Platform.runLater(() -> (usuarioErht.getText().isEmpty() ? usuarioErht : claveErht).requestFocus());
    }

    private void mostrarRegistroErht() {
        TextField nombreErht = new TextField();
        nombreErht.setPromptText("Nombres y apellidos");
        TextField dniErht = new TextField();
        dniErht.setPromptText("8 dígitos");
        TextField celularErht = new TextField();
        celularErht.setPromptText("9 dígitos");
        TextField correoErht = new TextField();
        correoErht.setPromptText("Opcional");
        TextField usuarioErht = new TextField();
        usuarioErht.setPromptText("Ej.: maria.quispe");
        PasswordField claveErht = new PasswordField();
        claveErht.setPromptText("Mínimo 6 caracteres");
        Label errorErht = etiquetaErrorErht();

        HBox dniCelErht = new HBox(12, campoErht("DNI", Feather.CREDIT_CARD, dniErht), campoErht("Celular", Feather.PHONE, celularErht));
        dniCelErht.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        Button crearErht = UtilidadesInterfaz.estilo(new Button("Crear cuenta", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
        crearErht.getStyleClass().add("boton-ancho");
        crearErht.setMaxWidth(Double.MAX_VALUE);
        crearErht.setDefaultButton(true);
        crearErht.setOnAction(e -> intentarErht(errorErht, () -> sErht.usuarios().registrarCliente(nombreErht.getText(), dniErht.getText(),
                celularErht.getText(), correoErht.getText(), usuarioErht.getText(), claveErht.getText())));

        Hyperlink volverErht = new Hyperlink("Ingresar");
        volverErht.getStyleClass().add("enlace");
        volverErht.setOnAction(e -> mostrarIngresoErht(null));
        HBox lineaVolverErht = new HBox(4, new Label("¿Ya tienes cuenta?"), volverErht);
        lineaVolverErht.setAlignment(Pos.CENTER);
        lineaVolverErht.getStyleClass().add("inicio-sesion-ayuda");

        VBox tarjetaErht = tarjetaErht("Cuenta de cliente", "Crea tu cuenta",
                "Con tu cuenta puedes ver los trajes libres, solicitar reservas y seguir tus pedidos.",
                campoErht("Nombre completo", Feather.USER, nombreErht), dniCelErht,
                campoErht("Correo", Feather.MAIL, correoErht),
                campoErht("Usuario", Feather.AT_SIGN, usuarioErht),
                campoErht("Contraseña", Feather.LOCK, claveErht),
                errorErht, crearErht, lineaVolverErht);
        mostrarErht(tarjetaErht);
        Platform.runLater(nombreErht::requestFocus);
    }

    private void mostrarErht(VBox tarjeta) {
        StackPane centroErht = new StackPane(tarjeta);
        centroErht.setPadding(new Insets(40));
        ScrollPane desplazableErht = new ScrollPane(centroErht);
        desplazableErht.setFitToWidth(true);
        desplazableErht.setFitToHeight(true);
        desplazableErht.getStyleClass().add("inicio-sesion-scroll");
        derechaErht.getChildren().setAll(desplazableErht);
    }

    private static VBox tarjetaErht(String sobre, String titulo, String texto, javafx.scene.Node... hijos) {
        Label t = new Label(titulo);
        t.getStyleClass().add("inicio-sesion-titulo");
        Label subErht = new Label(texto);
        subErht.getStyleClass().add("subtitulo");
        subErht.setWrapText(true);
        VBox cabeceraErht = new VBox(6, UtilidadesInterfaz.sobretitulo(sobre), t, subErht);
        VBox.setMargin(cabeceraErht, new Insets(0, 0, 8, 0));
        VBox cajaErht = new VBox(16);
        cajaErht.getChildren().add(cabeceraErht);
        cajaErht.getChildren().addAll(hijos);
        cajaErht.getStyleClass().add("inicio-sesion-tarjeta");
        cajaErht.setMaxWidth(440);
        cajaErht.setMaxHeight(Region.USE_PREF_SIZE);
        return cajaErht;
    }

    private static VBox campoErht(String titulo, Ikon ikon, TextField control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.getStyleClass().add("campo-interno");
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox cajaErht = new HBox(10, UtilidadesInterfaz.icono(ikon), control);
        cajaErht.setAlignment(Pos.CENTER_LEFT);
        cajaErht.getStyleClass().add("campo-icono");
        cajaErht.setOnMouseClicked(e -> control.requestFocus());
        return new VBox(7, l, cajaErht);
    }

    private static Label etiquetaErrorErht() {
        Label l = new Label();
        l.getStyleClass().add("error-suave");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setGraphic(UtilidadesInterfaz.icono(Feather.ALERT_CIRCLE));
        l.setGraphicTextGap(8);
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    @FunctionalInterface
    private interface Intento {
        Usuario ejecutar();
    }

    private void intentarErht(Label error, Intento intento) {
        try {
            Usuario u = intento.ejecutar();
            error.setVisible(false);
            error.setManaged(false);
            alIngresarErht.accept(u);
        } catch (ExcepcionReglaNegocio | DataAccessException ex) {
            error.setText(UtilidadesInterfaz.mensaje(ex));
            error.setVisible(true);
            error.setManaged(true);
        }
    }
}
