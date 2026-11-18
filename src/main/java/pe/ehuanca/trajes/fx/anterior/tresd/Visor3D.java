/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.anterior.tresd;

import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Visor 3D tipo estudio fotográfico: SubScene con depthBuffer y antialiasing, cámara en perspectiva
 * a la altura del pecho, iluminación de 3 puntos (principal cálida, relleno frío y contorno detrás)
 * con luz ambiental baja, fondo degradado, tarima de madera y sombra suave bajo los pies.
 * Se gira arrastrando el mouse (con inercia), se acerca con la rueda y tiene vistas de frente, lado y espalda.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class Visor3D extends BorderPane {

    private static final RadialGradient FONDO = new RadialGradient(0, 0, 0.5, 0.42, 0.8, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FCFAF6")), new Stop(0.55, Color.web("#EAE2D7")), new Stop(1, Color.web("#C9BDAE")));
    private static final double DISTANCIA = 920;
    private static final double GIRO_X_INICIAL = -3;
    private static final double GIRO_Y_INICIAL = -24;
    private static final String[] VISTAS = {"frente", "lado", "espalda"};
    private static final double[] ANGULO_VISTA = {0, -90, 180};

    private final Maniqui3D maniqui = new Maniqui3D();
    private final Rotate giroX = new Rotate(GIRO_X_INICIAL, 0, -10, 0, Rotate.X_AXIS);
    private final Rotate giroY = new Rotate(GIRO_Y_INICIAL, Rotate.Y_AXIS);
    private final Group giroAutomatico = new Group(maniqui);
    private final PerspectiveCamera camara = new PerspectiveCamera(true);
    private final SubScene subEscena;
    private final RotateTransition animacion;
    private final ToggleButton girar = new ToggleButton("Girar automáticamente");
    private final Button vista = new Button("Vista: frente");
    private int indiceVista = -1;
    private Timeline transicion;

    // Inercia del giro con el mouse
    private double xAnterior;
    private double yAnterior;
    private double velocidadY;
    private double velocidadX;
    private boolean arrastrando;
    private final AnimationTimer inercia = new AnimationTimer() {
        @Override
        public void handle(long ahora) {
            if (arrastrando) {
                return;
            }
            giroY.setAngle(giroY.getAngle() + velocidadY);
            giroX.setAngle(limitar(giroX.getAngle() + velocidadX, -40, 40));
            velocidadY *= 0.93;
            velocidadX *= 0.88;
            if (Math.abs(velocidadY) < 0.02 && Math.abs(velocidadX) < 0.02) {
                stop();
            }
        }
    };

    public Visor3D(double ancho, double alto) {
        Group modelo = new Group(giroAutomatico);
        modelo.getTransforms().addAll(giroX, giroY);

        // Iluminación de 3 puntos y ambiente bajo
        AmbientLight ambiente = new AmbientLight(Color.rgb(64, 60, 64));
        PointLight principal = luz(Color.rgb(255, 232, 205), -430, -380, -640);
        PointLight relleno = luz(Color.rgb(105, 120, 150), 520, -110, -380);
        PointLight contorno = luz(Color.rgb(255, 250, 240), 260, -330, 560);
        PointLight contorno2 = luz(Color.rgb(150, 150, 165), -300, -150, 520);

        // Cámara a la altura del pecho, mirando un poco hacia abajo al centro de la figura
        camara.setNearClip(1);
        camara.setFieldOfView(30);
        camara.setFarClip(6000);
        camara.setTranslateZ(-DISTANCIA);
        Group brazoCamara = new Group(camara);
        brazoCamara.setTranslateY(-6);
        brazoCamara.getTransforms().add(new Rotate(-4.6, Rotate.X_AXIS));

        Group raiz = new Group(tarima(), modelo, ambiente, principal, relleno, contorno, contorno2, brazoCamara);
        subEscena = new SubScene(raiz, ancho, alto, true, SceneAntialiasing.BALANCED);
        // El fondo degradado lo pinta el panel (así la foto ×2 también lo lleva completo)
        subEscena.setFill(Color.TRANSPARENT);
        subEscena.setCamera(camara);

        Pane lienzo = new Pane(subEscena);
        lienzo.setPrefSize(ancho, alto);
        lienzo.setMinSize(220, 260);
        lienzo.getStyleClass().add("visor-3d");
        lienzo.setBackground(new Background(new BackgroundFill(FONDO, new CornerRadii(8), Insets.EMPTY)));
        subEscena.widthProperty().bind(lienzo.widthProperty());
        subEscena.heightProperty().bind(lienzo.heightProperty());
        setCenter(lienzo);

        subEscena.setOnMousePressed(e -> {
            detenerMovimientos();
            arrastrando = true;
            velocidadX = 0;
            velocidadY = 0;
            xAnterior = e.getSceneX();
            yAnterior = e.getSceneY();
        });
        subEscena.setOnMouseDragged(e -> {
            double dy = -(e.getSceneX() - xAnterior) * 0.45;
            double dx = (e.getSceneY() - yAnterior) * 0.3;
            giroY.setAngle(giroY.getAngle() + dy);
            giroX.setAngle(limitar(giroX.getAngle() + dx, -40, 40));
            velocidadY = dy;
            velocidadX = dx;
            xAnterior = e.getSceneX();
            yAnterior = e.getSceneY();
        });
        subEscena.setOnMouseReleased(e -> {
            arrastrando = false;
            inercia.start();
        });
        subEscena.setOnScroll(e -> camara.setTranslateZ(limitar(camara.getTranslateZ() + e.getDeltaY() * 1.1, -1800, -520)));

        animacion = new RotateTransition(Duration.seconds(10), giroAutomatico);
        animacion.setAxis(Rotate.Y_AXIS);
        animacion.setByAngle(360);
        animacion.setCycleCount(RotateTransition.INDEFINITE);
        animacion.setInterpolator(Interpolator.LINEAR);
        girar.setOnAction(e -> {
            if (girar.isSelected()) {
                inercia.stop();
                animacion.play();
            } else {
                animacion.pause();
            }
        });

        vista.setOnAction(e -> siguienteVista());
        girar.setMinWidth(Region.USE_PREF_SIZE);
        vista.setMinWidth(Region.USE_PREF_SIZE);
        HBox botones = new HBox(8, girar, vista);
        VBox barra = new VBox(4, botones, UtilidadesInterfaz.subtitulo("Arrastra para girar · rueda para acercar"));
        barra.getStyleClass().add("visor-barra");
        setBottom(barra);
    }

    public Maniqui3D maniqui() {
        return maniqui;
    }

    /** Vista de tres cuartos con la que empieza el visor. */
    public void vistaInicial() {
        detenerMovimientos();
        giroAutomatico.setRotate(0);
        giroX.setAngle(GIRO_X_INICIAL);
        giroY.setAngle(GIRO_Y_INICIAL);
        camara.setTranslateZ(-DISTANCIA);
        indiceVista = -1;
        vista.setText("Vista: frente");
    }

    /** Pasa a la siguiente vista (frente → lado → espalda) con una transición suave. */
    private void siguienteVista() {
        detenerMovimientos();
        indiceVista = (indiceVista + 1) % VISTAS.length;
        vista.setText("Vista: " + VISTAS[(indiceVista + 1) % VISTAS.length]);
        // Se toma el camino más corto desde el ángulo actual, sumando el giro automático
        double actual = giroY.getAngle() - giroAutomatico.getRotate();
        giroAutomatico.setRotate(0);
        double destino = ANGULO_VISTA[indiceVista];
        double diferencia = ((destino - actual) % 360 + 540) % 360 - 180;
        giroY.setAngle(actual);
        transicion = new Timeline(new KeyFrame(Duration.millis(550),
                new KeyValue(giroY.angleProperty(), actual + diferencia, Interpolator.EASE_BOTH),
                new KeyValue(giroX.angleProperty(), GIRO_X_INICIAL, Interpolator.EASE_BOTH)));
        transicion.play();
    }

    private void detenerMovimientos() {
        inercia.stop();
        if (transicion != null) {
            transicion.stop();
        }
        if (girar.isSelected()) {
            girar.setSelected(false);
            animacion.pause();
        }
    }

    /** Detiene las animaciones (al cerrar la pantalla). */
    public void detener() {
        animacion.stop();
        inercia.stop();
        if (transicion != null) {
            transicion.stop();
        }
    }

    /** Foto de lo que se ve en el visor, a {@code escala} veces su tamaño en pantalla. */
    public WritableImage captura(double escala) {
        SnapshotParameters p = new SnapshotParameters();
        p.setFill(FONDO);
        p.setTransform(Transform.scale(escala, escala));
        return subEscena.snapshot(p, null);
    }

    /** Guarda la vista actual del maniquí como PNG en alta resolución (×2). */
    public void guardarCaptura(File archivo) {
        WritableImage imagen = captura(2);
        int w = (int) imagen.getWidth();
        int h = (int) imagen.getHeight();
        int[] pixeles = new int[w * h];
        imagen.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixeles, 0, w);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, w, h, pixeles, 0, w);
        try {
            ImageIO.write(bi, "png", archivo);
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo guardar la imagen: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ escena

    /** Tarima circular de madera con borde y una sombra suave bajo los pies. */
    private static Group tarima() {
        PhongMaterial madera = GeneradorTexturas.material(GeneradorTexturas.Tipo.MADERA, Color.web("#6B4730"));
        MeshView canto = new MeshView(GeneradorMallas.torno(GeneradorMallas.perfil(118, 176, 121, 177.2, 122, 180,
                122, 188, 119, 190, 0, 190), GeneradorMallas.SEGMENTOS * 2, 1));
        canto.setMaterial(madera);
        MeshView cara = new MeshView(GeneradorMallas.discoPlano(119, GeneradorMallas.SEGMENTOS * 2));
        cara.setMaterial(madera);
        cara.setTranslateY(176);

        PhongMaterial sombra = new PhongMaterial(Color.WHITE);
        sombra.setDiffuseMap(GeneradorTexturas.sombra());
        sombra.setSpecularColor(Color.BLACK);
        MeshView mancha = new MeshView(GeneradorMallas.discoPlano(70, GeneradorMallas.SEGMENTOS));
        mancha.setMaterial(sombra);
        mancha.setTranslateY(175.4);
        mancha.getTransforms().add(new Scale(1, 1, 0.75));
        return new Group(canto, cara, mancha);
    }

    private static PointLight luz(Color color, double x, double y, double z) {
        PointLight l = new PointLight(color);
        l.setTranslateX(x);
        l.setTranslateY(y);
        l.setTranslateZ(z);
        return l;
    }

    private static double limitar(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
