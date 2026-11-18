/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.portal;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

/**
 * Capa de confirmación: fondo velado y una tarjeta con un check animado
 * (el círculo aparece con un pequeño rebote y luego se "dibuja" el check).
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class AnimacionConfirmacion extends StackPane {

    /**
     * "Ease-out-back": pasa un poco del tamaño final y vuelve (el rebote del círculo).
     * Interpolator.SPLINE no sirve: sus puntos de control deben estar entre 0 y 1.
     */
    private static final Interpolator REBOTE = new Interpolator() {
        @Override
        protected double curve(double t) {
            double c1 = 1.70158;
            double c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        }
    };

    /** Un botón de la confirmación. */
    public record Opcion(String texto, boolean principal, Runnable accion) {}

    private AnimacionConfirmacion(String titulo, String mensaje, Opcion... opciones) {
        getStyleClass().add("confirmacion-velo");

        Circle circuloErht = new Circle(44);
        circuloErht.getStyleClass().add("visto-circulo");
        Polyline checkErht = new Polyline(-19, 1, -6, 14, 21, -14);
        checkErht.getStyleClass().add("visto-trazo");
        checkErht.setStrokeLineCap(StrokeLineCap.ROUND);
        checkErht.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double largoErht = Math.hypot(13, 13) + Math.hypot(27, 28);
        checkErht.getStrokeDashArray().setAll(largoErht, largoErht);
        checkErht.setStrokeDashOffset(largoErht);
        StackPane iconoErht = new StackPane(circuloErht, checkErht);
        iconoErht.setMinSize(96, 96);
        iconoErht.setMaxSize(96, 96);

        Label t = new Label(titulo);
        t.getStyleClass().add("confirmacion-titulo");
        Label m = new Label(mensaje);
        m.getStyleClass().add("confirmacion-texto");
        m.setWrapText(true);
        m.setMinHeight(Region.USE_PREF_SIZE);

        HBox botonesErht = new HBox(10);
        botonesErht.setAlignment(Pos.CENTER);
        for (Opcion o : opciones) {
            Button b = new Button(o.texto());
            b.getStyleClass().add(o.principal() ? "boton-primario" : "boton-secundario");
            b.setOnAction(e -> {
                cerrarErht();
                o.accion().run();
            });
            botonesErht.getChildren().add(b);
        }

        VBox tarjetaErht = new VBox(16, iconoErht, t, m, botonesErht);
        tarjetaErht.setAlignment(Pos.CENTER);
        tarjetaErht.getStyleClass().add("confirmacion-tarjeta");
        tarjetaErht.setMaxSize(460, Region.USE_PREF_SIZE);
        getChildren().add(tarjetaErht);

        // Animación: velo y tarjeta aparecen, el círculo rebota y el check se dibuja.
        setOpacity(0);
        circuloErht.setScaleX(0);
        circuloErht.setScaleY(0);
        FadeTransition apareceErht = new FadeTransition(Duration.millis(180), this);
        apareceErht.setToValue(1);
        ScaleTransition reboteErht = new ScaleTransition(Duration.millis(420), circuloErht);
        reboteErht.setToX(1);
        reboteErht.setToY(1);
        reboteErht.setInterpolator(REBOTE);
        Timeline dibujaErht = new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(checkErht.strokeDashOffsetProperty(), 0, Interpolator.EASE_OUT)));
        new SequentialTransition(apareceErht, reboteErht, dibujaErht).play();
    }

    /** Muestra la confirmación encima de {@code capa} (un StackPane que cubre la ventana). */
    public static void mostrar(StackPane capa, String titulo, String mensaje, Opcion... opciones) {
        capa.getChildren().removeIf(n -> n instanceof AnimacionConfirmacion);
        capa.getChildren().add(new AnimacionConfirmacion(titulo, mensaje, opciones));
    }

    private void cerrarErht() {
        if (getParent() instanceof StackPane p) {
            p.getChildren().remove(this);
        }
    }

}
