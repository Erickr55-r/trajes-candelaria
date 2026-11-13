package com.trajes.fx;

import com.trajes.modelo.Usuario;
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

/** Piezas comunes de la barra superior: el logo y el avatar del usuario con su menú. */
public final class Cabecera {

    private Cabecera() {
    }

    /** Cuadro dorado con tijeras + "Trajes Candelaria" + "ALQUILER Y CONFECCIÓN". */
    public static HBox marca(Runnable alHacerClic) {
        StackPane logo = new StackPane(Ui.icono(Feather.SCISSORS));
        logo.getStyleClass().add("logo-cuadro");
        Label titulo = new Label("Trajes Candelaria");
        titulo.getStyleClass().add("menu-titulo");
        Label subtitulo = new Label(Ui.espaciado("Alquiler y confección"));
        subtitulo.getStyleClass().add("menu-subtitulo");
        VBox textos = new VBox(1, titulo, subtitulo);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox marca = new HBox(logo, textos);
        marca.getStyleClass().add("menu-marca");
        marca.setAlignment(Pos.CENTER_LEFT);
        marca.setMinWidth(Region.USE_PREF_SIZE);
        if (alHacerClic != null) {
            marca.setOnMouseClicked(e -> alHacerClic.run());
            marca.setStyle("-fx-cursor: hand;");
        }
        return marca;
    }

    /** Avatar circular con las iniciales del usuario de la sesión y el menú Tema / Cerrar sesión / Salir. */
    public static StackPane avatar(Runnable alCerrarSesion) {
        Usuario u = Sesion.get().usuario();
        Circle circulo = new Circle(19);
        circulo.getStyleClass().add("avatar-circulo");
        Text iniciales = new Text(iniciales(u.nombreVisible()));
        iniciales.getStyleClass().add("avatar-texto");
        StackPane avatar = new StackPane(circulo, iniciales);
        avatar.getStyleClass().add("avatar");
        avatar.setMinWidth(Region.USE_PREF_SIZE);
        Tooltip.install(avatar, new Tooltip(u.nombreVisible() + " · " + u.rolVisible()));

        // Encabezado del menú: nombre y rol (no se puede elegir)
        Label nombre = new Label(u.nombreVisible());
        nombre.getStyleClass().add("menu-usuario-nombre");
        Label rol = new Label(Ui.espaciado(u.rolVisible()) + "  ·  @" + u.username());
        rol.getStyleClass().add("menu-usuario-rol");
        CustomMenuItem quien = new CustomMenuItem(new VBox(2, nombre, rol), false);
        quien.setDisable(true);
        quien.getStyleClass().add("menu-usuario");

        Menu tema = new Menu("Tema", Ui.icono(Feather.DROPLET));
        ToggleGroup grupo = new ToggleGroup();
        for (String t : Ui.TEMAS.keySet()) {
            RadioMenuItem r = new RadioMenuItem(t);
            r.setToggleGroup(grupo);
            r.setSelected(t.equals(Ui.temaActual()));
            r.setOnAction(e -> Ui.cambiarTema(t));
            tema.getItems().add(r);
        }
        MenuItem cerrar = new MenuItem("Cerrar sesión", Ui.icono(Feather.LOG_OUT));
        cerrar.setOnAction(e -> alCerrarSesion.run());
        MenuItem salir = new MenuItem("Salir", Ui.icono(Feather.POWER));
        salir.setOnAction(e -> Platform.exit());

        ContextMenu menu = new ContextMenu(quien, new SeparatorMenuItem(), tema, new SeparatorMenuItem(), cerrar, salir);
        avatar.setOnMouseClicked(e -> menu.show(avatar, Side.BOTTOM, -170, 8));
        return avatar;
    }

    /** Botón visible "Cerrar sesión" para la barra superior. */
    public static javafx.scene.control.Button botonCerrarSesion(Runnable alCerrarSesion) {
        javafx.scene.control.Button b = new javafx.scene.control.Button(null, Ui.icono(Feather.LOG_OUT));
        b.getStyleClass().add("boton-salir");
        b.setTooltip(new Tooltip("Cerrar sesión"));
        b.setOnAction(e -> alCerrarSesion.run());
        return b;
    }

    static String iniciales(String nombre) {
        String[] partes = nombre.trim().split("[\\s._-]+");
        String r = partes.length >= 2
                ? "" + partes[0].charAt(0) + partes[1].charAt(0)
                : nombre.substring(0, Math.min(2, nombre.length()));
        return r.toUpperCase(Locale.ROOT);
    }
}
