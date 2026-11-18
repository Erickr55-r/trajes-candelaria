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

    private final Servicios s;
    private final Consumer<Usuario> alIngresar;
    private final StackPane derecha = new StackPane();

    public VistaInicioSesion(Servicios s, Consumer<Usuario> alIngresar) {
        this.s = s;
        this.alIngresar = alIngresar;
        getStyleClass().add("inicio-sesion");

        StackPane izquierda = panelMarca();
        izquierda.prefWidthProperty().bind(widthProperty().multiply(0.46));
        izquierda.setMinWidth(380);

        derecha.getStyleClass().add("inicio-sesion-der");
        HBox.setHgrow(derecha, Priority.ALWAYS);
        getChildren().addAll(izquierda, derecha);
        mostrarIngreso(null);
    }

    // ------------------------------------------------------------------ panel izquierdo

    private StackPane panelMarca() {
        Group chakana = Adornos.chakanaEnGrupo(420, "inicio-sesion-chakana");
        StackPane.setAlignment(chakana, Pos.CENTER_RIGHT);
        StackPane.setMargin(chakana, new Insets(0, -150, 0, 0));

        Group greca = Adornos.greca(6, 9, "adorno-linea", "adorno");
        greca.setOpacity(0.5);

        StackPane logotipo = new StackPane(UtilidadesInterfaz.icono(Feather.SCISSORS));
        logotipo.getStyleClass().addAll("logotipo-cuadro", "logotipo-grande");
        Label nombre = new Label("Trajes Candelaria");
        nombre.getStyleClass().add("inicio-sesion-marca");
        Label frase = new Label("Trajes Candelaria · Alquiler y confección");
        frase.getStyleClass().add("inicio-sesion-frase");
        Label texto = new Label("Trajes de luces y autóctonos para la Festividad de la Virgen de la Candelaria. "
                + "Reserva en línea, recoge en tienda y luce tu mejor traje.");
        texto.getStyleClass().add("inicio-sesion-texto");
        texto.setWrapText(true);
        texto.setMaxWidth(420);

        VBox contenido = new VBox(18, logotipo, nombre, frase, greca, texto);
        contenido.setAlignment(Pos.CENTER_LEFT);
        contenido.setPadding(new Insets(56));
        contenido.setMaxWidth(560);
        StackPane.setAlignment(contenido, Pos.CENTER_LEFT);

        Label lugar = new Label(UtilidadesInterfaz.espaciado("Juliaca · Puno · Perú"));
        lugar.getStyleClass().add("inicio-sesion-pie");
        Label derechos = new Label(AcercaDe.PIE_DERECHOS);
        derechos.getStyleClass().add("inicio-sesion-pie");
        VBox pie = new VBox(4, lugar, derechos);
        pie.setMaxSize(javafx.scene.layout.Region.USE_PREF_SIZE, javafx.scene.layout.Region.USE_PREF_SIZE);
        StackPane.setAlignment(pie, Pos.BOTTOM_LEFT);
        StackPane.setMargin(pie, new Insets(0, 0, 32, 56));

        StackPane panel = new StackPane(chakana, contenido, pie);
        panel.getStyleClass().add("inicio-sesion-izq");
        javafx.scene.shape.Rectangle recorte = new javafx.scene.shape.Rectangle();
        recorte.widthProperty().bind(panel.widthProperty());
        recorte.heightProperty().bind(panel.heightProperty());
        panel.setClip(recorte);
        return panel;
    }

    // ------------------------------------------------------------------ ingresar

    private void mostrarIngreso(String usuarioSugerido) {
        TextField usuario = new TextField(usuarioSugerido == null ? "" : usuarioSugerido);
        usuario.setPromptText("Tu usuario");
        PasswordField clave = new PasswordField();
        clave.setPromptText("Tu contraseña");
        Label error = etiquetaError();

        Button ingresar = UtilidadesInterfaz.estilo(new Button("Ingresar", UtilidadesInterfaz.icono(Feather.ARROW_RIGHT)), "boton-primario");
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
        lineaCrear.getStyleClass().add("inicio-sesion-ayuda");

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
        Label error = etiquetaError();

        HBox dniCel = new HBox(12, campo("DNI", Feather.CREDIT_CARD, dni), campo("Celular", Feather.PHONE, celular));
        dniCel.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        Button crear = UtilidadesInterfaz.estilo(new Button("Crear cuenta", UtilidadesInterfaz.icono(Feather.USER_PLUS)), "boton-primario");
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
        lineaVolver.getStyleClass().add("inicio-sesion-ayuda");

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
        ScrollPane desplazable = new ScrollPane(centro);
        desplazable.setFitToWidth(true);
        desplazable.setFitToHeight(true);
        desplazable.getStyleClass().add("inicio-sesion-scroll");
        derecha.getChildren().setAll(desplazable);
    }

    private static VBox tarjeta(String sobre, String titulo, String texto, javafx.scene.Node... hijos) {
        Label t = new Label(titulo);
        t.getStyleClass().add("inicio-sesion-titulo");
        Label sub = new Label(texto);
        sub.getStyleClass().add("subtitulo");
        sub.setWrapText(true);
        VBox cabecera = new VBox(6, UtilidadesInterfaz.sobretitulo(sobre), t, sub);
        VBox.setMargin(cabecera, new Insets(0, 0, 8, 0));
        VBox caja = new VBox(16);
        caja.getChildren().add(cabecera);
        caja.getChildren().addAll(hijos);
        caja.getStyleClass().add("inicio-sesion-tarjeta");
        caja.setMaxWidth(440);
        caja.setMaxHeight(Region.USE_PREF_SIZE);
        return caja;
    }

    /** Campo de 44 px con ícono a la izquierda; el borde se vuelve dorado al enfocar. */
    private static VBox campo(String titulo, Ikon ikon, TextField control) {
        Label l = new Label(UtilidadesInterfaz.espaciado(titulo));
        l.getStyleClass().add("campo-titulo");
        control.getStyleClass().add("campo-interno");
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox caja = new HBox(10, UtilidadesInterfaz.icono(ikon), control);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("campo-icono");
        caja.setOnMouseClicked(e -> control.requestFocus());
        return new VBox(7, l, caja);
    }

    private static Label etiquetaError() {
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

    /** Ejecuta el ingreso o el registro; si falla, muestra el mensaje en rojo suave dentro de la tarjeta. */
    private void intentar(Label error, Intento intento) {
        try {
            Usuario u = intento.ejecutar();
            error.setVisible(false);
            error.setManaged(false);
            alIngresar.accept(u);
        } catch (ExcepcionReglaNegocio | DataAccessException ex) {
            error.setText(UtilidadesInterfaz.mensaje(ex));
            error.setVisible(true);
            error.setManaged(true);
        }
    }
}
