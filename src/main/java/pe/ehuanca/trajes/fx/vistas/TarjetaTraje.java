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

    public enum Punto { VERDE, AMBAR, ROJO }

    public record Datos(TrajeFila traje, boolean masAlquilado, String estadoTexto, Punto punto,
                        List<String> piezas, String tallas, String sufijoPrecio, boolean reservable,
                        Runnable alReservar, Runnable alDobleClic, String textoBoton, boolean abrirConUnClic) {

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

        StackPane fotoErht = fotoErht(t);
        if (d.masAlquilado()) {
            Label etiquetaMasAlquiladoErht = new Label(UtilidadesInterfaz.espaciado("Más alquilado"), UtilidadesInterfaz.icono(Feather.AWARD));
            etiquetaMasAlquiladoErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-oro");
            StackPane.setAlignment(etiquetaMasAlquiladoErht, Pos.TOP_LEFT);
            StackPane.setMargin(etiquetaMasAlquiladoErht, new Insets(14));
            fotoErht.getChildren().add(etiquetaMasAlquiladoErht);
        }
        Circle puntoErht = new Circle(4);
        puntoErht.getStyleClass().add(switch (d.punto()) {
            case VERDE -> "punto-verde";
            case AMBAR -> "punto-ambar";
            case ROJO -> "punto-rojo";
        });
        Label estadoErht = new Label(d.estadoTexto(), puntoErht);
        estadoErht.setGraphicTextGap(7);
        estadoErht.getStyleClass().addAll("etiqueta-opcion", "etiqueta-opcion-blanco");
        StackPane.setAlignment(estadoErht, Pos.BOTTOM_LEFT);
        StackPane.setMargin(estadoErht, new Insets(14));
        fotoErht.getChildren().add(estadoErht);

        Label metaErht = new Label(UtilidadesInterfaz.espaciado(t.danza()) + "  •  " + UtilidadesInterfaz.espaciado(t.color() == null ? "—" : t.color()));
        metaErht.getStyleClass().add("traje-datos");
        metaErht.setMinWidth(0);
        long n = t.vecesAlquilado();
        Label vecesErht = new Label("Alquilado " + n + (n == 1 ? " vez" : " veces"));
        vecesErht.getStyleClass().add("traje-veces");
        vecesErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaMetaErht = new HBox(8, metaErht, UtilidadesInterfaz.espacio(), vecesErht);
        lineaMetaErht.setAlignment(Pos.CENTER_LEFT);

        Label nombreErht = new Label(t.personaje());
        nombreErht.getStyleClass().add("traje-nombre");
        Label codigoErht = new Label(t.codigo());
        codigoErht.getStyleClass().add("traje-codigo");
        codigoErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaNombreErht = new HBox(8, nombreErht, UtilidadesInterfaz.espacio(), codigoErht);
        lineaNombreErht.setAlignment(Pos.BASELINE_LEFT);

        Label descErht = new Label(descripcionErht(t));
        descErht.getStyleClass().add("traje-desc");
        descErht.setWrapText(true);
        descErht.setMinHeight(40);
        descErht.setPrefHeight(40);
        descErht.setMaxHeight(40);

        Region sep1Erht = separadorErht();
        String incluyeErht = d.piezas().isEmpty() ? "Sin piezas registradas" : String.join(", ", d.piezas());
        Label lIncluyeErht = detalleErht(Feather.PACKAGE, "Incluye: ", incluyeErht);
        lIncluyeErht.setTooltip(new Tooltip(incluyeErht));
        VBox detallesErht = new VBox(7,
                lIncluyeErht,
                detalleErht(Feather.MAXIMIZE_2, "Tallas: ", d.tallas()),
                detalleErht(Feather.SHIELD, "Garantía: ", UtilidadesInterfaz.soles(t.garantia()) + " (se devuelve sin daños)"));

        Region sep2Erht = separadorErht();
        Label precioErht = new Label(UtilidadesInterfaz.soles(t.precioAlquiler()));
        precioErht.getStyleClass().add("traje-precio");
        precioErht.setMinWidth(Region.USE_PREF_SIZE);
        Label sufijoErht = new Label(d.sufijoPrecio());
        sufijoErht.getStyleClass().add("traje-precio-nota");
        sufijoErht.setMinWidth(Region.USE_PREF_SIZE);
        HBox lineaPrecioErht = new HBox(6, precioErht, sufijoErht);
        lineaPrecioErht.setAlignment(Pos.BASELINE_LEFT);

        Button reservarErht = UtilidadesInterfaz.estilo(new Button(d.textoBoton(), UtilidadesInterfaz.icono(Feather.ARROW_RIGHT)), "boton-guindo");
        reservarErht.setContentDisplay(ContentDisplay.RIGHT);
        reservarErht.setMinWidth(Region.USE_PREF_SIZE);
        reservarErht.setDisable(!d.reservable());
        reservarErht.setOnAction(e -> d.alReservar().run());
        HBox pieErht = new HBox(10, lineaPrecioErht, UtilidadesInterfaz.espacio(), reservarErht);
        pieErht.setAlignment(Pos.CENTER_LEFT);

        VBox cuerpoErht = new VBox(lineaMetaErht, lineaNombreErht, descErht, sep1Erht, detallesErht, sep2Erht, pieErht);
        cuerpoErht.getStyleClass().add("traje-cuerpo");
        VBox.setMargin(sep1Erht, new Insets(4, 0, 4, 0));
        VBox.setMargin(sep2Erht, new Insets(4, 0, 2, 0));

        VBox tarjetaErht = new VBox(fotoErht, cuerpoErht);
        tarjetaErht.getStyleClass().add("traje-tarjeta");
        tarjetaErht.setMaxWidth(Double.MAX_VALUE);
        tarjetaErht.setMinWidth(0);

        TranslateTransition subeErht = new TranslateTransition(Duration.millis(150), tarjetaErht);
        tarjetaErht.hoverProperty().addListener((o, antes, encima) -> {
            subeErht.stop();
            subeErht.setToY(encima ? -4 : 0);
            subeErht.play();
            if (encima) {
                tarjetaErht.getStyleClass().add("elevada");
            } else {
                tarjetaErht.getStyleClass().remove("elevada");
            }
        });
        if (d.alDobleClic() != null) {
            if (d.abrirConUnClic()) {
                tarjetaErht.setStyle("-fx-cursor: hand;");
            }
            tarjetaErht.setOnMouseClicked(e -> {
                if (e.getClickCount() == (d.abrirConUnClic() ? 1 : 2)) {
                    d.alDobleClic().run();
                }
            });
        }
        return tarjetaErht;
    }

    private static StackPane fotoErht(TrajeFila t) {
        StackPane cajaErht = new StackPane();
        cajaErht.setMinHeight(ALTO_FOTO);
        cajaErht.setPrefHeight(ALTO_FOTO);
        cajaErht.setMaxHeight(ALTO_FOTO);

        URL rutaErht = rutaFotoErht(t.idTraje());
        if (rutaErht != null) {
            cajaErht.setStyle("-fx-background-image: url('" + rutaErht.toExternalForm() + "');"
                    + "-fx-background-size: cover; -fx-background-position: center;");
        } else {
            String colorErht = Adornos.colorDanza(t.danza());
            cajaErht.setStyle("-fx-background-color: linear-gradient(to bottom right, derive(" + colorErht + ", 18%), "
                    + "derive(" + colorErht + ", -42%));");
            Group chakanaErht = Adornos.chakanaEnGrupo(230, "chakana-tenue");
            StackPane.setAlignment(chakanaErht, Pos.CENTER_RIGHT);
            StackPane.setMargin(chakanaErht, new Insets(0, -70, 0, 0));
            Label danzaErht = new Label(t.danza());
            danzaErht.getStyleClass().add("marcador-danza");
            danzaErht.setWrapText(true);
            Label subErht = new Label(UtilidadesInterfaz.espaciado(t.personaje()));
            subErht.getStyleClass().add("marcador-sub");
            VBox textosErht = new VBox(4, danzaErht, subErht);
            textosErht.setAlignment(Pos.CENTER);
            cajaErht.getChildren().addAll(chakanaErht, textosErht);
        }

        Rectangle redondoErht = new Rectangle();
        redondoErht.setArcWidth(36);
        redondoErht.setArcHeight(36);
        redondoErht.widthProperty().bind(cajaErht.widthProperty());
        redondoErht.setHeight(ALTO_FOTO);
        Rectangle rectoErht = new Rectangle(0, ALTO_FOTO - 20, 0, 20);
        rectoErht.widthProperty().bind(cajaErht.widthProperty());
        cajaErht.setClip(new Group(redondoErht, rectoErht));
        return cajaErht;
    }

    private static URL rutaFotoErht(int idTraje) {
        for (String extensionErht : new String[]{"png", "jpg"}) {
            URL u = TarjetaTraje.class.getResource("/img/trajes/" + idTraje + "." + extensionErht);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    private static Region separadorErht() {
        Region r = new Region();
        r.getStyleClass().add("traje-separador");
        return r;
    }

    private static Label detalleErht(Feather ikon, String titulo, String valor) {
        Label l = new Label(titulo + valor, UtilidadesInterfaz.icono(ikon));
        l.setGraphicTextGap(8);
        l.getStyleClass().add("traje-detalle");
        l.setMinWidth(0);
        return l;
    }

    private static String descripcionErht(TrajeFila t) {
        String generoErht = switch (t.genero() == null ? "" : t.genero()) {
            case "VARON" -> "para varón";
            case "MUJER" -> "para dama";
            default -> "unisex";
        };
        String colorErht = t.color() == null ? "" : " en " + t.color().toLowerCase(Locale.ROOT);
        return "Traje completo de " + t.danza().toLowerCase(Locale.ROOT) + " " + generoErht + colorErht
                + ", listo para la festividad. Se entrega revisado pieza por pieza.";
    }

    public static String tallas(TrajeFila t, List<TrajeFila> todos) {
        List<String> otrasErht = new ArrayList<>();
        for (String tallaErht : UtilidadesInterfaz.TALLAS) {
            if (tallaErht.equals(t.talla())) {
                continue;
            }
            boolean hayErht = todos.stream().anyMatch(o -> tallaErht.equals(o.talla())
                    && o.danza().equals(t.danza()) && o.personaje().equals(t.personaje())
                    && java.util.Objects.equals(o.color(), t.color()) && o.genero().equals(t.genero()));
            if (hayErht) {
                otrasErht.add(tallaErht);
            }
        }
        return otrasErht.isEmpty() ? t.talla() : t.talla() + " · también " + String.join(", ", otrasErht);
    }

    public static Map<Integer, LocalDate> proximaFechaLibre(List<Ocupacion> ocupaciones, LocalDate hoy) {
        Map<Integer, List<Ocupacion>> porTrajeErht = new HashMap<>();
        for (Ocupacion o : ocupaciones) {
            porTrajeErht.computeIfAbsent(o.idTraje(), k -> new ArrayList<>()).add(o);
        }
        Map<Integer, LocalDate> libreErht = new HashMap<>();
        porTrajeErht.forEach((id, lista) -> {
            LocalDate d = hoy;
            boolean cambioErht = true;
            while (cambioErht) {
                cambioErht = false;
                for (Ocupacion o : lista) {
                    LocalDate finErht = o.devolucionPrevista();
                    if ("ENTREGADO".equals(o.estado()) && finErht.isBefore(hoy)) {
                        finErht = hoy;
                    }
                    if (!o.salida().isAfter(d) && !finErht.isBefore(d)) {
                        d = finErht.plusDays(1);
                        cambioErht = true;
                    }
                }
            }
            libreErht.put(id, d);
        });
        return libreErht;
    }

    public static final class Cuadricula extends GridPane {

        private static final double ANCHO_MINIMO_3 = 1100;
        private List<Node> tarjetasErht = List.of();
        private int columnasErht;

        public Cuadricula() {
            setHgap(26);
            setVgap(26);
            widthProperty().addListener((o, a, b) -> {
                int c = b.doubleValue() >= ANCHO_MINIMO_3 ? 3 : 2;
                if (c != columnasErht) {
                    ordenarErht(c);
                }
            });
        }

        public void mostrar(List<Node> nuevas) {
            tarjetasErht = nuevas;
            ordenarErht(getWidth() == 0 ? 3 : (getWidth() >= ANCHO_MINIMO_3 ? 3 : 2));
        }

        private void ordenarErht(int c) {
            columnasErht = c;
            getChildren().clear();
            getColumnConstraints().clear();
            for (int i = 0; i < c; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setMinWidth(0);
                cc.setPrefWidth(100);
                cc.setHgrow(Priority.ALWAYS);
                cc.setFillWidth(true);
                getColumnConstraints().add(cc);
            }
            for (int i = 0; i < tarjetasErht.size(); i++) {
                Node n = tarjetasErht.get(i);
                GridPane.setFillWidth(n, true);
                add(n, i % c, i / c);
            }
        }
    }
}
