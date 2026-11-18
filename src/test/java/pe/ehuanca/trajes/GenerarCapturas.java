/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes;

import pe.ehuanca.trajes.fx.AplicacionFx;
import pe.ehuanca.trajes.fx.VistaPanelVendedor;
import pe.ehuanca.trajes.fx.VistaPortalCliente;
import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.AcercaDe;
import pe.ehuanca.trajes.fx.Sesion;
import pe.ehuanca.trajes.fx.vistas.AlquilerDetalleDialogo;
import pe.ehuanca.trajes.fx.vistas.ClienteDetalleDialogo;
import pe.ehuanca.trajes.fx.vistas.PedidoDisenoDialogo;
import pe.ehuanca.trajes.modelo.AlquilerResumen;
import pe.ehuanca.trajes.modelo.Opcion;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import pe.ehuanca.trajes.modelo.PiezaAlquiler;
import pe.ehuanca.trajes.modelo.TrajeDisponible;
import pe.ehuanca.trajes.modelo.TrajeFila;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import java.util.Arrays;
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
 * Con {@code -Dcapturas=22,31,33} solo se escriben esas capturas (el recorrido completo igual se hace).
 * Está en src/test/java, así que no entra en el jar de mvn package.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class GenerarCapturas {

    private static final int ANCHO = 1440;
    private static final int ALTO = 900;
    private static final File CARPETA = new File(System.getProperty("user.dir"), "capturas");
    private static final List<String> SOLO = System.getProperty("capturas", "").isBlank() ? List.of()
            : Arrays.stream(System.getProperty("capturas").split(",")).map(String::trim).toList();

    public static void main(String[] args) {
        String ruta = String.join(" ", args) + " " + System.getenv().getOrDefault("DB_URL", "");
        if (!ruta.contains("demo")) {
            System.err.println("[capturas] Por seguridad solo se ejecuta contra una base de demostración: pasa "
                    + "--spring.datasource.url=jdbc:postgresql://localhost:5432/trajes_demo (ver datos_demo.sql).");
            return;
        }
        Application.launch(AplicacionCapturas.class, args);
    }

    public static class AplicacionCapturas extends AplicacionFx {

        @Override
        public void start(Stage escenario) {
            super.start(escenario);
            if (leer(this, "serviciosErht") == null) {
                return;
            }
            escenario.setX(0);
            escenario.setY(0);
            Scene escena = escenario.getScene();
            escenario.setWidth(ANCHO + (escenario.getWidth() - escena.getWidth()));
            escenario.setHeight(ALTO + (escenario.getHeight() - escena.getHeight()));
            Thread hilo = new Thread(() -> {
                try {
                    new Recorrido(this, escenario).ejecutar();
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

    private static final class Recorrido {

        private final AplicacionCapturas aplicacion;
        private final Stage escenario;
        private final Servicios s;
        private final List<String> generadas = new ArrayList<>();

        Recorrido(AplicacionCapturas aplicacion, Stage escenario) {
            this.aplicacion = aplicacion;
            this.escenario = escenario;
            this.s = (Servicios) leer(aplicacion, "serviciosErht");
        }

        void ejecutar() throws Exception {
            CARPETA.mkdirs();
            pausa(1500);
            guardar("login.png");
            portalCliente();
            panelVendedor();
            verificarVendedor();
            System.out.println("[capturas] Generadas en " + CARPETA.getAbsolutePath() + ":");
            generadas.forEach(n -> System.out.println("   " + n));
        }

        private void portalCliente() throws Exception {
            fx(() -> aplicacion.ingresar(s.usuarios().autenticar("cliente", "cliente123")));
            pausa(2500);
            VistaPortalCliente portal = (VistaPortalCliente) escenario.getScene().getRoot();

            int pendiente = enviarDiseno(portal, 1, "ALQUILER", true);
            int cotizado = enviarDiseno(portal, 4, "CONFECCION", false);
            s.personalizador().cotizar(cotizado, new BigDecimal("185.00"));
            System.out.println("[capturas] diseños: " + pendiente + " PENDIENTE, " + cotizado + " COTIZADO");

            fx(() -> portal.ir(VistaPortalCliente.DISENIO));
            pausa(3000);
            Node disena = centro(portal).getContent();
            fx(() -> {
                escenario.getScene().getRoot().applyCss();
                escenario.getScene().getRoot().layout();
                clic(new ArrayList<>(disena.lookupAll(".muestra")).get(1));
                ((DatePicker) campo(disena, "entregaErht")).setValue(LocalDate.of(2027, 1, 24));
                for (Node n : disena.lookupAll(".modalidad")) {
                    if ("CONFECCION".equals(n.getUserData())) {
                        ((ToggleButton) n).setSelected(true);
                    }
                }
                List<Node> casillas = new ArrayList<>(disena.lookupAll(".check-box"));
                for (Node n : casillas.subList(casillas.size() - 2, casillas.size())) {
                    ((CheckBox) n).setSelected(true);
                    n.fireEvent(new javafx.event.ActionEvent());
                }
            });
            pausa(700);
            fx(() -> ((ScrollPane) disena.lookup(".pasos-scroll")).setVvalue(1));
            pausa(600);
            guardar("22_portal_disena_traje.png");

            fx(() -> portal.ir(VistaPortalCliente.INICIO));
            pausa(2500);
            fx(() -> {
                Object inicio = centro(portal).getContent();
                ComboBox<Opcion> danza = campo(inicio, "danzaErht");
                danza.getItems().stream().filter(o -> o != null && o.nombre().equals("Caporales")).findFirst()
                        .ifPresent(danza::setValue);
                ((DatePicker) campo(inicio, "recojoErht")).setValue(LocalDate.of(2027, 2, 1));
                ((DatePicker) campo(inicio, "devolucionErht")).setValue(LocalDate.of(2027, 2, 4));
                invocar(inicio, "buscarErht");
            });
            pausa(2500);
            fx(() -> desplazarHasta(centro(portal), ".buscador", 24));
            pausa(800);
            guardar("21_portal_catalogo.png");

            fx(() -> portal.ir(VistaPortalCliente.MIS_RESERVAS));
            pausa(1800);
            fx(() -> desplazarHasta(centro(portal), ".reserva-tarjeta", 18));
            pausa(700);
            guardar("25_portal_mis_reservas.png");

            fx(() -> portal.ir(VistaPortalCliente.MIS_DISENOS));
            pausa(1800);
            guardar("26_portal_mis_disenos.png");

            fx(() -> portal.ir(VistaPortalCliente.PERFIL));
            pausa(1500);
            guardar("27_portal_perfil.png");

            TrajeFila traje = s.trajes().listar(null, "DISPONIBLE", null, null).stream()
                    .filter(t -> "Caporales".equals(t.danza())).findFirst().orElseThrow();
            fx(() -> portal.verTraje(traje, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 4)));
            pausa(2500);
            guardar("portal_detalle_traje.png");

            int idCliente = Sesion.actual().idCliente();
            long antes = s.alquileres().listar("SOLICITADO", null).stream()
                    .filter(a -> a.idCliente() == idCliente).count();
            fx(() -> ((Button) escenario.getScene().getRoot().lookup(".boton-solicitar")).fire());
            pausa(2500);
            long despues = s.alquileres().listar("SOLICITADO", null).stream()
                    .filter(a -> a.idCliente() == idCliente).count();
            if (despues != antes + 1) {
                throw new IllegalStateException("La solicitud de reserva no se registró (" + antes + " → " + despues + ")");
            }
            System.out.println("[verificación] reserva solicitada desde el portal: " + antes + " → " + despues);

            Platform.runLater(AcercaDe::mostrar);
            pausa(1500);
            Stage acerca = ventana(w -> titulo(w).equals("Acerca de Trajes Candelaria"));
            guardarConDialogos("acerca_de.png", acerca);
            Platform.runLater(() -> boton(acerca.getScene().getRoot(), "Componentes de terceros").fire());
            pausa(1200);
            Stage terceros = ventana(w -> titulo(w).equals("Componentes de terceros"));
            String texto = ((javafx.scene.control.TextArea) terceros.getScene().getRoot().lookup(".text-area")).getText();
            if (!texto.contains("OpenJFX")) {
                throw new IllegalStateException("La ventana de terceros no muestra TERCEROS.md");
            }
            System.out.println("[verificación] Acerca de y TERCEROS.md: OK");
            fx(terceros::close);
            fx(acerca::close);
            pausa(500);
        }

        private void verificarVendedor() throws Exception {
            fx(aplicacion::mostrarInicioSesion);
            fx(() -> aplicacion.ingresar(s.usuarios().autenticar("vendedor", "venta123")));
            pausa(2000);
            Parent raiz = escenario.getScene().getRoot();
            if (!(raiz instanceof VistaPanelVendedor)) {
                throw new IllegalStateException("El vendedor no entró al panel: " + raiz.getClass().getSimpleName());
            }
            System.out.println("[verificación] login vendedor: panel de la tienda ("
                    + Sesion.actual().usuario().rol() + ")");
        }

        private int enviarDiseno(VistaPortalCliente portal, int indiceColor, String modalidad, boolean cascabeles)
                throws Exception {
            fx(() -> portal.ir(VistaPortalCliente.DISENIO));
            pausa(3000);
            Node vista = centro(portal).getContent();
            fx(() -> {
                escenario.getScene().getRoot().applyCss();
                escenario.getScene().getRoot().layout();
                System.out.println("[capturas] personalizador: " + vista.getClass().getSimpleName() + ", "
                        + vista.lookupAll(".muestra").size() + " colores");
            });
            fx(() -> clic(new ArrayList<>(vista.lookupAll(".muestra")).get(indiceColor)));
            pausa(400);
            fx(() -> {
                ((DatePicker) campo(vista, "entregaErht")).setValue(LocalDate.of(2027, 1, cascabeles ? 22 : 26));
                for (Node n : vista.lookupAll(".modalidad")) {
                    if (modalidad.equals(n.getUserData())) {
                        ((ToggleButton) n).setSelected(true);
                    }
                }
                List<Node> casillas = new ArrayList<>(vista.lookupAll(".check-box"));
                CheckBox cb = (CheckBox) casillas.get(cascabeles ? 1 : 0);
                cb.setSelected(true);
                cb.fireEvent(new javafx.event.ActionEvent());
            });
            pausa(600);
            fx(() -> boton(vista, "Enviar mi dise").fire());
            pausa(2500);
            fx(() -> boton(portal, "Diseñar otro").fire());
            pausa(500);
            return s.personalizador().pedidosDelCliente(Sesion.actual().idCliente()).get(0).idPedido();
        }

        private void panelVendedor() throws Exception {
            fx(aplicacion::mostrarInicioSesion);
            fx(() -> aplicacion.ingresar(s.usuarios().autenticar("admin", "admin123")));
            pausa(2500);
            VistaPanelVendedor panel = (VistaPanelVendedor) escenario.getScene().getRoot();

            fx(() -> panel.mostrar(VistaPanelVendedor.INICIO));
            pausa(2500);
            fx(() -> {
                ScrollPane sp = areaPanel(panel);
                Parent c = (Parent) sp.getContent();
                Node movimientos = c.getChildrenUnmodifiable().get(c.getChildrenUnmodifiable().size() - 1);
                desplazarANodo(sp, movimientos, 20);
            });
            pausa(700);
            guardar("31_panel_inicio.png");

            fx(() -> panel.mostrar(VistaPanelVendedor.SOLICITUDES));
            pausa(1800);
            guardar("panel_solicitudes.png");

            fx(() -> panel.mostrar(VistaPanelVendedor.NUEVA_RESERVA));
            pausa(1800);
            fx(() -> {
                Object v = areaPanel(panel).getContent();
                ComboBox<Opcion> cliente = campo(v, "clienteErht");
                cliente.getItems().stream().filter(o -> o != null && o.nombre().startsWith("Roxana Calla Huanca"))
                        .findFirst().ifPresent(cliente::setValue);
                ((DatePicker) campo(v, "salidaErht")).setValue(LocalDate.of(2027, 2, 1));
                ((DatePicker) campo(v, "devolucionErht")).setValue(LocalDate.of(2027, 2, 4));
                ComboBox<Opcion> danza = campo(v, "danzaErht");
                danza.getItems().stream().filter(o -> o != null && o.nombre().equals("Caporales")).findFirst()
                        .ifPresent(danza::setValue);
                invocar(v, "buscarErht");
                TableView<?> tabla = campo(v, "tablaErht");
                try {
                    Method marca = v.getClass().getDeclaredMethod("marcaErht", TrajeDisponible.class);
                    marca.setAccessible(true);
                    for (Object t : tabla.getItems().subList(0, Math.min(3, tabla.getItems().size()))) {
                        ((javafx.beans.property.BooleanProperty) marca.invoke(v, t)).set(true);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                ((TextField) campo(v, "adelantoErht")).setText("150.00");
                seleccionar(campo(v, "metodoErht"), "YAPE");
            });
            pausa(900);
            guardar("33_panel_nueva_reserva.png");

            fx(() -> panel.mostrar(VistaPanelVendedor.ALQUILERES));
            pausa(1800);
            fx(() -> {
                TableView<?> tabla = campo(areaPanel(panel).getContent(), "tablaErht");
                for (TableColumn<?, ?> c : tabla.getColumns()) {
                    if ("CLIENTE".equals(c.getText())) {
                        ordenar(tabla, c);
                    }
                }
            });
            pausa(800);
            guardar("34_panel_alquileres.png");

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

            Platform.runLater(() -> boton(detalle.getScene().getRoot(), "Registrar entrega").fire());
            pausa(1500);
            Window confirmar = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            guardarConDialogos("36_panel_entrega.png", detalle, confirmar);
            fx(() -> pulsar(confirmar, ButtonType.NO));
            pausa(500);
            fx(detalle::close);
            pausa(800);

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
            Window confirmarDevolucion = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            fx(() -> pulsar(confirmarDevolucion, ButtonType.YES));
            pausa(2000);
            Window info = ventana(w -> w.getScene() != null && w.getScene().getRoot() instanceof DialogPane);
            guardarConDialogos("37_panel_devolucion_danios.png", devolucion, info);
            fx(() -> pulsar(info, ButtonType.OK));
            pausa(400);
            fx(devolucion::close);
            pausa(800);

            fx(() -> panel.mostrar(VistaPanelVendedor.PEDIDOS));
            pausa(1800);
            guardar("38_panel_pedidos_diseno.png");

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

            fx(() -> panel.mostrar(VistaPanelVendedor.CLIENTES));
            pausa(1800);
            int idCliente = s.clientes().listar("44268151").get(0).idCliente();
            Platform.runLater(() -> new ClienteDetalleDialogo(s, idCliente).mostrar());
            pausa(2000);
            Stage detalleCliente = ventana(w -> titulo(w).equals("Detalle del cliente"));
            guardarConDialogos("40_panel_clientes.png", detalleCliente);
            fx(detalleCliente::close);
            pausa(600);

            fx(() -> panel.mostrar(VistaPanelVendedor.TRAJES));
            pausa(1800);
            fx(() -> {
                TableView<?> tabla = campo(areaPanel(panel).getContent(), "tablaErht");
                for (TableColumn<?, ?> c : tabla.getColumns()) {
                    if ("ESTADO".equals(c.getText())) {
                        c.setSortType(TableColumn.SortType.DESCENDING);
                        ordenarSinCambiarSentido(tabla, c);
                    }
                }
            });
            pausa(700);
            guardar("42_panel_trajes.png");

            fx(() -> panel.mostrar(VistaPanelVendedor.REPORTES));
            pausa(1800);
            fx(() -> {
                TabPane pestanias = (TabPane) areaPanel(panel).getContent().lookup(".tab-pane");
                for (Tab t : pestanias.getTabs()) {
                    if ("Ingresos por mes".equals(t.getText())) {
                        pestanias.getSelectionModel().select(t);
                    }
                }
            });
            pausa(900);
            guardar("44_panel_reportes.png");

            fx(() -> panel.mostrar(VistaPanelVendedor.CONJUNTOS));
            pausa(1500);
            guardar("41_panel_conjuntos.png");
            fx(() -> panel.mostrar(VistaPanelVendedor.USUARIOS));
            pausa(1500);
            guardar("43_panel_usuarios.png");
        }

        private void guardar(String archivo) throws Exception {
            guardarConDialogos(archivo);
        }

        private void guardarConDialogos(String archivo, Window... dialogos) throws Exception {
            String clave = archivo.matches("\\d+_.*") ? archivo.substring(0, archivo.indexOf('_'))
                    : archivo.replace(".png", "");
            if (!SOLO.isEmpty() && !SOLO.contains(clave)) {
                System.out.println("[capturas] (omitida) " + archivo);
                return;
            }
            BufferedImage[] base = new BufferedImage[1];
            List<BufferedImage> capas = new ArrayList<>();
            fx(() -> {
                base[0] = aImagenAwt(escenario.getScene().snapshot(null));
                for (Window w : dialogos) {
                    capas.add(aImagenAwt(w.getScene().snapshot(null)));
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
                for (int k = 14; k > 0; k -= 2) {
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

        private static ScrollPane centro(VistaPortalCliente portal) {
            return (ScrollPane) ((BorderPane) portal.getChildren().get(0)).getCenter();
        }

        private static ScrollPane areaPanel(VistaPanelVendedor panel) {
            return (ScrollPane) panel.getCenter();
        }

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

        private static void desplazarANodo(ScrollPane sp, Node nodo, double margen) {
            Node contenido = sp.getContent();
            double y = nodo.getBoundsInParent().getMinY() - margen;
            double total = contenido.getBoundsInLocal().getHeight() - sp.getViewportBounds().getHeight();
            sp.setVvalue(total <= 0 ? 0 : Math.min(1, Math.max(0, y / total)));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void ordenarSinCambiarSentido(TableView tabla, TableColumn columna) {
            tabla.getSortOrder().setAll(columna);
            tabla.sort();
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

    private static BufferedImage aImagenAwt(WritableImage im) {
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
