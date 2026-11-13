package com.trajes.fx;

import com.trajes.fx.portal.ConfirmacionCheck;
import com.trajes.fx.portal.DetalleTrajeVista;
import com.trajes.fx.portal.MisDisenosVista;
import com.trajes.fx.portal.MisReservasVista;
import com.trajes.fx.portal.PerfilVista;
import com.trajes.fx.vistas.CatalogoVista;
import com.trajes.fx.vistas.InicioVista;
import com.trajes.modelo.TrajeFila;
import com.trajes.modelo.Usuario;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Portal del cliente (rol CLIENTE), con el tema guindo: Inicio · Catálogo · Diseña tu traje · Mis reservas ·
 * Mis diseños, el detalle de cada traje y el perfil. Todo lo que muestra se filtra por el cliente de la sesión.
 */
public class PortalClienteView extends StackPane {

    public static final String INICIO = "Inicio";
    public static final String CATALOGO = "Catálogo";
    public static final String DISENIO = "Diseña tu traje";
    public static final String MIS_RESERVAS = "Mis reservas";
    public static final String MIS_DISENOS = "Mis diseños";
    public static final String PERFIL = "Perfil";

    private final Servicios s;
    private final Runnable alCerrarSesion;
    private final BorderPane marco = new BorderPane();
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final ScrollPane area = new ScrollPane();
    /** Pantalla a la que vuelve el detalle de un traje. */
    private String origen = INICIO;

    public PortalClienteView(Servicios s, Runnable alCerrarSesion) {
        this.s = s;
        this.alCerrarSesion = alCerrarSesion;
        Ui.cambiarTema("Guindo");   // el portal siempre usa la paleta guindo
        area.setFitToWidth(true);
        area.setFitToHeight(true);
        area.getStyleClass().add("area-contenido");
        marco.setTop(barraSuperior());
        marco.setCenter(area);
        getChildren().add(marco);
        ir(INICIO);
    }

    // ------------------------------------------------------------------ barra superior

    private HBox barraSuperior() {
        HBox menu = new HBox();
        menu.getStyleClass().add("nav-menu");
        botones.clear();
        for (String p : List.of(INICIO, CATALOGO, DISENIO, MIS_RESERVAS, MIS_DISENOS)) {
            Button b = new Button(p);
            b.getStyleClass().add("nav-item");
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setOnAction(e -> ir(p));
            botones.put(p, b);
            menu.getChildren().add(b);
        }
        Region izq = new Region();
        Region der = new Region();
        HBox.setHgrow(izq, Priority.ALWAYS);
        HBox.setHgrow(der, Priority.ALWAYS);
        HBox barra = new HBox(Cabecera.marca(() -> ir(INICIO)), izq, menu, der, usuario());
        barra.getStyleClass().add("barra-superior");
        return barra;
    }

    /** Avatar con el nombre del cliente; al hacer clic, menú Perfil / Cerrar sesión. */
    private HBox usuario() {
        Usuario u = Sesion.get().usuario();
        Circle circulo = new Circle(18);
        circulo.getStyleClass().add("avatar-circulo");
        Text iniciales = new Text(Cabecera.iniciales(u.nombreVisible()));
        iniciales.getStyleClass().add("avatar-texto");
        StackPane avatar = new StackPane(circulo, iniciales);
        Label nombre = new Label(u.nombreVisible());
        nombre.getStyleClass().add("usuario-nombre");
        Label rol = new Label(Ui.espaciado("Cliente"));
        rol.getStyleClass().add("usuario-rol");
        javafx.scene.layout.VBox textos = new javafx.scene.layout.VBox(0, nombre, rol);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox caja = new HBox(10, avatar, textos, Ui.icono(Feather.CHEVRON_DOWN, 14));
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().addAll("avatar", "usuario-caja");
        caja.setMinWidth(Region.USE_PREF_SIZE);

        MenuItem perfil = new MenuItem("Perfil", Ui.icono(Feather.USER));
        perfil.setOnAction(e -> ir(PERFIL));
        MenuItem cerrar = new MenuItem("Cerrar sesión", Ui.icono(Feather.LOG_OUT));
        cerrar.setOnAction(e -> alCerrarSesion.run());
        ContextMenu menu = new ContextMenu(perfil, new SeparatorMenuItem(), cerrar);
        caja.setOnMouseClicked(e -> menu.show(caja, Side.BOTTOM, 0, 8));
        return caja;
    }

    /** Después de editar el perfil: el nombre del avatar cambia. */
    public void refrescarCabecera() {
        marco.setTop(barraSuperior());
        marcarActivo(PERFIL);
    }

    // ------------------------------------------------------------------ navegación

    public void ir(String pantalla) {
        if (INICIO.equals(pantalla) || CATALOGO.equals(pantalla)) {
            origen = pantalla;
        }
        cambiar(pantalla, () -> switch (pantalla) {
            case CATALOGO -> CatalogoVista.paraCliente(s, t -> verTraje(t, null, null));
            case DISENIO -> new com.trajes.fx.portal.DisenaTuTrajeVista(s, this);
            case MIS_RESERVAS -> new MisReservasVista(s, this);
            case MIS_DISENOS -> new MisDisenosVista(s, this);
            case PERFIL -> new PerfilVista(s, this);
            default -> InicioVista.paraCliente(s, (t, desde, hasta, conjunto, nota) -> verTraje(t, desde, hasta));
        });
    }

    /** Abre el detalle del traje (con las fechas del buscador si las hay). */
    public void verTraje(TrajeFila t, LocalDate desde, LocalDate hasta) {
        cambiar(null, () -> new DetalleTrajeVista(s, this, t, desde, hasta));
    }

    /** Vuelve del detalle a la lista de la que vino (Inicio o Catálogo). */
    public void volver() {
        ir(origen);
    }

    /** Confirmación elegante con check animado encima de todo el portal. */
    public void confirmar(String titulo, String mensaje, ConfirmacionCheck.Opcion... opciones) {
        ConfirmacionCheck.mostrar(this, titulo, mensaje, opciones);
    }

    private void cambiar(String pantalla, Supplier<Node> vista) {
        Ui.ejecutar(() -> {
            area.setContent(vista.get());
            area.setVvalue(0);
            marcarActivo(pantalla);
        });
    }

    private void marcarActivo(String pantalla) {
        botones.forEach((n, b) -> {
            b.getStyleClass().remove("activo");
            if (n.equals(pantalla)) {
                b.getStyleClass().add("activo");
            }
        });
    }
}
