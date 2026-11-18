/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.modelo.Usuario;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;

import java.util.Locale;

/**
 * Piezas comunes de la barra superior: el logo y el avatar del usuario con su menú.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class Cabecera {

    private Cabecera() {
    }

    /** Cuadro dorado con tijeras + "Trajes Candelaria" + "ALQUILER Y CONFECCIÓN". */
    public static HBox marca(Runnable alHacerClic) {
        StackPane logotipoErht = new StackPane(UtilidadesInterfaz.icono(Feather.SCISSORS));
        logotipoErht.getStyleClass().add("logotipo-cuadro");
        Label tituloErht = new Label("Trajes Candelaria");
        tituloErht.getStyleClass().add("menu-titulo");
        Label subtituloErht = new Label(UtilidadesInterfaz.espaciado("Alquiler y confección"));
        subtituloErht.getStyleClass().add("menu-subtitulo");
        VBox textosErht = new VBox(1, tituloErht, subtituloErht);
        textosErht.setAlignment(Pos.CENTER_LEFT);
        HBox marcaErht = new HBox(logotipoErht, textosErht);
        marcaErht.getStyleClass().add("menu-marca");
        marcaErht.setAlignment(Pos.CENTER_LEFT);
        marcaErht.setMinWidth(Region.USE_PREF_SIZE);
        if (alHacerClic != null) {
            marcaErht.setOnMouseClicked(e -> alHacerClic.run());
            marcaErht.setStyle("-fx-cursor: hand;");
        }
        return marcaErht;
    }

    /** Avatar circular con las iniciales del usuario de la sesión y el menú Tema / Cerrar sesión / Salir. */
    public static StackPane avatar(Runnable alCerrarSesion) {
        Usuario u = Sesion.actual().usuario();
        Circle circuloErht = new Circle(19);
        circuloErht.getStyleClass().add("avatar-circulo");
        Text inicialesErht = new Text(iniciales(u.nombreVisible()));
        inicialesErht.getStyleClass().add("avatar-texto");
        StackPane avatarErht = new StackPane(circuloErht, inicialesErht);
        avatarErht.getStyleClass().add("avatar");
        avatarErht.setMinWidth(Region.USE_PREF_SIZE);
        Tooltip.install(avatarErht, new Tooltip(u.nombreVisible() + " · " + u.rolVisible()));

        // Encabezado del menú: nombre y rol (no se puede elegir)
        Label nombreErht = new Label(u.nombreVisible());
        nombreErht.getStyleClass().add("menu-usuario-nombre");
        Label rolErht = new Label(UtilidadesInterfaz.espaciado(u.rolVisible()) + "  ·  @" + u.nombreUsuario());
        rolErht.getStyleClass().add("menu-usuario-rol");
        CustomMenuItem quienErht = new CustomMenuItem(new VBox(2, nombreErht, rolErht), false);
        quienErht.setDisable(true);
        quienErht.getStyleClass().add("menu-usuario");

        Menu temaErht = new Menu("Tema", UtilidadesInterfaz.icono(Feather.DROPLET));
        ToggleGroup grupoErht = new ToggleGroup();
        for (String t : UtilidadesInterfaz.TEMAS.keySet()) {
            RadioMenuItem r = new RadioMenuItem(t);
            r.setToggleGroup(grupoErht);
            r.setSelected(t.equals(UtilidadesInterfaz.temaActual()));
            r.setOnAction(e -> UtilidadesInterfaz.cambiarTema(t));
            temaErht.getItems().add(r);
        }
        MenuItem cerrarErht = new MenuItem("Cerrar sesión", UtilidadesInterfaz.icono(Feather.LOG_OUT));
        cerrarErht.setOnAction(e -> alCerrarSesion.run());
        MenuItem salirErht = new MenuItem("Salir", UtilidadesInterfaz.icono(Feather.POWER));
        salirErht.setOnAction(e -> Platform.exit());

        MenuItem acercaErht = new MenuItem("Acerca de", UtilidadesInterfaz.icono(Feather.INFO));
        acercaErht.setOnAction(e -> AcercaDe.mostrar());

        ContextMenu menuErht = new ContextMenu(quienErht, new SeparatorMenuItem(), temaErht, acercaErht, new SeparatorMenuItem(),
                cerrarErht, salirErht);
        avatarErht.setOnMouseClicked(e -> menuErht.show(avatarErht, Side.BOTTOM, -170, 8));
        return avatarErht;
    }

    /** Botón visible "Cerrar sesión" para la barra superior. */
    public static javafx.scene.control.Button botonCerrarSesion(Runnable alCerrarSesion) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(null, UtilidadesInterfaz.icono(Feather.LOG_OUT));
        b.getStyleClass().add("boton-salir");
        b.setTooltip(new Tooltip("Cerrar sesión"));
        b.setOnAction(e -> alCerrarSesion.run());
        return b;
    }

    static String iniciales(String nombre) {
        String[] partesErht = nombre.trim().split("[\\s._-]+");
        String r = partesErht.length >= 2
                ? "" + partesErht[0].charAt(0) + partesErht[1].charAt(0)
                : nombre.substring(0, Math.min(2, nombre.length()));
        return r.toUpperCase(Locale.ROOT);
    }
}
