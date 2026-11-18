/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Ventana «Acerca de»: nombre y versión del programa, autoría, contacto, fecha de compilación
 * y acceso a la lista de componentes de terceros (TERCEROS.md).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class AcercaDe {

    /** Correo de contacto del autor. */
    public static final String CONTACTO = "erickrhuanca@gmail.com";
    /** Línea corta de derechos para el pie del inicio de sesión. */
    public static final String PIE_DERECHOS = "© 2026 Erick R. Huanca Tiña · v1.0";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private AcercaDe() {
    }

    /** Abre la ventana «Acerca de» (modal). */
    public static void mostrar() {
        Properties datos = leerDatos();

        StackPane logotipo = new StackPane(UtilidadesInterfaz.icono(Feather.SCISSORS));
        logotipo.getStyleClass().addAll("logotipo-cuadro", "logotipo-grande");

        Label nombre = UtilidadesInterfaz.titulo("Trajes Candelaria v1.0");
        Label descripcion = UtilidadesInterfaz.subtitulo("Alquiler y personalización de trajes de danza");
        Label derechos = new Label("© 2026 Erick Reison Huanca Tiña · Todos los derechos reservados");
        derechos.setWrapText(true);
        Label contacto = new Label("Contacto: " + CONTACTO);
        Label version = new Label("Versión " + datos.getProperty("version", "1.0.0")
                + " · Compilado el " + fechaCompilacion(datos.getProperty("fecha.compilacion", "")));
        version.getStyleClass().add("nota");

        Button terceros = UtilidadesInterfaz.estilo(
                new Button("Componentes de terceros", UtilidadesInterfaz.icono(Feather.FILE_TEXT)), "boton-secundario");
        terceros.setOnAction(e -> mostrarTerceros());
        Button cerrar = UtilidadesInterfaz.estilo(new Button("Cerrar"), "boton-primario");
        cerrar.setDefaultButton(true);
        HBox botones = new HBox(10, terceros, cerrar);
        botones.setAlignment(Pos.CENTER);

        VBox caja = new VBox(10, logotipo, nombre, descripcion, derechos, contacto, version, botones);
        caja.setAlignment(Pos.CENTER);
        caja.setPadding(new Insets(28, 32, 24, 32));
        VBox.setMargin(botones, new Insets(12, 0, 0, 0));
        caja.getStyleClass().add("contenido");
        derechos.setStyle("-fx-text-alignment: center;");

        Stage ventana = UtilidadesInterfaz.ventana("Acerca de Trajes Candelaria", caja, 460, 400);
        ventana.setResizable(false);
        cerrar.setOnAction(e -> ventana.close());
        ventana.showAndWait();
    }

    /** Muestra el contenido de TERCEROS.md en una ventana de solo lectura. */
    private static void mostrarTerceros() {
        TextArea texto = new TextArea(leerTerceros());
        texto.setEditable(false);
        texto.setWrapText(false);
        texto.setFont(Font.font("Consolas", 12));
        Button cerrar = UtilidadesInterfaz.estilo(new Button("Cerrar"), "boton-primario");
        HBox pie = new HBox(cerrar);
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.setPadding(new Insets(10, 0, 0, 0));
        BorderPane raiz = new BorderPane(texto, null, null, pie, null);
        raiz.setPadding(new Insets(16));
        raiz.getStyleClass().add("contenido");
        Stage ventana = UtilidadesInterfaz.ventana("Componentes de terceros", raiz, 900, 600);
        cerrar.setOnAction(e -> ventana.close());
        ventana.showAndWait();
    }

    private static Properties leerDatos() {
        Properties p = new Properties();
        try (InputStream in = AcercaDe.class.getResourceAsStream("/acerca.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignorada) {
            // Sin el archivo se muestran los valores por defecto.
        }
        return p;
    }

    /**
     * Fecha de compilación en la hora local. Si el programa no se compiló con Maven (el valor sigue con @),
     * se usa la fecha de modificación de esta clase compilada.
     */
    static String fechaCompilacion(String valor) {
        try {
            if (!valor.isBlank() && !valor.startsWith("@")) {
                return FORMATO_FECHA.format(Instant.parse(valor).atZone(ZoneId.systemDefault()));
            }
            URL clase = AcercaDe.class.getResource("AcercaDe.class");
            if (clase != null) {
                long ms = clase.openConnection().getLastModified();
                if (ms > 0) {
                    return FORMATO_FECHA.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()));
                }
            }
        } catch (Exception ignorada) {
            // Se devuelve el texto por defecto.
        }
        return "(fecha no disponible)";
    }

    /** TERCEROS.md: primero el copiado al compilar (carpeta legal del classpath), si no el de la carpeta del proyecto. */
    static String leerTerceros() {
        try (InputStream in = AcercaDe.class.getResourceAsStream("/legal/TERCEROS.md")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Path archivo = Path.of(System.getProperty("user.dir"), "TERCEROS.md");
            if (Files.exists(archivo)) {
                return Files.readString(archivo, StandardCharsets.UTF_8);
            }
        } catch (IOException ignorada) {
            // Se muestra el aviso de abajo.
        }
        return "No se encontró el archivo TERCEROS.md. Consulte el repositorio del programa.";
    }
}
