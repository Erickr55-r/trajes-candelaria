/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Adornos;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.Ocupacion;
import pe.ehuanca.trajes.modelo.TrajeFila;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tarjeta de un traje (estilo tienda web) y utilidades para mostrarlas en cuadrícula.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class TarjetaTraje {

    public static final double ALTO_FOTO = 250;

    /** Punto de color del chip de estado. */
    public enum Punto { VERDE, AMBAR, ROJO }

    /** Todo lo que muestra una tarjeta. */
    public record Datos(TrajeFila traje, boolean masAlquilado, String estadoTexto, Punto punto,
                        List<String> piezas, String tallas, String sufijoPrecio, boolean reservable,
                        Runnable alReservar, Runnable alDobleClic, String textoBoton, boolean abrirConUnClic) {

        /** Tarjeta de la tienda: botón "Reservar" y doble clic para el detalle. */
        public Datos(TrajeFila traje, boolean masAlquilado, String estadoTexto, Punto punto, List<String> piezas,
                     String tallas, String sufijoPrecio, boolean reservable, Runnable alReservar, Runnable alDobleClic) {
            this(traje, masAlquilado, estadoTexto, punto, piezas, tallas, sufijoPrecio, reservable, alReservar,
                    alDobleClic, "Reservar", false);
        }
    }

    private TarjetaTraje() {
    }

    public static VBox crear(Datos d) {
        TrajeFila t = d.traje();

        // ---------------- foto
        StackPane foto = foto(t);
        if (d.masAlquilado()) {
            Label etiquetaMasAlquilado = new Label(UtilidadesInterfaz.espaciado("Más alquilado"), UtilidadesInterfaz.icono(Feather.AWARD));
            etiquetaMasAlquilado.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-oro");
            StackPane.setAlignment(etiquetaMasAlquilado, Pos.TOP_LEFT);
            StackPane.setMargin(etiquetaMasAlquilado, new Insets(14));
            foto.getChildren().add(etiquetaMasAlquilado);
        }
        Circle punto = new Circle(4);
        punto.getStyleClass().add(switch (d.punto()) {
            case VERDE -> "punto-verde";
            case AMBAR -> "punto-ambar";
            case ROJO -> "punto-rojo";
        });
        Label estado = new Label(d.estadoTexto(), punto);
        estado.setGraphicTextGap(7);
        estado.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-blanco");
        StackPane.setAlignment(estado, Pos.BOTTOM_LEFT);
        StackPane.setMargin(estado, new Insets(14));
        foto.getChildren().add(estado);

        // ---------------- cuerpo
        Label meta = new Label(UtilidadesInterfaz.espaciado(t.danza()) + "  •  " + UtilidadesInterfaz.espaciado(t.color() == null ? "—" : t.color()));
        meta.getStyleClass().add("traje-datos");
        meta.setMinWidth(0);
        long n = t.vecesAlquilado();
        Label veces = new Label("Alquilado " + n + (n == 1 ? " vez" : " veces"));
        veces.getStyleClass().add("traje-veces");
        veces.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaMeta = new HBox(8, meta, UtilidadesInterfaz.espacio(), veces);
        lineaMeta.setAlignment(Pos.CENTER_LEFT);

        Label nombre = new Label(t.personaje());
        nombre.getStyleClass().add("traje-nombre");
        Label codigo = new Label(t.codigo());
        codigo.getStyleClass().add("traje-codigo");
        codigo.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaNombre = new HBox(8, nombre, UtilidadesInterfaz.espacio(), codigo);
        lineaNombre.setAlignment(Pos.BASELINE_LEFT);

        Label desc = new Label(descripcion(t));
        desc.getStyleClass().add("traje-desc");
        desc.setWrapText(true);
        desc.setMinHeight(40);
        desc.setPrefHeight(40);
        desc.setMaxHeight(40);   // dos líneas

        Region sep1 = separador();
        String incluye = d.piezas().isEmpty() ? "Sin piezas registradas" : String.join(", ", d.piezas());
        Label lIncluye = detalle(Feather.PACKAGE, "Incluye: ", incluye);
        lIncluye.setTooltip(new Tooltip(incluye));
        VBox detalles = new VBox(7,
                lIncluye,
                detalle(Feather.MAXIMIZE_2, "Tallas: ", d.tallas()),
                detalle(Feather.SHIELD, "Garantía: ", UtilidadesInterfaz.soles(t.garantia()) + " (se devuelve sin daños)"));

        Region sep2 = separador();
        Label precio = new Label(UtilidadesInterfaz.soles(t.precioAlquiler()));
        precio.getStyleClass().add("traje-precio");
        precio.setMinWidth(Region.USE_PREF_SIZE);
        Label sufijo = new Label(d.sufijoPrecio());
        sufijo.getStyleClass().add("traje-precio-nota");
        sufijo.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaPrecio = new HBox(6, precio, sufijo);
        lineaPrecio.setAlignment(Pos.BASELINE_LEFT);

        Button reservar = UtilidadesInterfaz.estilo(new Button(d.textoBoton(), UtilidadesInterfaz.icono(Feather.ARROW_RIGHT)), "boton-guindo");
        reservar.setContentDisplay(ContentDisplay.RIGHT);
        reservar.setMinWidth(Region.USE_PREF_SIZE);
        reservar.setDisable(!d.reservable());
        reservar.setOnAction(e -> d.alReservar().run());
        HBox pie = new HBox(10, lineaPrecio, UtilidadesInterfaz.espacio(), reservar);
        pie.setAlignment(Pos.CENTER_LEFT);

        VBox cuerpo = new VBox(lineaMeta, lineaNombre, desc, sep1, detalles, sep2, pie);
        cuerpo.getStyleClass().add("traje-cuerpo");
        VBox.setMargin(sep1, new Insets(4, 0, 4, 0));
        VBox.setMargin(sep2, new Insets(4, 0, 2, 0));

        VBox tarjeta = new VBox(foto, cuerpo);
        tarjeta.getStyleClass().add("traje-tarjeta");
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        tarjeta.setMinWidth(0);

        // Al pasar el mouse sube 4 px y crece la sombra (150 ms).
        TranslateTransition sube = new TranslateTransition(Duration.millis(150), tarjeta);
        tarjeta.hoverProperty().addListener((o, antes, encima) -> {
            sube.stop();
            sube.setToY(encima ? -4 : 0);
            sube.play();
            if (encima) {
                tarjeta.getStyleClass().add("elevada");
            } else {
                tarjeta.getStyleClass().remove("elevada");
            }
        });
        if (d.alDobleClic() != null) {
            if (d.abrirConUnClic()) {
                tarjeta.setStyle("-fx-cursor: hand;");
            }
            tarjeta.setOnMouseClicked(e -> {
                if (e.getClickCount() == (d.abrirConUnClic() ? 1 : 2)) {
                    d.alDobleClic().run();
                }
            });
        }
        return tarjeta;
    }

    // ------------------------------------------------------------------ foto

    private static StackPane foto(TrajeFila t) {
        StackPane caja = new StackPane();
        caja.setMinHeight(ALTO_FOTO);
        caja.setPrefHeight(ALTO_FOTO);
        caja.setMaxHeight(ALTO_FOTO);

        URL ruta = rutaFoto(t.idTraje());
        if (ruta != null) {
            // "cover": llena el recuadro y recorta lo que sobra, sin deformar.
            caja.setStyle("-fx-background-image: url('" + ruta.toExternalForm() + "');"
                    + "-fx-background-size: cover; -fx-background-position: center;");
        } else {
            String color = Adornos.colorDanza(t.danza());
            caja.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + color + ", 18%), "
                    + "derive(" + color + ", -42%));");
            Group chakana = Adornos.chakanaEnGrupo(230, "chakana-tenue");
            StackPane.setAlignment(chakana, Pos.CENTER_RIGHT);
            StackPane.setMargin(chakana, new Insets(0, -70, 0, 0));
            Label danza = new Label(t.danza());
            danza.getStyleClass().add("marcador-danza");
            danza.setWrapText(true);
            Label sub = new Label(UtilidadesInterfaz.espaciado(t.personaje()));
            sub.getStyleClass().add("marcador-sub");
            VBox textos = new VBox(4, danza, sub);
            textos.setAlignment(Pos.CENTER);
            caja.getChildren().addAll(chakana, textos);
        }

        // Esquinas superiores redondeadas: rectángulo con arcos + franja recta abajo.
        Rectangle redondo = new Rectangle();
        redondo.setArcWidth(36);
        redondo.setArcHeight(36);
        redondo.widthProperty().bind(caja.widthProperty());
        redondo.setHeight(ALTO_FOTO);
        Rectangle recto = new Rectangle(0, ALTO_FOTO - 20, 0, 20);
        recto.widthProperty().bind(caja.widthProperty());
        caja.setClip(new Group(redondo, recto));
        return caja;
    }

    private static URL rutaFoto(int idTraje) {
        for (String extension : new String[]{"png", "jpg"}) {
            URL u = TarjetaTraje.class.getResource("/img/trajes/" + idTraje + "." + extension);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    private static Region separador() {
        Region r = new Region();
        r.getStyleClass().add("traje-separador");
        return r;
    }

    private static Label detalle(Feather ikon, String titulo, String valor) {
        Label l = new Label(titulo + valor, UtilidadesInterfaz.icono(ikon));
        l.setGraphicTextGap(8);
        l.getStyleClass().add("traje-detalle");
        l.setMinWidth(0);
        return l;
    }

    private static String descripcion(TrajeFila t) {
        String genero = switch (t.genero() == null ? "" : t.genero()) {
            case "VARON" -> "para varón";
            case "MUJER" -> "para dama";
            default -> "unisex";
        };
        String color = t.color() == null ? "" : " en " + t.color().toLowerCase(Locale.ROOT);
        return "Traje completo de " + t.danza().toLowerCase(Locale.ROOT) + " " + genero + color
                + ", listo para la festividad. Se entrega revisado pieza por pieza.";
    }

    // ------------------------------------------------------------------ utilidades

    /** "M · también S, L": la talla del traje y las de los trajes iguales (misma danza, personaje y color). */
    public static String tallas(TrajeFila t, List<TrajeFila> todos) {
        List<String> otras = new ArrayList<>();
        for (String talla : UtilidadesInterfaz.TALLAS) {
            if (talla.equals(t.talla())) {
                continue;
            }
            boolean hay = todos.stream().anyMatch(o -> talla.equals(o.talla())
                    && o.danza().equals(t.danza()) && o.personaje().equals(t.personaje())
                    && java.util.Objects.equals(o.color(), t.color()) && o.genero().equals(t.genero()));
            if (hay) {
                otras.add(talla);
            }
        }
        return otras.isEmpty() ? t.talla() : t.talla() + " · también " + String.join(", ", otras);
    }

    /**
     * Primer día (desde hoy) en que cada traje no está comprometido en un alquiler reservado o entregado.
     * Un alquiler entregado y vencido se cuenta hasta hoy (el traje aún no volvió).
     */
    public static Map<Integer, LocalDate> proximaFechaLibre(List<Ocupacion> ocupaciones, LocalDate hoy) {
        Map<Integer, List<Ocupacion>> porTraje = new HashMap<>();
        for (Ocupacion o : ocupaciones) {
            porTraje.computeIfAbsent(o.idTraje(), k -> new ArrayList<>()).add(o);
        }
        Map<Integer, LocalDate> libre = new HashMap<>();
        porTraje.forEach((id, lista) -> {
            LocalDate d = hoy;
            boolean cambio = true;
            while (cambio) {
                cambio = false;
                for (Ocupacion o : lista) {
                    LocalDate fin = o.devolucionPrevista();
                    if ("ENTREGADO".equals(o.estado()) && fin.isBefore(hoy)) {
                        fin = hoy;
                    }
                    if (!o.salida().isAfter(d) && !fin.isBefore(d)) {
                        d = fin.plusDays(1);
                        cambio = true;
                    }
                }
            }
            libre.put(id, d);
        });
        return libre;
    }

    /** Cuadrícula de tarjetas: 3 columnas, o 2 si el ancho disponible es angosto. */
    public static final class Cuadricula extends GridPane {

        private static final double ANCHO_MINIMO_3 = 1100;
        private List<Node> tarjetas = List.of();
        private int columnas;

        public Cuadricula() {
            setHgap(26);
            setVgap(26);
            widthProperty().addListener((o, a, b) -> {
                int c = b.doubleValue() >= ANCHO_MINIMO_3 ? 3 : 2;
                if (c != columnas) {
                    ordenar(c);
                }
            });
        }

        public void mostrar(List<Node> nuevas) {
            tarjetas = nuevas;
            ordenar(getWidth() == 0 ? 3 : (getWidth() >= ANCHO_MINIMO_3 ? 3 : 2));
        }

        private void ordenar(int c) {
            columnas = c;
            getChildren().clear();
            getColumnConstraints().clear();
            for (int i = 0; i < c; i++) {
                // Columnas iguales: mismo ancho preferido y todas crecen (sin percentWidth).
                ColumnConstraints cc = new ColumnConstraints();
                cc.setMinWidth(0);
                cc.setPrefWidth(100);
                cc.setHgrow(Priority.ALWAYS);
                cc.setFillWidth(true);
                getColumnConstraints().add(cc);
            }
            for (int i = 0; i < tarjetas.size(); i++) {
                Node n = tarjetas.get(i);
                GridPane.setFillWidth(n, true);
                add(n, i % c, i / c);
            }
        }
    }
}
