/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import pe.ehuanca.trajes.fx.vistas.AlquileresVista;
import pe.ehuanca.trajes.fx.vistas.CatalogoVista;
import pe.ehuanca.trajes.fx.vistas.ClientesVista;
import pe.ehuanca.trajes.fx.vistas.ConjuntosVista;
import pe.ehuanca.trajes.fx.vistas.InicioVista;
import pe.ehuanca.trajes.fx.vistas.NuevaReservaVista;
import pe.ehuanca.trajes.fx.vistas.PedidosVista;
import pe.ehuanca.trajes.fx.vistas.ReportesVista;
import pe.ehuanca.trajes.fx.vistas.SolicitudesVista;
import pe.ehuanca.trajes.fx.vistas.TrajesVista;
import pe.ehuanca.trajes.fx.vistas.UsuariosVista;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.Usuario;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Panel de la tienda (ADMIN y VENDEDOR): barra lateral guindo a la izquierda y la pantalla elegida (con scroll).
 * Reportes y Usuarios solo los ve el ADMIN.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class VistaPanelVendedor extends BorderPane {

    public static final String INICIO = "Inicio";
    public static final String SOLICITUDES = "Solicitudes";
    public static final String CATALOGO = "Catálogo";
    public static final String NUEVA_RESERVA = "Nueva reserva";
    public static final String ALQUILERES = "Alquileres";
    public static final String CLIENTES = "Clientes";
    public static final String PEDIDOS = "Pedidos de diseño";
    public static final String TRAJES = "Inventario";
    public static final String CONJUNTOS = "Conjuntos";
    public static final String REPORTES = "Reportes";
    public static final String USUARIOS = "Usuarios";

    private static final List<String> SOLO_ADMIN = List.of(REPORTES, USUARIOS);

    private final Servicios serviciosErht;
    private final Runnable alCerrarSesionErht;
    private final boolean adminErht;
    private final Map<String, Button> botonesErht = new LinkedHashMap<>();
    private final Label insigniaErht = new Label();
    private final Label insigniaPedidosErht = new Label();
    private final ScrollPane areaErht = new ScrollPane();
    private final Timeline refrescoErht;

    public VistaPanelVendedor(Servicios servicios, Runnable alCerrarSesion) {
        this.serviciosErht = servicios;
        this.alCerrarSesionErht = alCerrarSesion;
        this.adminErht = Usuario.ADMIN.equals(Sesion.actual().usuario().rol());
        areaErht.setFitToWidth(true);
        areaErht.setFitToHeight(true);
        areaErht.getStyleClass().add("area-contenido");
        setLeft(barraLateralErht());
        setCenter(areaErht);

        refrescoErht = new Timeline(new KeyFrame(Duration.seconds(60), e -> actualizarInsignia()));
        refrescoErht.setCycleCount(Timeline.INDEFINITE);
        refrescoErht.play();
        sceneProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                refrescoErht.stop();
            }
        });
        mostrar(INICIO);
    }

    private Node barraLateralErht() {
        Button nuevaErht = UtilidadesInterfaz.estilo(new Button("Nueva reserva", UtilidadesInterfaz.icono(Feather.PLUS)), "boton-primario");
        nuevaErht.setMaxWidth(Double.MAX_VALUE);
        nuevaErht.setOnAction(e -> mostrar(NUEVA_RESERVA));
        VBox.setMargin(nuevaErht, new Insets(4, 18, 10, 18));

        HBox marcaErht = Cabecera.marca(() -> mostrar(INICIO));
        marcaErht.getStyleClass().add("menu-marca-lateral");
        VBox menuErht = new VBox(marcaErht, nuevaErht);
        menuErht.getStyleClass().add("menu-lateral");

        menuErht.getChildren().add(grupoErht("Principal"));
        agregarOpcionMenuErht(menuErht, INICIO, Feather.HOME);
        agregarOpcionMenuErht(menuErht, SOLICITUDES, Feather.INBOX);
        agregarOpcionMenuErht(menuErht, CATALOGO, Feather.GRID);
        agregarOpcionMenuErht(menuErht, ALQUILERES, Feather.CALENDAR);
        agregarOpcionMenuErht(menuErht, CLIENTES, Feather.USERS);
        menuErht.getChildren().add(grupoErht("Taller"));
        agregarOpcionMenuErht(menuErht, PEDIDOS, Feather.SCISSORS);
        menuErht.getChildren().add(grupoErht("Inventario"));
        agregarOpcionMenuErht(menuErht, TRAJES, Feather.PACKAGE);
        agregarOpcionMenuErht(menuErht, CONJUNTOS, Feather.LAYERS);
        if (adminErht) {
            menuErht.getChildren().add(grupoErht("Administración"));
            agregarOpcionMenuErht(menuErht, REPORTES, Feather.BAR_CHART_2);
            agregarOpcionMenuErht(menuErht, USUARIOS, Feather.SHIELD);
        }

        Region espacioErht = new Region();
        VBox.setVgrow(espacioErht, Priority.ALWAYS);
        menuErht.getChildren().addAll(espacioErht, tarjetaUsuarioErht());

        ScrollPane desplazableErht = new ScrollPane(menuErht);
        desplazableErht.setFitToWidth(true);
        desplazableErht.setFitToHeight(true);
        desplazableErht.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazableErht.getStyleClass().add("menu-scroll");
        desplazableErht.setMinWidth(248);
        desplazableErht.setPrefWidth(248);
        return desplazableErht;
    }

    private static Label grupoErht(String texto) {
        Label l = new Label(UtilidadesInterfaz.espaciado(texto));
        l.getStyleClass().add("menu-grupo");
        return l;
    }

    private void agregarOpcionMenuErht(VBox menu, String pantalla, Ikon ikon) {
        Button b = new Button(pantalla, UtilidadesInterfaz.icono(ikon));
        b.getStyleClass().add("menu-boton");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> mostrar(pantalla));
        TranslateTransition t = new TranslateTransition(Duration.millis(150), b);
        b.hoverProperty().addListener((o, antes, encima) -> {
            t.stop();
            t.setToX(encima && !b.getStyleClass().contains("activo") ? 4 : 0);
            t.play();
        });
        botonesErht.put(pantalla, b);
        Label insigniaOpcionErht = SOLICITUDES.equals(pantalla) ? insigniaErht : PEDIDOS.equals(pantalla) ? insigniaPedidosErht : null;
        if (insigniaOpcionErht != null) {
            insigniaOpcionErht.getStyleClass().add("insignia");
            insigniaOpcionErht.setMouseTransparent(true);
            StackPane conInsigniaErht = new StackPane(b, insigniaOpcionErht);
            StackPane.setAlignment(insigniaOpcionErht, Pos.CENTER_RIGHT);
            StackPane.setMargin(insigniaOpcionErht, new Insets(0, 22, 0, 0));
            menu.getChildren().add(conInsigniaErht);
            actualizarInsignia();
        } else {
            menu.getChildren().add(b);
        }
    }

    private HBox tarjetaUsuarioErht() {
        Usuario u = Sesion.actual().usuario();
        Label nombreErht = new Label(u.nombreVisible());
        nombreErht.getStyleClass().add("usuario-nombre");
        Label rolErht = new Label(UtilidadesInterfaz.espaciado(u.rolVisible()));
        rolErht.getStyleClass().add("usuario-rol");
        VBox textosErht = new VBox(1, nombreErht, rolErht);
        textosErht.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textosErht, Priority.ALWAYS);
        HBox cajaErht = new HBox(10, Cabecera.avatar(alCerrarSesionErht), textosErht, Cabecera.botonCerrarSesion(alCerrarSesionErht));
        cajaErht.setAlignment(Pos.CENTER_LEFT);
        cajaErht.getStyleClass().add("menu-usuario-lateral");
        return cajaErht;
    }

    public void actualizarInsignia() {
        pintarErht(insigniaErht, () -> serviciosErht.alquileres().contarSolicitudes());
        pintarErht(insigniaPedidosErht, () -> serviciosErht.personalizador().contarPendientes());
    }

    private static void pintarErht(Label insigniaOpcion, java.util.function.IntSupplier contar) {
        int n;
        try {
            n = contar.getAsInt();
        } catch (RuntimeException e) {
            n = 0;
        }
        insigniaOpcion.setText(n > 99 ? "99+" : String.valueOf(n));
        insigniaOpcion.setVisible(n > 0);
    }

    public void mostrar(String pantalla) {
        if (SOLO_ADMIN.contains(pantalla) && !adminErht) {
            UtilidadesInterfaz.mostrarError("Esta opción solo está disponible para el administrador.");
            return;
        }
        cambiarErht(pantalla, () -> switch (pantalla) {
            case SOLICITUDES -> new SolicitudesVista(serviciosErht, this::actualizarInsignia);
            case CATALOGO -> CatalogoVista.paraTienda(serviciosErht, this);
            case NUEVA_RESERVA -> new NuevaReservaVista(serviciosErht, this);
            case ALQUILERES -> new AlquileresVista(serviciosErht);
            case TRAJES -> new TrajesVista(serviciosErht);
            case CLIENTES -> new ClientesVista(serviciosErht);
            case CONJUNTOS -> new ConjuntosVista(serviciosErht);
            case PEDIDOS -> new PedidosVista(serviciosErht, this::actualizarInsignia);
            case REPORTES -> new ReportesVista(serviciosErht);
            case USUARIOS -> new UsuariosVista(serviciosErht);
            default -> InicioVista.paraTienda(serviciosErht,
                    (t, d, h, c, n) -> reservar(t.idTraje(), d, h, c, n), () -> mostrar(SOLICITUDES));
        });
    }

    public void reservar(int idTraje, LocalDate desde, LocalDate hasta, Opcion conjunto, String observacion) {
        cambiarErht(NUEVA_RESERVA, () -> {
            NuevaReservaVista v = new NuevaReservaVista(serviciosErht, this);
            v.preparar(idTraje, desde, hasta, conjunto, observacion);
            return v;
        });
    }

    private void cambiarErht(String pantalla, Supplier<Node> vista) {
        UtilidadesInterfaz.ejecutar(() -> {
            areaErht.setContent(vista.get());
            areaErht.setVvalue(0);
            botonesErht.forEach((nombre, boton) -> {
                boton.getStyleClass().remove("activo");
                boton.setTranslateX(0);
                if (nombre.equals(pantalla)) {
                    boton.getStyleClass().add("activo");
                }
            });
            actualizarInsignia();
        });
    }
}
