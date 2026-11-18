/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.fx.portal.AnimacionConfirmacion;
import pe.ehuanca.trajes.fx.portal.DetalleTrajeVista;
import pe.ehuanca.trajes.fx.portal.MisDisenosVista;
import pe.ehuanca.trajes.fx.portal.MisReservasVista;
import pe.ehuanca.trajes.fx.portal.PerfilVista;
import pe.ehuanca.trajes.fx.vistas.CatalogoVista;
import pe.ehuanca.trajes.fx.vistas.InicioVista;
import pe.ehuanca.trajes.modelo.TrajeFila;
import pe.ehuanca.trajes.modelo.Usuario;
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
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class VistaPortalCliente extends StackPane {

    public static final String INICIO = "Inicio";
    public static final String CATALOGO = "Catálogo";
    public static final String DISENIO = "Diseña tu traje";
    public static final String MIS_RESERVAS = "Mis reservas";
    public static final String MIS_DISENOS = "Mis diseños";
    public static final String PERFIL = "Perfil";

    private final Servicios sErht;
    private final Runnable alCerrarSesionErht;
    private final BorderPane marcoErht = new BorderPane();
    private final Map<String, Button> botonesErht = new LinkedHashMap<>();
    private final ScrollPane areaErht = new ScrollPane();
    /** Pantalla a la que vuelve el detalle de un traje. */
    private String origenErht = INICIO;

    public VistaPortalCliente(Servicios s, Runnable alCerrarSesion) {
        this.sErht = s;
        this.alCerrarSesionErht = alCerrarSesion;
        UtilidadesInterfaz.cambiarTema("Guindo");   // el portal siempre usa la paleta guindo
        areaErht.setFitToWidth(true);
        areaErht.setFitToHeight(true);
        areaErht.getStyleClass().add("area-contenido");
        marcoErht.setTop(barraSuperiorErht());
        marcoErht.setCenter(areaErht);
        getChildren().add(marcoErht);
        ir(INICIO);
    }

    // ------------------------------------------------------------------ barra superior

    private HBox barraSuperiorErht() {
        HBox menuErht = new HBox();
        menuErht.getStyleClass().add("navegacion-menu");
        botonesErht.clear();
        for (String p : List.of(INICIO, CATALOGO, DISENIO, MIS_RESERVAS, MIS_DISENOS)) {
            Button b = new Button(p);
            b.getStyleClass().add("navegacion-opcion");
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setOnAction(e -> ir(p));
            botonesErht.put(p, b);
            menuErht.getChildren().add(b);
        }
        Region izqErht = new Region();
        Region derErht = new Region();
        HBox.setHgrow(izqErht, Priority.ALWAYS);
        HBox.setHgrow(derErht, Priority.ALWAYS);
        HBox barraErht = new HBox(Cabecera.marca(() -> ir(INICIO)), izqErht, menuErht, derErht, usuarioErht());
        barraErht.getStyleClass().add("barra-superior");
        return barraErht;
    }

    /** Avatar con el nombre del cliente; al hacer clic, menú Perfil / Cerrar sesión. */
    private HBox usuarioErht() {
        Usuario u = Sesion.actual().usuario();
        Circle circuloErht = new Circle(18);
        circuloErht.getStyleClass().add("avatar-circulo");
        Text inicialesErht = new Text(Cabecera.iniciales(u.nombreVisible()));
        inicialesErht.getStyleClass().add("avatar-texto");
        StackPane avatarErht = new StackPane(circuloErht, inicialesErht);
        Label nombreErht = new Label(u.nombreVisible());
        nombreErht.getStyleClass().add("usuario-nombre");
        Label rolErht = new Label(UtilidadesInterfaz.espaciado("Cliente"));
        rolErht.getStyleClass().add("usuario-rol");
        javafx.scene.layout.VBox textosErht = new javafx.scene.layout.VBox(0, nombreErht, rolErht);
        textosErht.setAlignment(Pos.CENTER_LEFT);
        HBox cajaErht = new HBox(10, avatarErht, textosErht, UtilidadesInterfaz.icono(Feather.CHEVRON_DOWN, 14));
        cajaErht.setAlignment(Pos.CENTER_LEFT);
        cajaErht.getStyleClass().addAll("avatar", "usuario-caja");
        cajaErht.setMinWidth(Region.USE_PREF_SIZE);

        MenuItem perfilErht = new MenuItem("Perfil", UtilidadesInterfaz.icono(Feather.USER));
        perfilErht.setOnAction(e -> ir(PERFIL));
        MenuItem cerrarErht = new MenuItem("Cerrar sesión", UtilidadesInterfaz.icono(Feather.LOG_OUT));
        cerrarErht.setOnAction(e -> alCerrarSesionErht.run());
        MenuItem acercaErht = new MenuItem("Acerca de", UtilidadesInterfaz.icono(Feather.INFO));
        acercaErht.setOnAction(e -> AcercaDe.mostrar());
        ContextMenu menuErht = new ContextMenu(perfilErht, acercaErht, new SeparatorMenuItem(), cerrarErht);
        cajaErht.setOnMouseClicked(e -> menuErht.show(cajaErht, Side.BOTTOM, 0, 8));
        return cajaErht;
    }

    /** Después de editar el perfil: el nombre del avatar cambia. */
    public void refrescarCabecera() {
        marcoErht.setTop(barraSuperiorErht());
        marcarActivoErht(PERFIL);
    }

    // ------------------------------------------------------------------ navegación

    public void ir(String pantalla) {
        if (INICIO.equals(pantalla) || CATALOGO.equals(pantalla)) {
            origenErht = pantalla;
        }
        cambiarErht(pantalla, () -> switch (pantalla) {
            case CATALOGO -> CatalogoVista.paraCliente(sErht, t -> verTraje(t, null, null));
            case DISENIO -> new pe.ehuanca.trajes.fx.portal.DisenaTuTrajeVista(sErht, this);
            case MIS_RESERVAS -> new MisReservasVista(sErht, this);
            case MIS_DISENOS -> new MisDisenosVista(sErht, this);
            case PERFIL -> new PerfilVista(sErht, this);
            default -> InicioVista.paraCliente(sErht, (t, desde, hasta, conjunto, nota) -> verTraje(t, desde, hasta));
        });
    }

    /** Abre el detalle del traje (con las fechas del buscador si las hay). */
    public void verTraje(TrajeFila t, LocalDate desde, LocalDate hasta) {
        cambiarErht(null, () -> new DetalleTrajeVista(sErht, this, t, desde, hasta));
    }

    /** Vuelve del detalle a la lista de la que vino (Inicio o Catálogo). */
    public void volver() {
        ir(origenErht);
    }

    /** Confirmación elegante con check animado encima de todo el portal. */
    public void confirmar(String titulo, String mensaje, AnimacionConfirmacion.Opcion... opciones) {
        AnimacionConfirmacion.mostrar(this, titulo, mensaje, opciones);
    }

    private void cambiarErht(String pantalla, Supplier<Node> vista) {
        UtilidadesInterfaz.ejecutar(() -> {
            areaErht.setContent(vista.get());
            areaErht.setVvalue(0);
            marcarActivoErht(pantalla);
        });
    }

    private void marcarActivoErht(String pantalla) {
        botonesErht.forEach((n, b) -> {
            b.getStyleClass().remove("activo");
            if (n.equals(pantalla)) {
                b.getStyleClass().add("activo");
            }
        });
    }
}
