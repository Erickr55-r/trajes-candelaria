package com.trajes;

import com.trajes.fx.AppFx;
import com.trajes.fx.PanelVendedorView;
import com.trajes.fx.PortalClienteView;
import com.trajes.fx.Servicios;
import com.trajes.fx.Sesion;
import com.trajes.fx.vistas.AlquilerDetalleDialogo;
import com.trajes.fx.vistas.PedidoDisenoDialogo;
import com.trajes.modelo.AlquilerResumen;
import com.trajes.modelo.Opcion;
import com.trajes.modelo.PedidoResumen;
import com.trajes.modelo.PiezaAlquiler;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

/**
 * Genera las capturas del manual en capturas/ (1440×900) recorriendo la interfaz actual: portal del cliente
 * (sesión «cliente») y panel de la tienda (sesión «admin»). Los diálogos se capturan encima de la pantalla.
 * <p>
 * Escribe datos (crea diseños, registra una devolución…), así que SOLO se ejecuta contra una base de
 * demostración cuyo nombre contenga «demo», cargada con base_datos.sql + personalizador.sql + datos_demo.sql:
 * <pre>
 *   (con DB_PASSWORD definida) GenerarCapturas --spring.datasource.url=jdbc:postgresql://localhost:5432/trajes_demo
 * </pre>
 * Está en src/test/java, así que no entra en el jar de mvn package.
 */
public class GenerarCapturas {

    private static final int ANCHO = 1440;
    private static final int ALTO = 900;
    private static final File CARPETA = new File(System.getProperty("user.dir"), "capturas");

    public static void main(String[] args) {
        String url = String.join(" ", args) + " " + System.getenv().getOrDefault("DB_URL", "");
        if (!url.contains("demo")) {
            System.err.println("[capturas] Por seguridad solo se ejecuta contra una base de demostración: pasa "
                    + "--spring.datasource.url=jdbc:postgresql://localhost:5432/trajes_demo (ver datos_demo.sql).");
            return;
        }
        Application.launch(App.class, args);
    }

    /** La misma AppFx, con la escena a 1440×900 y un hilo que la recorre. */
    public static class App extends AppFx {

        @Override
        public void start(Stage stage) {
            super.start(stage);
            if (leer(this, "servicios") == null) {
                return; // no conectó a PostgreSQL: AppFx ya mostró el error
            }
            stage.setX(0);
            stage.setY(0);
            Scene escena = stage.getScene();
            stage.setWidth(ANCHO + (stage.getWidth() - escena.getWidth()));
            stage.setHeight(ALTO + (stage.getHeight() - escena.getHeight()));
            Thread hilo = new Thread(() -> {
                try {
                    new Recorrido(this, stage).ejecutar();
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

    // =====================================================================================================

    private static final class Recorrido {

        private final App app;
        private final Stage stage;
        private final Servicios s;
        private final List<String> generadas = new ArrayList<>();

        Recorrido(App app, Stage stage) {
            this.app = app;
            this.stage = stage;
            this.s = (Servicios) leer(app, "servicios");
        }

        void ejecutar() throws Exception {
            CARPETA.mkdirs();
            pausa(1500);
            portalCliente();
            panelVendedor();
            System.out.println("[capturas] Generadas en " + CARPETA.getAbsolutePath() + ":");
            generadas.forEach(n -> System.out.println("   " + n));
        }

        // ------------------------------------------------------------------ portal del cliente

        private void portalCliente() throws Exception {
            fx(() -> app.ingresar(s.usuarios().autenticar("cliente", "cliente123")));
            pausa(2500);
            PortalClienteView portal = (PortalClienteView) stage.getScene().getRoot();

            // Dos diseños enviados desde «Diseña tu traje» (con su imagen): uno queda PENDIENTE y otro se cotiza.
            int pendiente = enviarDiseno(portal, 1, "ALQUILER", true);
            int cotizado = enviarDiseno(portal, 4, "CONFECCION", false);
            s.personalizador().cotizar(cotizado, new BigDecimal("185.00"));
            System.out.println("[capturas] diseños: " + pendiente + " PENDIENTE, " + cotizado + " COTIZADO");

            // 21 · catálogo con el buscador lleno (Caporales, 01/02 al 04/02/2027)
            fx(() -> portal.ir(PortalClienteView.INICIO));
            pausa(2500);
            fx(() -> {
                Object inicio = centro(portal).getContent();
                ComboBox<Opcion> danza = campo(inicio, "danza");
                danza.getItems().stream().filter(o -> o != null && o.nombre().equals("Caporales")).findFirst()
                        .ifPresent(danza::setValue);
                ((DatePicker) campo(inicio, "recojo")).setValue(LocalDate.of(2027, 2, 1));
                ((DatePicker) campo(inicio, "devolucion")).setValue(LocalDate.of(2027, 2, 4));
                invocar(inicio, "buscar");
            });
            pausa(2500);
            fx(() -> desplazarHasta(centro(portal), ".buscador", 24));
            pausa(800);
            guardar("21_portal_catalogo.png");

            // 25 · mis reservas (SOLICITADO, RESERVADO y RECHAZADO)
            fx(() -> portal.ir(PortalClienteView.MIS_RESERVAS));
            pausa(1800);
            fx(() -> desplazarHasta(centro(portal), ".reserva-tarjeta", 18));
            pausa(700);
            guardar("25_portal_mis_reservas.png");

            // 26 · mis diseños (PENDIENTE y COTIZADO con Aprobar / Rechazar)
            fx(() -> portal.ir(PortalClienteView.MIS_DISENOS));
            pausa(1800);
            guardar("26_portal_mis_disenos.png");

            // 27 · perfil
            fx(() -> portal.ir(PortalClienteView.PERFIL));
            pausa(1500);
            guardar("27_portal_perfil.png");
        }

        /** Envía un diseño desde el personalizador del portal: color de la paleta, modalidad e implemento. */
        private int enviarDiseno(PortalClienteView portal, int indiceColor, String modalidad, boolean cascabeles)
                throws Exception {
            fx(() -> portal.ir(PortalClienteView.DISENIO));
            pausa(3000);
            Node vista = centro(portal).getContent();
            fx(() -> {
                stage.getScene().getRoot().applyCss();
                stage.getScene().getRoot().layout();
                System.out.println("[capturas] personalizador: " + vista.getClass().getSimpleName() + ", "
                        + vista.lookupAll(".muestra").size() + " colores");
            });
            fx(() -> clic(new ArrayList<>(vista.lookupAll(".muestra")).get(indiceColor)));
            pausa(400);
            fx(() -> {
                // entrega antes de la Candelaria 2027
                ((DatePicker) campo(vista, "entrega")).setValue(LocalDate.of(2027, 1, cascabeles ? 22 : 26));
                for (Node n : vista.lookupAll(".modalidad")) {
                    if (modalidad.equals(n.getUserData())) {
                        ((ToggleButton) n).setSelected(true);
                    }
                }
                List<Node> checks = new ArrayList<>(vista.lookupAll(".check-box"));
                CheckBox cb = (CheckBox) checks.get(cascabeles ? 1 : 0);
                cb.setSelected(true);
                cb.fireEvent(new javafx.event.ActionEvent());
            });
            pausa(600);
            fx(() -> boton(vista, "Enviar mi dise").fire());
            pausa(2500);
            fx(() -> boton(portal, "Diseñar otro").fire());
            pausa(500);
            return s.personalizador().pedidosDelCliente(Sesion.get().getCliente()).get(0).idPedido();
        }

        // ------------------------------------------------------------------ panel de la tienda

        private void panelVendedor() throws Exception {
            fx(app::mostrarLogin);
            fx(() -> app.ingresar(s.usuarios().autenticar("admin", "admin123")));
            pausa(2500);
            PanelVendedorView panel = (PanelVendedorView) stage.getScene().getRoot();

            // 34 · alquileres con estados variados (ordenados por cliente para que se mezclen)
            fx(() -> panel.mostrar(PanelVendedorView.ALQUILERES));
            pausa(1800);
            fx(() -> {
                TableView<?> tabla = campo(areaPanel(panel).getContent(), "tabla");
                for (TableColumn<?, ?> c : tabla.getColumns()) {
                    if ("CLIENTE".equals(c.getText())) {
                        ordenar(tabla, c);
                    }
                }
            });
            pausa(800);
            guardar("34_panel_alquileres.png");

            // 35 · registrar un pago (adelanto) de un alquiler RESERVADO de un conjunto
            AlquilerResumen reservado = s.alquileres().listar("RESERVADO", null).stream()
                    .filter(a -> a.saldo().signum() > 0 && !"PARTICULAR".equals(a.conjunto()))
                    .findFirst().orElseThrow();
            Platform.runLater(() -> new AlquilerDetalleDialogo(s, reservado.idAlquiler()).mostrar());
            pausa(2200);
            Stage detalle = ventana(w -> titulo(w).startsWith("Alquiler N° " + reservado.idAlquiler()));
            BigDecimal pago = reservado.saldo().divide(BigDecimal.valueOf(2), 0, RoundingMode.DOWN);
            fx(() -> {
                Parent r = detalle.getScene().getRoot();
                for (Node n : r.lookupAll(".text-field")) {
                    if (n instanceof TextField t && "Monto S/".equals(t.getPromptText())) {
                        t.setText(pago.toPlainString() + ".00");
                    }
                }
                for (Node n : r.lookupAll(".combo-box")) {
                    ComboBox<?> c = (ComboBox<?>) n;
                    if (c.getItems().contains("YAPE")) {
                        seleccionar(c, "YAPE");
                    }
                }
            });
            pausa(700);
            guardarConDialogos("35_panel_pago.png", detalle);

            // 36 · confirmación de entrega del mismo alquiler RESERVADO
            Platform.runLater(() -> boton(detalle.getScene().getRoot(), "Registrar entrega").fire());
            pausa(1500);
            Window confirmar = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            guardarConDialogos("36_panel_entrega.png", detalle, confirmar);
            fx(() -> pulsar(confirmar, ButtonType.NO));
            pausa(500);
            fx(detalle::close);
            pausa(800);

            // 37 · devolución con dos daños (LEVE y GRAVE) y la liquidación de la garantía
            AlquilerResumen entregado = s.alquileres().listar("ENTREGADO", null).stream()
                    .filter(a -> s.alquileres().piezasDelAlquiler(a.idAlquiler()).size() >= 2)
                    .findFirst().orElseThrow();
            Platform.runLater(() -> new AlquilerDetalleDialogo(s, entregado.idAlquiler()).mostrar());
            pausa(2200);
            Stage devolucion = ventana(w -> titulo(w).startsWith("Alquiler N° " + entregado.idAlquiler()));
            fx(() -> {
                Parent r = devolucion.getScene().getRoot();
                List<ComboBox<Object>> piezas = new ArrayList<>();
                List<ComboBox<Object>> tipos = new ArrayList<>();
                for (Node n : r.lookupAll(".combo-box")) {
                    @SuppressWarnings("unchecked")
                    ComboBox<Object> c = (ComboBox<Object>) n;
                    if (c.getItems().stream().anyMatch(x -> x instanceof PiezaAlquiler)) {
                        piezas.add(c);
                    } else if (c.getItems().contains("GRAVE")) {
                        tipos.add(c);
                    }
                }
                piezas.get(0).setValue(piezas.get(0).getItems().get(1));
                piezas.get(1).setValue(piezas.get(1).getItems().get(2));
                tipos.get(0).setValue("LEVE");
                tipos.get(1).setValue("GRAVE");
                List<TextField> descripciones = new ArrayList<>();
                for (Node n : r.lookupAll(".text-field")) {
                    if (n instanceof TextField t && "Ej.: costura rota".equals(t.getPromptText())) {
                        descripciones.add(t);
                    }
                }
                descripciones.get(0).setText("Costura del puño descosida");
                descripciones.get(1).setText("Bordado de pedrería arrancado");
            });
            pausa(600);
            Platform.runLater(() -> boton(devolucion.getScene().getRoot(), "Registrar devolución").fire());
            pausa(1500);
            Window confirmarDev = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            fx(() -> pulsar(confirmarDev, ButtonType.YES));
            pausa(2000);
            Window info = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            guardarConDialogos("37_panel_devolucion_danios.png", devolucion, info);
            fx(() -> pulsar(info, ButtonType.OK));
            pausa(400);
            fx(devolucion::close);
            pausa(800);

            // 38 · pedidos de diseño (uno COTIZADO)
            fx(() -> panel.mostrar(PanelVendedorView.PEDIDOS));
            pausa(1800);
            guardar("38_panel_pedidos_diseno.png");

            // 39 · detalle del pedido PENDIENTE con su imagen y el diálogo para cotizar
            PedidoResumen pendiente = s.personalizador().listarPedidos("PENDIENTE", null).stream()
                    .filter(p -> s.personalizador().imagenDisenoTienda(p.idPedido()) != null)
                    .findFirst().orElseThrow();
            Platform.runLater(() -> new PedidoDisenoDialogo(s, pendiente.idPedido()).mostrar());
            pausa(2200);
            Stage pedido = ventana(w -> titulo(w).startsWith("Pedido de diseño N° " + pendiente.idPedido()));
            Platform.runLater(() -> boton(pedido.getScene().getRoot(), "Cotizar").fire());
            pausa(1500);
            Window cotizar = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            fx(() -> {
                for (Node n : cotizar.getScene().getRoot().lookupAll(".text-field")) {
                    ((TextField) n).setText("120.00");
                }
            });
            pausa(500);
            guardarConDialogos("39_panel_pedido_cotizar.png", pedido, cotizar);
            fx(() -> pulsar(cotizar, ButtonType.CANCEL));
            pausa(400);
            fx(pedido::close);
            pausa(800);

            // 41 · conjuntos y 43 · usuarios
            fx(() -> panel.mostrar(PanelVendedorView.CONJUNTOS));
            pausa(1500);
            guardar("41_panel_conjuntos.png");
            fx(() -> panel.mostrar(PanelVendedorView.USUARIOS));
            pausa(1500);
            guardar("43_panel_usuarios.png");
        }

        // ------------------------------------------------------------------ capturas

        private void guardar(String archivo) throws Exception {
            guardarConDialogos(archivo);
        }

        /**
         * Captura la ventana principal y, encima, cada diálogo (en orden): se oscurece lo de atrás y el diálogo
         * se dibuja centrado con una sombra, como se ve en pantalla.
         */
        private void guardarConDialogos(String archivo, Window... dialogos) throws Exception {
            BufferedImage[] base = new BufferedImage[1];
            List<BufferedImage> capas = new ArrayList<>();
            fx(() -> {
                base[0] = aBuffered(stage.getScene().snapshot(null));
                for (Window w : dialogos) {
                    capas.add(aBuffered(w.getScene().snapshot(null)));
                }
            });
            BufferedImage img = base[0];
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (BufferedImage d : capas) {
                g.setComposite(AlphaComposite.SrcOver);
                g.setColor(new Color(30, 10, 15, 110));
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
                int x = Math.max(0, (img.getWidth() - d.getWidth()) / 2);
                int y = Math.max(0, (img.getHeight() - d.getHeight()) / 2);
                for (int k = 14; k > 0; k -= 2) {   // sombra suave
                    g.setColor(new Color(0, 0, 0, 10));
                    g.fillRoundRect(x - k, y - k + 8, d.getWidth() + 2 * k, d.getHeight() + 2 * k, 24 + k, 24 + k);
                }
                g.drawImage(d, x, y, null);
            }
            g.dispose();
            ImageIO.write(img, "png", new File(CARPETA, archivo));
            generadas.add(archivo + "  (" + img.getWidth() + "×" + img.getHeight() + ")");
            System.out.println("[capturas] " + archivo);
        }

        // ------------------------------------------------------------------ ayudas de navegación

        private static ScrollPane centro(PortalClienteView portal) {
            return (ScrollPane) ((BorderPane) portal.getChildren().get(0)).getCenter();
        }

        private static ScrollPane areaPanel(PanelVendedorView panel) {
            return (ScrollPane) panel.getCenter();
        }

        /** Desplaza el scroll para que el primer nodo con esa clase quede arriba (con un margen). */
        private static void desplazarHasta(ScrollPane sp, String selector, double margen) {
            Node contenido = sp.getContent();
            Node objetivo = contenido.lookup(selector);
            if (objetivo == null) {
                return;
            }
            double y = contenido.sceneToLocal(objetivo.localToScene(0, 0)).getY() - margen;
            double total = contenido.getBoundsInLocal().getHeight() - sp.getViewportBounds().getHeight();
            sp.setVvalue(total <= 0 ? 0 : Math.min(1, Math.max(0, y / total)));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void ordenar(TableView tabla, TableColumn columna) {
            columna.setSortType(TableColumn.SortType.ASCENDING);
            tabla.getSortOrder().setAll(columna);
            tabla.sort();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void seleccionar(ComboBox c, Object valor) {
            c.setValue(valor);
        }

        private static Stage ventana(Predicate<Window> condicion) throws Exception {
            for (int intento = 0; intento < 40; intento++) {
                Window[] hallada = new Window[1];
                fx(() -> {
                    List<Window> ws = new ArrayList<>(Window.getWindows());
                    for (int i = ws.size() - 1; i >= 0; i--) {
                        if (ws.get(i).isShowing() && condicion.test(ws.get(i))) {
                            hallada[0] = ws.get(i);
                            return;
                        }
                    }
                });
                if (hallada[0] != null) {
                    return (Stage) hallada[0];
                }
                pausa(150);
            }
            throw new IllegalStateException("No apareció la ventana esperada.");
        }

        private static String titulo(Window w) {
            return w instanceof Stage st && st.getTitle() != null ? st.getTitle() : "";
        }

        private static void pulsar(Window w, ButtonType tipo) {
            DialogPane dp = (DialogPane) w.getScene().getRoot();
            Node b = dp.lookupButton(tipo);
            if (b instanceof Button boton) {
                boton.fire();
            }
        }

        private static Button boton(Node raiz, String empieza) {
            for (Node n : ((Parent) raiz).lookupAll(".button")) {
                if (n instanceof Button b && b.getText() != null && b.getText().startsWith(empieza) && b.isVisible()) {
                    return b;
                }
            }
            throw new IllegalStateException("No hay botón «" + empieza + "».");
        }

        private static void clic(Node n) {
            n.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5, MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, true, false, true, null));
        }
    }

    // =====================================================================================================

    private static BufferedImage aBuffered(WritableImage im) {
        int w = (int) im.getWidth();
        int h = (int) im.getHeight();
        int[] px = new int[w * h];
        im.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), px, 0, w);
        BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        b.setRGB(0, 0, w, h, px, 0, w);
        return b;
    }

    private static void pausa(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /** Ejecuta en el hilo de JavaFX y espera a que termine (no usar con acciones que abren diálogos modales). */
    private static void fx(Runnable r) throws InterruptedException {
        CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                l.countDown();
            }
        });
        l.await();
    }

    private static Object leer(Object o, String nombre) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(nombre);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("No existe el campo " + nombre);
    }

    @SuppressWarnings("unchecked")
    private static <T> T campo(Object o, String nombre) {
        return (T) leer(o, nombre);
    }

    private static void invocar(Object o, String metodo) {
        try {
            Method m = o.getClass().getDeclaredMethod(metodo);
            m.setAccessible(true);
            m.invoke(o);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
