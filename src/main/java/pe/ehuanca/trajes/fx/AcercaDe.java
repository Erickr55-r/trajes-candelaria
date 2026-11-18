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

    public static final String CONTACTO = "erickrhuanca@gmail.com";
    public static final String PIE_DERECHOS = "© 2026 Erick R. Huanca Tiña · v1.0";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private AcercaDe() {
    }

    public static void mostrar() {
        Properties datosErht = leerDatosErht();

        StackPane logotipoErht = new StackPane(UtilidadesInterfaz.icono(Feather.SCISSORS));
        logotipoErht.getStyleClass().addAll("logotipo-cuadro", "logotipo-grande");

        Label nombreErht = UtilidadesInterfaz.titulo("Trajes Candelaria v1.0");
        Label descripcionErht = UtilidadesInterfaz.subtitulo("Alquiler y personalización de trajes de danza");
        Label derechosErht = new Label("© 2026 Erick Reison Huanca Tiña · Todos los derechos reservados");
        derechosErht.setWrapText(true);
        Label contactoErht = new Label("Contacto: " + CONTACTO);
        Label versionErht = new Label("Versión " + datosErht.getProperty("version", "1.0.0")
                + " · Compilado el " + fechaCompilacion(datosErht.getProperty("fecha.compilacion", "")));
        versionErht.getStyleClass().add("nota");

        Button tercerosErht = UtilidadesInterfaz.estilo(
                new Button("Componentes de terceros", UtilidadesInterfaz.icono(Feather.FILE_TEXT)), "boton-secundario");
        tercerosErht.setOnAction(e -> mostrarTercerosErht());
        Button cerrarErht = UtilidadesInterfaz.estilo(new Button("Cerrar"), "boton-primario");
        cerrarErht.setDefaultButton(true);
        HBox botonesErht = new HBox(10, tercerosErht, cerrarErht);
        botonesErht.setAlignment(Pos.CENTER);

        VBox cajaErht = new VBox(10, logotipoErht, nombreErht, descripcionErht, derechosErht, contactoErht, versionErht, botonesErht);
        cajaErht.setAlignment(Pos.CENTER);
        cajaErht.setPadding(new Insets(28, 32, 24, 32));
        VBox.setMargin(botonesErht, new Insets(12, 0, 0, 0));
        cajaErht.getStyleClass().add("contenido");
        derechosErht.setStyle("-fx-text-alignment: center;");

        Stage ventanaErht = UtilidadesInterfaz.ventana("Acerca de Trajes Candelaria", cajaErht, 460, 400);
        ventanaErht.setResizable(false);
        cerrarErht.setOnAction(e -> ventanaErht.close());
        ventanaErht.showAndWait();
    }

    private static void mostrarTercerosErht() {
        TextArea textoErht = new TextArea(leerTerceros());
        textoErht.setEditable(false);
        textoErht.setWrapText(false);
        textoErht.setFont(Font.font("Consolas", 12));
        Button cerrarErht = UtilidadesInterfaz.estilo(new Button("Cerrar"), "boton-primario");
        HBox pieErht = new HBox(cerrarErht);
        pieErht.setAlignment(Pos.CENTER_RIGHT);
        pieErht.setPadding(new Insets(10, 0, 0, 0));
        BorderPane raizErht = new BorderPane(textoErht, null, null, pieErht, null);
        raizErht.setPadding(new Insets(16));
        raizErht.getStyleClass().add("contenido");
        Stage ventanaErht = UtilidadesInterfaz.ventana("Componentes de terceros", raizErht, 900, 600);
        cerrarErht.setOnAction(e -> ventanaErht.close());
        ventanaErht.showAndWait();
    }

    private static Properties leerDatosErht() {
        Properties p = new Properties();
        try (InputStream in = AcercaDe.class.getResourceAsStream("/acerca.properties")) {
            if (in != null) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignorada) {
        }
        return p;
    }

    static String fechaCompilacion(String valor) {
        try {
            if (!valor.isBlank() && !valor.startsWith("@")) {
                return FORMATO_FECHA.format(Instant.parse(valor).atZone(ZoneId.systemDefault()));
            }
            URL claseErht = AcercaDe.class.getResource("AcercaDe.class");
            if (claseErht != null) {
                long ms = claseErht.openConnection().getLastModified();
                if (ms > 0) {
                    return FORMATO_FECHA.format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()));
                }
            }
        } catch (Exception ignorada) {
        }
        return "(fecha no disponible)";
    }

    static String leerTerceros() {
        try (InputStream in = AcercaDe.class.getResourceAsStream("/legal/TERCEROS.md")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Path archivoErht = Path.of(System.getProperty("user.dir"), "TERCEROS.md");
            if (Files.exists(archivoErht)) {
                return Files.readString(archivoErht, StandardCharsets.UTF_8);
            }
        } catch (IOException ignorada) {
        }
        return "No se encontró el archivo TERCEROS.md. Consulte el repositorio del programa.";
    }
}
