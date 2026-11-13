package com.trajes.fx;

import com.trajes.fx.vistas.AlquileresVista;
import com.trajes.fx.vistas.CatalogoVista;
import com.trajes.fx.vistas.ClientesVista;
import com.trajes.fx.vistas.ConjuntosVista;
import com.trajes.fx.vistas.InicioVista;
import com.trajes.fx.vistas.NuevaReservaVista;
import com.trajes.fx.vistas.PedidosVista;
import com.trajes.fx.vistas.ReportesVista;
import com.trajes.fx.vistas.SolicitudesVista;
import com.trajes.fx.vistas.TrajesVista;
import com.trajes.fx.vistas.UsuariosVista;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.Usuario;
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
 */
public class PanelVendedorView extends BorderPane {

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

    /** Pantallas que solo ve el ADMIN. */
    private static final List<String> SOLO_ADMIN = List.of(REPORTES, USUARIOS);

    private final Servicios servicios;
    private final Runnable alCerrarSesion;
    private final boolean admin;
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final Label insignia = new Label();
    /** Pedidos de diseño PENDIENTES (por cotizar). */
    private final Label insigniaPedidos = new Label();
    private final ScrollPane area = new ScrollPane();
    private final Timeline refresco;

    public PanelVendedorView(Servicios servicios, Runnable alCerrarSesion) {
        this.servicios = servicios;
        this.alCerrarSesion = alCerrarSesion;
        this.admin = Usuario.ADMIN.equals(Sesion.get().usuario().rol());
        area.setFitToWidth(true);
        area.setFitToHeight(true);
        area.getStyleClass().add("area-contenido");
        setLeft(barraLateral());
        setCenter(area);

        // La insignia de Solicitudes se actualiza sola cada minuto (los clientes piden desde su portal).
        refresco = new Timeline(new KeyFrame(Duration.seconds(60), e -> actualizarInsignia()));
        refresco.setCycleCount(Timeline.INDEFINITE);
        refresco.play();
        sceneProperty().addListener((o, antes, ahora) -> {
            if (ahora == null) {
                refresco.stop();
            }
        });
        mostrar(INICIO);
    }

    // ------------------------------------------------------------------ barra lateral

    private Node barraLateral() {
        Button nueva = Ui.estilo(new Button("Nueva reserva", Ui.icono(Feather.PLUS)), "boton-primario");
        nueva.setMaxWidth(Double.MAX_VALUE);
        nueva.setOnAction(e -> mostrar(NUEVA_RESERVA));
        VBox.setMargin(nueva, new Insets(4, 18, 10, 18));

        HBox marca = Cabecera.marca(() -> mostrar(INICIO));
        marca.getStyleClass().add("menu-marca-lateral");
        VBox menu = new VBox(marca, nueva);
        menu.getStyleClass().add("menu-lateral");

        menu.getChildren().add(grupo("Principal"));
        item(menu, INICIO, Feather.HOME);
        item(menu, SOLICITUDES, Feather.INBOX);
        item(menu, CATALOGO, Feather.GRID);
        item(menu, ALQUILERES, Feather.CALENDAR);
        item(menu, CLIENTES, Feather.USERS);
        menu.getChildren().add(grupo("Taller"));
        item(menu, PEDIDOS, Feather.SCISSORS);
        menu.getChildren().add(grupo("Inventario"));
        item(menu, TRAJES, Feather.PACKAGE);
        item(menu, CONJUNTOS, Feather.LAYERS);
        if (admin) {
            menu.getChildren().add(grupo("Administración"));
            item(menu, REPORTES, Feather.BAR_CHART_2);
            item(menu, USUARIOS, Feather.SHIELD);
        }

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);
        menu.getChildren().addAll(espacio, tarjetaUsuario());

        ScrollPane scroll = new ScrollPane(menu);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("menu-scroll");
        scroll.setMinWidth(248);
        scroll.setPrefWidth(248);
        return scroll;
    }

    private static Label grupo(String texto) {
        Label l = new Label(Ui.espaciado(texto));
        l.getStyleClass().add("menu-grupo");
        return l;
    }

    private void item(VBox menu, String pantalla, Ikon ikon) {
        Button b = new Button(pantalla, Ui.icono(ikon));
        b.getStyleClass().add("menu-boton");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setOnAction(e -> mostrar(pantalla));
        // Animación suave: el ítem se desliza unos píxeles al pasar el mouse.
        TranslateTransition t = new TranslateTransition(Duration.millis(150), b);
        b.hoverProperty().addListener((o, antes, encima) -> {
            t.stop();
            t.setToX(encima && !b.getStyleClass().contains("activo") ? 4 : 0);
            t.play();
        });
        botones.put(pantalla, b);
        Label badge = SOLICITUDES.equals(pantalla) ? insignia : PEDIDOS.equals(pantalla) ? insigniaPedidos : null;
        if (badge != null) {
            badge.getStyleClass().add("insignia");
            badge.setMouseTransparent(true);
            StackPane conInsignia = new StackPane(b, badge);
            StackPane.setAlignment(badge, Pos.CENTER_RIGHT);
            StackPane.setMargin(badge, new Insets(0, 22, 0, 0));
            menu.getChildren().add(conInsignia);
            actualizarInsignia();
        } else {
            menu.getChildren().add(b);
        }
    }

    /** Usuario de la sesión abajo: avatar (menú Tema / Cerrar sesión / Salir), nombre, rol y botón de salida. */
    private HBox tarjetaUsuario() {
        Usuario u = Sesion.get().usuario();
        Label nombre = new Label(u.nombreVisible());
        nombre.getStyleClass().add("usuario-nombre");
        Label rol = new Label(Ui.espaciado(u.rolVisible()));
        rol.getStyleClass().add("usuario-rol");
        VBox textos = new VBox(1, nombre, rol);
        textos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox caja = new HBox(10, Cabecera.avatar(alCerrarSesion), textos, Cabecera.botonCerrarSesion(alCerrarSesion));
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.getStyleClass().add("menu-usuario-lateral");
        return caja;
    }

    /** Insignias rojas: solicitudes por aprobar y pedidos de diseño por cotizar (ocultas si no hay). */
    public void actualizarInsignia() {
        pintar(insignia, () -> servicios.alquileres().contarSolicitudes());
        pintar(insigniaPedidos, () -> servicios.personalizador().contarPendientes());
    }

    private static void pintar(Label badge, java.util.function.IntSupplier contar) {
        int n;
        try {
            n = contar.getAsInt();
        } catch (RuntimeException e) {
            n = 0;   // sin conexión: no se muestra la insignia
        }
        badge.setText(n > 99 ? "99+" : String.valueOf(n));
        badge.setVisible(n > 0);
    }

    // ------------------------------------------------------------------ navegación

    /** Cambia la pantalla (se crea de nuevo para que muestre datos actualizados). */
    public void mostrar(String pantalla) {
        if (SOLO_ADMIN.contains(pantalla) && !admin) {
            Ui.error("Esta opción solo está disponible para el administrador.");
            return;
        }
        cambiar(pantalla, () -> switch (pantalla) {
            case SOLICITUDES -> new SolicitudesVista(servicios, this::actualizarInsignia);
            case CATALOGO -> CatalogoVista.paraTienda(servicios, this);
            case NUEVA_RESERVA -> new NuevaReservaVista(servicios, this);
            case ALQUILERES -> new AlquileresVista(servicios);
            case TRAJES -> new TrajesVista(servicios);
            case CLIENTES -> new ClientesVista(servicios);
            case CONJUNTOS -> new ConjuntosVista(servicios);
            case PEDIDOS -> new PedidosVista(servicios, this::actualizarInsignia);
            case REPORTES -> new ReportesVista(servicios);
            case USUARIOS -> new UsuariosVista(servicios);
            default -> InicioVista.paraTienda(servicios,
                    (t, d, h, c, n) -> reservar(t.idTraje(), d, h, c, n), () -> mostrar(SOLICITUDES));
        });
    }

    /** Abre Nueva reserva con las fechas, el conjunto y el traje ya puestos. */
    public void reservar(int idTraje, LocalDate desde, LocalDate hasta, Opcion conjunto, String observacion) {
        cambiar(NUEVA_RESERVA, () -> {
            NuevaReservaVista v = new NuevaReservaVista(servicios, this);
            v.preparar(idTraje, desde, hasta, conjunto, observacion);
            return v;
        });
    }

    private void cambiar(String pantalla, Supplier<Node> vista) {
        Ui.ejecutar(() -> {
            area.setContent(vista.get());
            area.setVvalue(0);
            botones.forEach((nombre, boton) -> {
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
