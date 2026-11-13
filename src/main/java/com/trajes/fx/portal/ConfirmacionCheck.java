package com.trajes.fx.portal;

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
 */
public final class ConfirmacionCheck extends StackPane {

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

    private ConfirmacionCheck(String titulo, String mensaje, Opcion... opciones) {
        getStyleClass().add("confirmacion-velo");

        Circle circulo = new Circle(44);
        circulo.getStyleClass().add("check-circulo");
        Polyline check = new Polyline(-19, 1, -6, 14, 21, -14);
        check.getStyleClass().add("check-trazo");
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double largo = Math.hypot(13, 13) + Math.hypot(27, 28);
        check.getStrokeDashArray().setAll(largo, largo);
        check.setStrokeDashOffset(largo);
        StackPane icono = new StackPane(circulo, check);
        icono.setMinSize(96, 96);
        icono.setMaxSize(96, 96);

        Label t = new Label(titulo);
        t.getStyleClass().add("confirmacion-titulo");
        Label m = new Label(mensaje);
        m.getStyleClass().add("confirmacion-texto");
        m.setWrapText(true);
        m.setMinHeight(Region.USE_PREF_SIZE);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        for (Opcion o : opciones) {
            Button b = new Button(o.texto());
            b.getStyleClass().add(o.principal() ? "boton-primario" : "boton-secundario");
            b.setOnAction(e -> {
                cerrar();
                o.accion().run();
            });
            botones.getChildren().add(b);
        }

        VBox tarjeta = new VBox(16, icono, t, m, botones);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.getStyleClass().add("confirmacion-tarjeta");
        tarjeta.setMaxSize(460, Region.USE_PREF_SIZE);
        getChildren().add(tarjeta);

        // Animación: velo y tarjeta aparecen, el círculo rebota y el check se dibuja.
        setOpacity(0);
        circulo.setScaleX(0);
        circulo.setScaleY(0);
        FadeTransition aparece = new FadeTransition(Duration.millis(180), this);
        aparece.setToValue(1);
        ScaleTransition rebote = new ScaleTransition(Duration.millis(420), circulo);
        rebote.setToX(1);
        rebote.setToY(1);
        rebote.setInterpolator(REBOTE);
        Timeline dibuja = new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(check.strokeDashOffsetProperty(), 0, Interpolator.EASE_OUT)));
        new SequentialTransition(aparece, rebote, dibuja).play();
    }

    /** Muestra la confirmación encima de {@code capa} (un StackPane que cubre la ventana). */
    public static void mostrar(StackPane capa, String titulo, String mensaje, Opcion... opciones) {
        capa.getChildren().removeIf(n -> n instanceof ConfirmacionCheck);
        capa.getChildren().add(new ConfirmacionCheck(titulo, mensaje, opciones));
    }

    private void cerrar() {
        if (getParent() instanceof StackPane p) {
            p.getChildren().remove(this);
        }
    }

}
