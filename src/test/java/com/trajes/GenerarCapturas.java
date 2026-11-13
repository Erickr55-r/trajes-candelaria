package com.trajes;

import com.trajes.fx.AppFx;
import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.fx.PanelVendedorView;
import com.trajes.fx.vistas.AlquilerDetalleDialogo;
import com.trajes.modelo.ColorTraje;
import com.trajes.modelo.Implemento;
import com.trajes.modelo.Opcion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Genera las capturas de pantalla del manual en la carpeta "capturas/" del proyecto.
 * Arranca igual que {@link Launcher} (Spring + servicios reales) y recorre las pantallas por código.
 * No guarda nada en la base: lo único que escribe (la reserva que dispara el trigger) va dentro de
 * una transacción marcada para deshacerse.
 * <p>
 * Está en src/test/java, así que no entra en el jar de mvn package.
 */
public class GenerarCapturas {

    private static final int ANCHO = 1366;
    private static final int ALTO = 768;
    private static final File CARPETA = new File(System.getProperty("user.dir"), "capturas");

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }

    /** La misma AppFx, con la ventana a 1366x768 y un hilo que la recorre. */
    public static class App extends AppFx {

        @Override
        public void start(Stage stage) {
            super.start(stage);
            if (stage.getScene() == null) {
                return; // no conectó a PostgreSQL: AppFx ya mostró el error
            }
            // Las capturas son del panel de la tienda: se entra como el usuario de prueba admin.
            ingresar(((com.trajes.fx.Servicios) leer(this, "servicios")).usuarios().autenticar("admin", "admin123"));
            Parent raiz = stage.getScene().getRoot();
            stage.getScene().setRoot(new Group());
            Scene escena = new Scene(raiz, ANCHO, ALTO);
            escena.getStylesheets().add(Ui.CSS);
            stage.setScene(escena);
            stage.sizeToScene();
            stage.setX(0);
            stage.setY(0);

            ConfigurableApplicationContext contexto = (ConfigurableApplicationContext) leer(this, "contexto");
            Thread hilo = new Thread(() -> {
                try {
                    new Recorrido(stage, (PanelVendedorView) raiz, contexto).ejecutar();
                } catch (Throwable e) {
                    System.err.println("[capturas] Error: " + e);
                    e.printStackTrace();
                } finally {
                    Platform.exit();
                }
            }, "capturas");
            hilo.setDaemon(true);
            hilo.start();
        }
    }

    /** Pasos para cada captura. */
    private static final class Recorrido {

        private final Stage stage;
        private final PanelVendedorView ventana;
        private final Servicios s;
        private final JdbcTemplate jdbc;
        private final TransactionTemplate tx;

        Recorrido(Stage stage, PanelVendedorView ventana, ConfigurableApplicationContext contexto) {
            this.stage = stage;
            this.ventana = ventana;
            this.s = (Servicios) leer(ventana, "servicios");
            this.jdbc = contexto.getBean(JdbcTemplate.class);
            this.tx = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
        }

        void ejecutar() throws Exception {
            CARPETA.mkdirs();
            pausa(2000);

            // 01 Inicio (es la pantalla con la que abre)
            pantalla(PanelVendedorView.INICIO, "01_inicio.png");
            pantalla(PanelVendedorView.TRAJES, "02_trajes.png");
            nuevaReserva();
            detalleAlquiler();
            pantalla(PanelVendedorView.CLIENTES, "05_clientes.png");
            pantalla(PanelVendedorView.REPORTES, "06_reportes.png");
            disenio();
            errorTrigger();

            System.out.println("[capturas] Listo: " + CARPETA.getAbsolutePath());
        }

        private void pantalla(String nombre, String archivo) throws Exception {
            fx(() -> ventana.mostrar(nombre));
            pausa(1800);
            guardar(stage.getScene(), archivo);
        }

        // ------------------------------------------------------------ 03 Nueva reserva

        private void nuevaReserva() throws Exception {
            Map<String, Object> cj = jdbc.queryForMap("""
                    SELECT c.id_conjunto, c.id_delegado
                    FROM conjunto c JOIN danza d ON d.id_danza = c.id_danza
                    WHERE d.nombre = 'Caporales' ORDER BY c.id_conjunto LIMIT 1
                    """);
            Integer idConjunto = (Integer) cj.get("id_conjunto");
            Integer idCliente = cj.get("id_delegado") != null ? (Integer) cj.get("id_delegado")
                    : jdbc.queryForObject("SELECT MIN(id_cliente) FROM cliente", Integer.class);

            fx(() -> ventana.mostrar(PanelVendedorView.NUEVA_RESERVA));
            pausa(1200);
            fx(() -> {
                Object v = ((ScrollPane) ventana.getCenter()).getContent();
                seleccionar(leer(v, "cliente"), o -> idCliente.equals(((Opcion) o).id()));
                seleccionar(leer(v, "conjunto"), o -> o != null && idConjunto.equals(((Opcion) o).id()));
                ((DatePicker) leer(v, "salida")).setValue(LocalDate.now().plusDays(7));
                ((DatePicker) leer(v, "devolucion")).setValue(LocalDate.now().plusDays(10));
                seleccionar(leer(v, "danza"), o -> o != null && "Caporales".equals(((Opcion) o).nombre()));
                invocar(v, "buscar");
                TableView<?> tabla = (TableView<?>) leer(v, "tabla");
                if (tabla.getItems().size() < 5) {
                    System.err.println("[capturas] Aviso: solo hay " + tabla.getItems().size()
                            + " trajes de Caporales libres en esas fechas.");
                }
                for (Object t : tabla.getItems().subList(0, Math.min(5, tabla.getItems().size()))) {
                    ((BooleanProperty) invocar(v, "marca", t)).set(true);
                }
                tabla.refresh();
                invocar(v, "recalcular");
            });
            pausa(1200);
            guardar(stage.getScene(), "03_nueva_reserva.png");
        }

        // ------------------------------------------------------------ 04 Detalle de un alquiler ENTREGADO

        private void detalleAlquiler() throws Exception {
            Integer id = jdbc.queryForObject("""
                    SELECT a.id_alquiler FROM alquiler a
                    WHERE a.estado = 'ENTREGADO'
                    ORDER BY (SELECT COUNT(*) FROM detalle_alquiler d WHERE d.id_alquiler = a.id_alquiler) DESC,
                             a.id_alquiler
                    LIMIT 1
                    """, Integer.class);
            // mostrar() usa showAndWait: se lanza sin esperar y se busca la ventana.
            Platform.runLater(() -> new AlquilerDetalleDialogo(s, id).mostrar());
            Window w = esperarVentana(x -> x instanceof Stage st && ("Alquiler N° " + id).equals(st.getTitle()));
            pausa(1200);
            // Baja hasta el formulario de devolución.
            fx(() -> desplazarHasta(w.getScene(), "Registrar devolución"));
            pausa(800);
            guardar(w.getScene(), "04_detalle_alquiler.png");
            fx(() -> ((Stage) w).close());
            pausa(500);
        }

        // ------------------------------------------------------------ 07 Pedidos de diseño
        // (el personalizador 3D se retiró; ahora el cliente diseña por capas desde su portal)

        private void disenio() throws Exception {
            pantalla(PanelVendedorView.PEDIDOS, "07_pedidos_de_diseno.png");
        }

        // ------------------------------------------------------------ 09 Alert del trigger

        private void errorTrigger() throws Exception {
            Map<String, Object> r = jdbc.queryForMap("""
                    SELECT a.id_cliente, da.id_traje, a.fecha_salida, a.fecha_devolucion_prevista
                    FROM alquiler a
                    JOIN detalle_alquiler da ON da.id_alquiler = a.id_alquiler
                    JOIN traje t ON t.id_traje = da.id_traje
                    WHERE a.estado = 'RESERVADO' AND t.estado = 'DISPONIBLE'
                    ORDER BY a.id_alquiler, da.id_traje
                    LIMIT 1
                    """);
            int idCliente = (Integer) r.get("id_cliente");
            int idTraje = (Integer) r.get("id_traje");
            LocalDate desde = ((java.sql.Date) r.get("fecha_salida")).toLocalDate();
            LocalDate hasta = ((java.sql.Date) r.get("fecha_devolucion_prevista")).toLocalDate();

            fx(() -> ventana.mostrar(PanelVendedorView.NUEVA_RESERVA));
            pausa(800);
            // Mismo camino que "Registrar reserva": el servicio lanza el error del trigger y Ui lo muestra.
            // Todo va en una transacción que siempre se deshace.
            Platform.runLater(() -> tx.executeWithoutResult(estado -> {
                estado.setRollbackOnly();
                Ui.ejecutar(() -> s.alquileres().crear(idCliente, null, desde, hasta, List.of(idTraje),
                        null, "EFECTIVO", "captura del manual"));
            }));
            Window w = esperarVentana(x -> x.getScene() != null && x.getScene().getRoot() instanceof DialogPane);
            pausa(1000);
            guardar(w.getScene(), "09_error_trigger.png");
            fx(() -> ((Button) ((DialogPane) w.getScene().getRoot()).lookupButton(ButtonType.OK)).fire());
            pausa(800);
        }

        // ------------------------------------------------------------ ayudas

        /** Mueve el ScrollPane para que el panel que contiene el botón con ese texto quede arriba. */
        private static void desplazarHasta(Scene escena, String textoBoton) {
            Node boton = escena.getRoot().lookupAll(".button").stream()
                    .filter(n -> n instanceof Button b && textoBoton.equals(b.getText())).findFirst().orElseThrow();
            Node panel = boton;
            while (panel != null && !panel.getStyleClass().contains("panel")) {
                panel = panel.getParent();
            }
            ScrollPane scroll = null;
            for (Node n = boton; n != null; n = n.getParent()) {
                if (n instanceof ScrollPane sp) {
                    scroll = sp;
                    break;
                }
            }
            if (panel == null || scroll == null) {
                return;
            }
            Node contenido = scroll.getContent();
            double y = contenido.sceneToLocal(panel.localToScene(0, 0)).getY() - 10;
            double sobrante = contenido.getLayoutBounds().getHeight() - scroll.getViewportBounds().getHeight();
            scroll.setVvalue(sobrante <= 0 ? 0 : Math.max(0, Math.min(1, y / sobrante)));
        }

        private Window esperarVentana(Predicate<Window> condicion) throws Exception {
            for (int i = 0; i < 100; i++) {
                Window w = fx(() -> Window.getWindows().stream().filter(Window::isShowing).filter(condicion)
                        .findFirst().orElse(null));
                if (w != null) {
                    return w;
                }
                pausa(100);
            }
            throw new IllegalStateException("No se abrió la ventana esperada.");
        }

        private void guardar(Scene escena, String archivo) throws Exception {
            WritableImage img = fx(() -> escena.snapshot(null));
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();
            int[] px = new int[w * h];
            img.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, w, h, px, 0, w);
            File destino = new File(CARPETA, archivo);
            ImageIO.write(bi, "png", destino);

            Set<Integer> distintos = new HashSet<>();
            for (int i = 0; i < px.length; i += 37) {
                distintos.add(px[i]);
            }
            System.out.println("[capturas] " + archivo + "  " + w + "x" + h + "  colores=" + distintos.size()
                    + (distintos.size() < 20 ? "  ¡POSIBLEMENTE EN BLANCO!" : ""));
        }
    }

    // ------------------------------------------------------------ hilo de JavaFX y reflexión

    private interface Paso {
        void ejecutar() throws Exception;
    }

    private static void fx(Paso paso) throws Exception {
        fx(() -> {
            paso.ejecutar();
            return null;
        });
    }

    private static <T> T fx(Callable<T> accion) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<T> valor = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                valor.set(accion.call());
            } catch (Exception e) {
                error.set(e);
            } finally {
                listo.countDown();
            }
        });
        listo.await();
        if (error.get() != null) {
            throw error.get();
        }
        return valor.get();
    }

    private static void pausa(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    @SuppressWarnings("unchecked")
    private static void seleccionar(Object combo, Predicate<Object> condicion) {
        ComboBox<Object> c = (ComboBox<Object>) combo;
        c.getItems().stream().filter(condicion).findFirst().ifPresent(c::setValue);
    }

    private static Object leer(Object objeto, String nombre) {
        for (Class<?> c = objeto.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(nombre);
                f.setAccessible(true);
                return f.get(objeto);
            } catch (NoSuchFieldException e) {
                // sigue con la clase padre
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("No existe el campo " + nombre + " en " + objeto.getClass());
    }

    private static Object invocar(Object objeto, String nombre, Object... args) throws Exception {
        for (Method m : objeto.getClass().getDeclaredMethods()) {
            if (m.getName().equals(nombre) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(objeto, args);
            }
        }
        throw new IllegalArgumentException("No existe el método " + nombre + " en " + objeto.getClass());
    }
}
