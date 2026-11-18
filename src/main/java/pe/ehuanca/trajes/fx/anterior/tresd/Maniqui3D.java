/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.anterior.tresd;

import pe.ehuanca.trajes.fx.anterior.tresd.GeneradorTexturas.Tipo;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Danzante 3D estilizado, hecho con mallas suaves de {@link GeneradorMallas} y texturas de
 * {@link GeneradorTexturas}. Mide unas 8 cabezas (352 unidades) con los pies en y = 172.
 *
 * <p>Cada combinación de danza y figura (varón o cholita) se arma una sola vez y se guarda: elegir
 * qué partes lleva el traje solo muestra u oculta prendas, y cambiar un color solo regenera la
 * textura de esa parte. Si existe {@code /modelos/<danza>.obj} (o {@code <danza>_cholita.obj}),
 * se usa ese modelo en lugar del generado por código.</p>
 *
 * <p>Coordenadas: Y crece hacia abajo y el frente mira hacia -Z (hacia la cámara).</p>
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class Maniqui3D extends Group {

    /** Implemento que se dibuja sobre el maniquí. */
    public record Adorno(String nombre, String destino, int cantidad) {}

    private enum Forma { CAPORAL, MORENADA, DIABLADA, TINKUS }

    public static final double ALTURA = 352;
    public static final double Y_PIES = 172;
    private static final int SEG = GeneradorMallas.SEGMENTOS;

    private static final Map<String, Color> COLOR_INICIAL = Map.of(
            "chaqueta", Color.web("#C62828"), "pantalon", Color.web("#1C1C1C"), "pollera", Color.web("#F06292"),
            "botas", Color.web("#F5F5F5"), "sombrero", Color.web("#F5F5F5"), "capa", Color.web("#6A1B9A"),
            "mascara", Color.web("#C62828"), "faja", Color.web("#D4AF37"));

    private static final List<Color> COLORES_PLUMA = List.of(Color.web("#C62828"), Color.web("#2E8B3E"),
            Color.web("#F5D90A"), Color.web("#F5F5F5"), Color.web("#4FC3F7"), Color.web("#F57C00"),
            Color.web("#6A1B9A"), Color.web("#F06292"), Color.web("#1E5BFF"));

    // Mallas pequeñas que se repiten muchas veces (una sola malla, muchos MeshView)
    private static TriangleMesh esferita;
    private static TriangleMesh fleco;
    private static TriangleMesh espejo;
    private static TriangleMesh pluma;

    private final Map<String, PhongMaterial> materiales = new HashMap<>();
    private final Map<String, Color> colores = new HashMap<>();

    private final PhongMaterial piel = GeneradorTexturas.material(Tipo.PIEL, Color.web("#A86E4E"));
    private final PhongMaterial malla = GeneradorTexturas.material(Tipo.TELA, Color.web("#CFC4B4"));
    private final PhongMaterial pelo = GeneradorTexturas.material(Tipo.TERCIOPELO, Color.web("#19110F"));
    private final PhongMaterial blancoOjo = GeneradorTexturas.material(Tipo.LACA, Color.web("#F3EFE6"));
    private final PhongMaterial iris = GeneradorTexturas.material(Tipo.LACA, Color.web("#2A1A12"));
    private final PhongMaterial irisDiablo = GeneradorTexturas.material(Tipo.LACA, Color.web("#1E6B34"));
    private final PhongMaterial labios = GeneradorTexturas.material(Tipo.PIEL, Color.web("#8C4B3E"));
    private final PhongMaterial bocaRoja = GeneradorTexturas.material(Tipo.LACA, Color.web("#B71C1C"));
    private final PhongMaterial negro = GeneradorTexturas.material(Tipo.LACA, Color.web("#141010"));
    private final PhongMaterial madera = GeneradorTexturas.material(Tipo.MADERA, Color.web("#8A5A35"));
    private final PhongMaterial oro = GeneradorTexturas.material(Tipo.METAL, Color.web("#D9B24A"));
    private final PhongMaterial plata = GeneradorTexturas.material(Tipo.METAL, Color.web("#E2E3EA"));
    private final PhongMaterial camisa = GeneradorTexturas.material(Tipo.TELA, Color.web("#EFEAE0"));
    private final PhongMaterial encaje = GeneradorTexturas.material(Tipo.ENCAJE, Color.web("#FBFAF6"));
    private final PhongMaterial lana = GeneradorTexturas.material(Tipo.TERCIOPELO, Color.web("#C62828"));
    private final Map<Color, PhongMaterial> materialPluma = new HashMap<>();

    private final Map<String, Figura> figuras = new HashMap<>();
    private Figura figura;
    private final Group adornos = new Group();
    private final Set<String> partes = new HashSet<>();
    private List<Adorno> adornosActuales = List.of();
    private boolean conBordado;
    private boolean conLentejuelas;

    /** Una figura armada (malla y puntos donde se ponen los implementos). */
    private static final class Figura {
        final Group raiz = new Group();
        /** Nodos de cada parte; la clave "!parte" significa "visible solo si NO se incluye la parte". */
        final Map<String, List<Node>> porParte = new HashMap<>();
        Node matracaMadera;
        Point3D manoDerecha;
        Point3D manoIzquierda;
        double xPierna;
        double[][] anillosCascabel = {};
        double ySombrero;
        double yCopa;
        double radioCopa;
        double yCabeza;
        double yCintura;
        double radioCintura;
        double fondoCintura;
        double zPecho;

        void agregar(String parte, Node... nodos) {
            raiz.getChildren().addAll(nodos);
            if (parte != null) {
                porParte.computeIfAbsent(parte, k -> new ArrayList<>()).addAll(List.of(nodos));
            }
        }
    }

    /** Recorrido del brazo derecho (x > 0) y sus radios; el izquierdo es su espejo. */
    private record Brazo(List<Point3D> curva, double[] radios) {
        Point3D mano() {
            Point3D w = curva.get(curva.size() - 1);
            Point3D antes = curva.get(curva.size() - 2);
            return w.add(w.subtract(antes).normalize().multiply(8));
        }

        Brazo espejo() {
            return new Brazo(curva.stream().map(p -> new Point3D(-p.getX(), p.getY(), p.getZ())).toList(), radios);
        }

        Brazo grosor(double extra, double puno) {
            double[] r = radios.clone();
            for (int i = 0; i < r.length; i++) {
                r[i] += extra;
            }
            r[r.length - 1] += puno;
            return new Brazo(curva, r);
        }
    }

    public Maniqui3D() {
        armar(null, List.of());
    }

    /** Nombre sin tildes y en minúsculas ("Pantalón" → "pantalon"), para comparar partes. */
    public static String clave(String nombre) {
        if (nombre == null) {
            return "";
        }
        String sin = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sin.toLowerCase(Locale.ROOT).trim();
    }

    // ------------------------------------------------------------------ colores y texturas

    /** Material de una parte; se crea la primera vez con su textura y un color inicial típico. */
    public PhongMaterial material(String parte) {
        String k = clave(parte);
        PhongMaterial m = materiales.get(k);
        if (m == null) {
            m = new PhongMaterial();
            materiales.put(k, m);
            retexturizar(k);
        }
        return m;
    }

    /** Cambia el color de una parte: solo se regenera la textura de esa parte. */
    public void colorear(String parte, Color color) {
        String k = clave(parte);
        colores.put(k, color);
        material(k);
        retexturizar(k);
    }

    private static Tipo tipoDe(String parte) {
        return switch (parte) {
            case "chaqueta", "capa" -> Tipo.TERCIOPELO;
            case "botas" -> Tipo.CUERO;
            case "mascara" -> Tipo.LACA;
            case "faja" -> Tipo.AGUAYO;
            default -> Tipo.TELA;
        };
    }

    private void retexturizar(String k) {
        PhongMaterial m = materiales.get(k);
        if (m == null) {
            return;
        }
        Color c = colores.getOrDefault(k, COLOR_INICIAL.getOrDefault(k, Color.web("#8A1C2B")));
        boolean bordado = conBordado && (k.equals("chaqueta") || k.equals("pollera"));
        boolean lentejuelas = conLentejuelas && k.equals("chaqueta");
        GeneradorTexturas.aplicar(m, tipoDe(k), c, bordado, lentejuelas);
        if (k.equals("chaqueta")) {
            // La pechera de la diablada siempre va bordada; la mantilla de la cholita es más clara
            GeneradorTexturas.aplicar(derivado("pechera"), Tipo.TERCIOPELO, c, true, lentejuelas);
            GeneradorTexturas.aplicar(derivado("mantilla"), Tipo.TELA, c.interpolate(Color.WHITE, 0.45), false, false);
        }
    }

    private PhongMaterial derivado(String nombre) {
        return materiales.computeIfAbsent(nombre, x -> new PhongMaterial());
    }

    // ------------------------------------------------------------------ armado

    /**
     * Arma el maniquí para una danza con las partes indicadas. Las prendas que no se incluyen
     * no se ven (queda el cuerpo de maniquí). Si se incluye "Pollera", la figura es una cholita.
     */
    public void armar(String danza, Collection<String> nombresPartes) {
        partes.clear();
        nombresPartes.forEach(p -> partes.add(clave(p)));
        boolean cholita = partes.contains("pollera");
        String d = clave(danza);
        Forma forma = d.contains("morenada") ? Forma.MORENADA
                : d.contains("diablada") ? Forma.DIABLADA
                : d.contains("tinku") ? Forma.TINKUS
                : Forma.CAPORAL;
        String archivo = d.isEmpty() ? "" : d.replace(' ', '_') + (cholita ? "_cholita" : "");
        Figura f = figuras.computeIfAbsent(forma + "|" + cholita + "|" + archivo,
                k -> construir(forma, cholita, archivo));
        if (f != figura) {
            figura = f;
            getChildren().setAll(f.raiz, adornos);
        }
        for (Map.Entry<String, List<Node>> e : f.porParte.entrySet()) {
            String k = e.getKey();
            boolean visible = k.startsWith("!") ? !partes.contains(k.substring(1)) : partes.contains(k);
            e.getValue().forEach(n -> n.setVisible(visible));
        }
        mostrarImplementos(adornosActuales);
    }

    private Figura construir(Forma forma, boolean cholita, String archivo) {
        Figura procedural = construirProcedural(forma, cholita);
        if (!archivo.isEmpty()) {
            Map<String, MeshView> grupos = CargadorObj.desdeRecurso("/modelos/" + archivo + ".obj", ALTURA, Y_PIES);
            if (grupos != null) {
                return desdeArchivoObj(grupos, procedural);
            }
        }
        return procedural;
    }

    /** Figura a partir de un modelo .obj: cada grupo se pinta según su nombre (chaqueta, botas...). */
    private Figura desdeArchivoObj(Map<String, MeshView> grupos, Figura referencia) {
        Figura f = new Figura();
        for (Map.Entry<String, MeshView> g : grupos.entrySet()) {
            String n = clave(g.getKey());
            String parte = parteDeGrupo(n);
            MeshView v = g.getValue();
            if (parte != null) {
                v.setMaterial(material(parte));
                f.agregar(parte, v);
            } else if (n.contains("pelo") || n.contains("cabello") || n.contains("hair")) {
                v.setMaterial(pelo);
                f.agregar(null, v);
            } else if (n.contains("ojo") || n.contains("eye")) {
                v.setMaterial(blancoOjo);
                f.agregar(null, v);
            } else if (n.contains("piel") || n.contains("cuerpo") || n.contains("cabeza") || n.contains("mano")
                    || n.contains("cara") || n.contains("skin") || n.contains("body")) {
                v.setMaterial(piel);
                f.agregar(null, v);
            } else {
                v.setMaterial(malla);
                f.agregar(null, v);
            }
        }
        // Los implementos se ubican con las medidas de la figura generada (misma altura y posición)
        f.manoDerecha = referencia.manoDerecha;
        f.manoIzquierda = referencia.manoIzquierda;
        f.xPierna = referencia.xPierna;
        f.anillosCascabel = referencia.anillosCascabel;
        f.ySombrero = referencia.ySombrero;
        f.yCopa = referencia.yCopa;
        f.radioCopa = referencia.radioCopa;
        f.yCabeza = referencia.yCabeza;
        f.yCintura = referencia.yCintura;
        f.radioCintura = referencia.radioCintura;
        f.fondoCintura = referencia.fondoCintura;
        f.zPecho = referencia.zPecho;
        return f;
    }

    private static String parteDeGrupo(String n) {
        if (n.contains("chaqueta") || n.contains("blusa") || n.contains("chaleco") || n.contains("saco")) {
            return "chaqueta";
        }
        if (n.contains("pantalon")) {
            return "pantalon";
        }
        if (n.contains("pollera") || n.contains("falda")) {
            return "pollera";
        }
        if (n.contains("bota") || n.contains("zapato") || n.contains("abarca") || n.contains("polaina")) {
            return "botas";
        }
        if (n.contains("sombrero") || n.contains("montera") || n.contains("bombin")) {
            return "sombrero";
        }
        if (n.contains("capa")) {
            return "capa";
        }
        if (n.contains("mascara")) {
            return "mascara";
        }
        if (n.contains("faja")) {
            return "faja";
        }
        return null;
    }

    private Figura construirProcedural(Forma forma, boolean cholita) {
        Figura f = new Figura();
        Brazo brazo = brazo(forma, cholita);
        cuerpo(f, forma, cholita, brazo);
        cara(f);
        if (cholita) {
            cholita(f, forma, brazo);
        } else {
            switch (forma) {
                case MORENADA -> morenada(f, brazo);
                case DIABLADA -> diablada(f, brazo);
                case TINKUS -> tinkus(f, brazo);
                default -> caporal(f, brazo);
            }
        }
        f.manoDerecha = brazo.mano();
        f.manoIzquierda = brazo.espejo().mano();
        return f;
    }

    private static Brazo brazo(Forma forma, boolean cholita) {
        if (cholita) {
            // Manos en la cintura, sobre la pollera
            return new Brazo(List.of(p3(31, -108, 0), p3(37, -86, 3), p3(43, -66, 6), p3(43, -52, -4), p3(38, -46, -14)),
                    new double[]{8, 7, 5.6, 5.3, 4.2});
        }
        if (forma == Forma.MORENADA) {
            // Brazos abiertos por encima del traje de campana
            return new Brazo(List.of(p3(40, -110, 0), p3(52, -88, 2), p3(63, -66, 0), p3(70, -44, -8), p3(73, -28, -16)),
                    new double[]{9.5, 8.3, 6.6, 6.2, 4.9});
        }
        return new Brazo(List.of(p3(35, -108, 0), p3(39, -82, 1.5), p3(42, -56, 3), p3(44, -33, -3), p3(45, -12, -8)),
                new double[]{9.5, 8.3, 6.6, 6.2, 4.9});
    }

    // ------------------------------------------------------------------ cuerpo

    private void cuerpo(Figura f, Forma forma, boolean mujer, Brazo brazo) {
        // Cabeza ovalada con mentón, cuello y torso de sección elíptica
        f.agregar(null, pieza(cabeza(), piel, 0, -158, 0));
        f.agregar(null, pieza(torno(1, 7, -146, 7.2, -136, 8.5, -126, 10, -122), piel, 0, 0, 0));
        double[] torso = mujer
                ? new double[]{0, -129, 8, -127, 16, -123, 27, -117, 31, -108, 31, -98, 28, -84, 23, -64, 22, -54,
                26, -38, 32, -20, 33, -8, 27, 2, 14, 7, 0, 9}
                : new double[]{0, -129, 9, -127, 18, -123, 30, -117, 35, -108, 35, -96, 32, -80, 27, -62, 26, -52,
                28, -38, 31, -22, 31, -10, 26, 0, 14, 6, 0, 8};
        f.agregar(null, pieza(torno(mujer ? 0.62 : 0.6, torso), malla, 0, 0, 0));
        double xHombro = mujer ? 30 : 34;
        for (int lado : new int[]{-1, 1}) {
            f.agregar(null, pieza(GeneradorMallas.elipsoide(10.5, 11.5, 10, 32), malla, lado * xHombro, -108, 0));
        }

        // Brazos un poco separados y doblados, con las manos
        for (Brazo b : new Brazo[]{brazo, brazo.espejo()}) {
            f.agregar(null, pieza(GeneradorMallas.tubo(b.curva(), b.radios(), 32, true), mujer ? piel : malla, 0, 0, 0));
            Point3D m = b.mano();
            double lado = Math.signum(m.getX());
            f.agregar(null, pieza(GeneradorMallas.elipsoide(3.6, 8.5, 6, 24), piel, m.getX(), m.getY(), m.getZ()),
                    pieza(GeneradorMallas.elipsoide(1.8, 4.4, 1.9, 16), piel, m.getX() - lado * 1.6, m.getY() - 2.5, m.getZ() - 5.2));
        }

        // Piernas con rodilla, pantorrilla y tobillo
        f.xPierna = mujer ? 13.5 : 15;
        PhongMaterial pielPiernas = mujer ? piel : malla;
        PhongMaterial pies = (forma == Forma.TINKUS && !mujer) || mujer ? piel : malla;
        double e = mujer ? 0.92 : 1;
        for (int lado : new int[]{-1, 1}) {
            f.agregar(null, pieza(torno(1, escalarRadios(e, 17, -14, 16.5, 4, 15, 36, 12.5, 70, 11, 84, 12, 100,
                    11.5, 118, 8.5, 140, 6.5, 156, 6.5, 162)), pielPiernas, lado * f.xPierna, 0, 0));
            f.agregar(null, pieza(GeneradorMallas.elipsoide(7 * e, 5, 16.5 * e, 24), pies, lado * (f.xPierna + 0.5), 167, -6));
        }

        // Cabello (debajo del sombrero o la máscara)
        MeshView casco = pieza(peloCasco(mujer ? 0.31 : 0.27, mujer ? 0.68 : 0.6), pelo, 0, -158, 0);
        f.agregar("!mascara", casco);

        f.yCabeza = -180;
        f.ySombrero = -180;
        f.yCopa = -168;
        f.radioCopa = 0;
        f.yCintura = -45;
        f.radioCintura = 30;
        f.fondoCintura = 0.66;
        f.zPecho = -24;
    }

    /** Nariz, orejas, ojos, cejas y boca (se ocultan cuando el traje lleva máscara). */
    private void cara(Figura f) {
        f.agregar("!mascara", pieza(GeneradorMallas.elipsoide(2.6, 5.5, 3.6, 24), piel, 0, -155, -17.2));
        for (int lado : new int[]{-1, 1}) {
            f.agregar("!mascara",
                    pieza(GeneradorMallas.elipsoide(2.4, 6, 4, 20), piel, lado * 14.2, -158, 1.5),
                    pieza(GeneradorMallas.elipsoide(3.2, 1.9, 1.6, 20), blancoOjo, lado * 5.6, -162, -15.6),
                    pieza(GeneradorMallas.elipsoide(1.35, 1.35, 0.8, 16), iris, lado * 5.6, -162, -17),
                    pieza(GeneradorMallas.elipsoide(3.8, 0.9, 1.2, 16), pelo, lado * 5.8, -166.4, -15.7));
        }
        f.agregar("!mascara", pieza(GeneradorMallas.elipsoide(4.6, 1.15, 1.3, 20), labios, 0, -146.6, -14.3));
    }

    // ------------------------------------------------------------------ danzas

    private void caporal(Figura f, Brazo brazo) {
        PhongMaterial chaqueta = material("chaqueta");
        // Chaqueta entallada, abierta abajo como saco
        f.agregar("chaqueta", pieza(torno(0.64, 0, -130.5, 10, -128.5, 20, -124.5, 33, -118.5, 38, -109, 38, -96,
                35, -80, 30, -62, 29, -52, 31, -38, 34, -26, 35.5, -20, 33, -19), chaqueta, 0, 0, 0));
        mangas(f, brazo.grosor(2.6, 1.2), chaqueta);
        hombreras(f, 33, -117.5, 13, chaqueta);
        botones(f, new double[]{-96, -84, -72, -60}, new double[]{-24.9, -23.3, -20.6, -19.3});

        // Pantalón acampanado
        PhongMaterial pantalon = material("pantalon");
        f.agregar("pantalon", pieza(torno(0.63, 27.5, -54, 30, -38, 33, -22, 33.5, -10, 28, 1, 15, 7.5, 0, 9.5), pantalon, 0, 0, 0));
        for (int lado : new int[]{-1, 1}) {
            f.agregar("pantalon", pieza(torno(1, 18.5, -12, 18.2, 4, 16.8, 36, 14.2, 70, 13.2, 84, 14.6, 100,
                    17.5, 114, 19.5, 122, 19.8, 125, 18.5, 125.5), pantalon, lado * f.xPierna, 0, 0));
        }
        botas(f, true, new double[]{14.8, 98, 14.2, 104, 13.2, 118, 11, 136, 9.3, 150, 8.8, 160, 9.5, 166});
        f.anillosCascabel = new double[][]{{134, 12.6}, {145, 11.2}};

        sombreroAlaAncha(f, 0, -197, 11, -197, 14, -194.5, 15, -186, 15.6, -176, 17, -173.4, 30, -172.6,
                37, -171.2, 38, -169.8, 36.5, -169.4, 17, -170.6, 0, -170.6);
        f.agregar("sombrero", pieza(torno(1, 15.9, -179, 16.2, -176.5, 15.9, -174), oro, 0, 0, 0));
        f.ySombrero = -197;
        f.yCopa = -184;
        f.radioCopa = 15;

        faja(f, 31.5, -55, 32.6, -51, 32.6, -41, 31.5, -37);
        f.yCintura = -37;
        f.radioCintura = 32;
        f.fondoCintura = 0.67;
    }

    private void morenada(Figura f, Brazo brazo) {
        PhongMaterial chaqueta = material("chaqueta");
        // Traje ancho y abultado tipo campana, con pliegues suaves abajo
        f.agregar("chaqueta", pieza(GeneradorMallas.tornoOndulado(GeneradorMallas.perfil(0, -131, 10, -129, 22, -125,
                39, -119, 47, -109, 49.5, -96, 52, -78, 56, -54, 62, -28, 69, -2, 74, 18, 76, 27, 74.5, 31, 60, 32.5,
                30, 33, 0, 33), SEG, 0.8, 12, v -> v < 0.35 ? 0 : 0.035 * (v - 0.35) / 0.65, false), chaqueta, 0, 0, 0));
        mangas(f, brazo.grosor(5.5, 3), chaqueta);
        hombreras(f, 42, -116.5, 19, chaqueta);

        PhongMaterial pantalon = material("pantalon");
        for (int lado : new int[]{-1, 1}) {
            f.agregar("pantalon", pieza(torno(1, 18.5, -12, 18.2, 4, 16.8, 36, 14.2, 70, 13.2, 84, 13.8, 100,
                    13, 118, 11.2, 136, 10.6, 142), pantalon, lado * f.xPierna, 0, 0));
        }
        botas(f, true, new double[]{11.8, 132, 11.2, 138, 10, 150, 9.4, 160, 9.8, 166});
        f.anillosCascabel = new double[][]{{146, 11.3}, {155, 10.6}};

        sombreroAlaAncha(f, 0, -201, 12, -201, 15, -198, 16, -186, 16.5, -177, 18, -174.2, 31, -173.2, 36, -171.6,
                36.6, -170.4, 34, -170.2, 18, -171.2, 0, -171.2);
        f.agregar("sombrero", pieza(torno(1, 16.6, -180, 16.9, -177.5, 16.6, -175), oro, 0, 0, 0));
        f.ySombrero = -201;
        f.yCopa = -186;
        f.radioCopa = 16;

        // Máscara de moreno: ojos saltones, nariz y labios gruesos
        PhongMaterial mascara = material("mascara");
        MeshView cara = pieza(cabeza(), mascara, 0, -158.5, -1.5);
        cara.getTransforms().add(new Scale(1.2, 1.15, 1.2));
        f.agregar("mascara", cara,
                pieza(GeneradorMallas.elipsoide(4.2, 6.5, 4.6, 24), mascara, 0, -155, -21.3),
                pieza(GeneradorMallas.elipsoide(9.5, 3.6, 4.5, 24), bocaRoja, 0, -146, -19.2));
        for (int lado : new int[]{-1, 1}) {
            f.agregar("mascara",
                    pieza(GeneradorMallas.elipsoide(5.5, 5, 4.5, 24), blancoOjo, lado * 7, -163, -19.8),
                    pieza(GeneradorMallas.elipsoide(2.4, 2.4, 1.4, 16), iris, lado * 7, -163, -23.9),
                    pieza(GeneradorMallas.elipsoide(6.5, 2, 3, 16), mascara, lado * 7, -169.5, -19.5));
        }

        // Matraca de madera del moreno (se cambia por la dorada si se elige ese implemento)
        Group matraca = matraca(madera, brazo.mano());
        f.matracaMadera = matraca;
        f.agregar(null, matraca);
        f.zPecho = -40;
    }

    private void diablada(Figura f, Brazo brazo) {
        PhongMaterial chaqueta = material("chaqueta");
        double[] ys = {-118.5, -109, -96, -80, -62, -52};
        double[] rs = {33, 38, 38, 35, 30, 29};
        f.agregar("chaqueta", pieza(torno(0.64, 0, -130.5, 10, -128.5, 20, -124.5, 33, -118.5, 38, -109, 38, -96,
                35, -80, 30, -62, 29, -52, 31, -38, 34, -26, 35.5, -20, 33, -19), chaqueta, 0, 0, 0));
        mangas(f, brazo.grosor(2.6, 1.6), chaqueta);
        hombreras(f, 33, -117.5, 12, chaqueta);

        // Pechera bordada sobre el pecho, siguiendo la curva de la chaqueta
        TriangleMesh pechera = GeneradorMallas.superficie(24, 20, (u, v) -> {
            double ancho = 27 - 7 * v - 6 * Math.pow(v, 4);
            double x = (u - 0.5) * 2 * ancho;
            double y = -116 + v * 58;
            double r = interpolar(y, ys, rs);
            double z = -0.64 * Math.sqrt(Math.max(r * r - x * x, 0)) - 1.4;
            return new Point3D(x, y, z);
        }, false, 1, 1.2);
        f.agregar("chaqueta", pieza(pechera, derivado("pechera"), 0, 0, 0));

        // Capa que cae por la espalda con pliegues
        TriangleMesh capa = GeneradorMallas.superficie(40, 30, (u, v) -> {
            double s = u * 2 - 1;
            double x = s * (40 + 18 * v);
            double y = -117 + v * 235;
            double z = 27 + 14 * v - Math.pow(1 - v, 2) * 11 * s * s + 5 * Math.pow(v, 0.8) * Math.sin(s * Math.PI * 4.5);
            return new Point3D(x, y, z);
        }, true, 2, 2.5);
        f.agregar("capa", pieza(capa, material("capa"), 0, 0, 0));

        PhongMaterial pantalon = material("pantalon");
        f.agregar("pantalon", pieza(torno(0.63, 27.5, -54, 30, -38, 33, -22, 33.5, -10, 28, 1, 15, 7.5, 0, 9.5), pantalon, 0, 0, 0));
        for (int lado : new int[]{-1, 1}) {
            f.agregar("pantalon", pieza(torno(1, 18.5, -12, 18.2, 4, 16.8, 36, 14.2, 70, 13.2, 84, 14.4, 100,
                    16, 112, 16.6, 118, 15.6, 118.5), pantalon, lado * f.xPierna, 0, 0));
        }
        botas(f, true, new double[]{13.8, 106, 13.2, 114, 12.6, 124, 10.8, 138, 9.4, 152, 9, 162, 9.6, 166});
        f.anillosCascabel = new double[][]{{136, 12.4}, {147, 11.2}};

        // Máscara de diablo: cuernos curvos, ojos saltones, cejas, colmillos y orejas puntiagudas
        PhongMaterial mascara = material("mascara");
        MeshView cara = pieza(cabeza(), mascara, 0, -160, -1);
        cara.getTransforms().add(new Scale(1.3, 1.2, 1.25));
        f.agregar("mascara", cara,
                pieza(GeneradorMallas.elipsoide(5, 4.4, 4.6, 24), mascara, 0, -157, -23),
                pieza(GeneradorMallas.elipsoide(11, 3.2, 2.5, 24), negro, 0, -146, -20.6));
        for (int lado : new int[]{-1, 1}) {
            f.agregar("mascara",
                    pieza(GeneradorMallas.tubo(List.of(p3(lado * 9, -178, 2), p3(lado * 16, -192, 4), p3(lado * 25, -200, 9),
                            p3(lado * 31, -212, 15), p3(lado * 32, -224, 20)), new double[]{6, 5, 3.8, 2.2, 0.3}, 24, true), oro, 0, 0, 0),
                    pieza(GeneradorMallas.tubo(List.of(p3(lado * 17, -160, 0), p3(lado * 25, -167, 2), p3(lado * 30, -176, 4)),
                            new double[]{4, 2.5, 0.3}, 20, true), mascara, 0, 0, 0),
                    pieza(GeneradorMallas.elipsoide(6.5, 6.5, 5.5, 28), blancoOjo, lado * 8.5, -166, -20.5),
                    pieza(GeneradorMallas.elipsoide(3.1, 3.1, 1.6, 20), irisDiablo, lado * 8.5, -166, -25.6),
                    pieza(GeneradorMallas.elipsoide(1.6, 4.2, 1.6, 12), blancoOjo, lado * 10, -143.5, -20.4));
            MeshView ceja = pieza(GeneradorMallas.elipsoide(8.5, 2.6, 4, 20), mascara, lado * 8.5, -174, -20);
            ceja.getTransforms().add(new Rotate(lado * 15, Rotate.Z_AXIS));
            f.agregar("mascara", ceja);
        }
        for (int k = 0; k < 8; k++) {
            double x = -8.4 + k * 2.4;
            f.agregar("mascara", pieza(GeneradorMallas.elipsoide(1.2, 2.2, 1.2, 12), blancoOjo, x, -147.6, -21.6 + 0.02 * x * x));
        }
        f.zPecho = -27;
    }

    private void tinkus(Figura f, Brazo brazo) {
        // Camisa blanca (mangas siempre visibles) y chaleco encima
        mangas(f, brazo.grosor(2.2, 1.4), camisa, null);
        f.agregar("chaqueta", pieza(torno(0.65, 0, -130.5, 10, -128.5, 19, -124.5, 31, -118.8, 36, -110, 37.2, -98,
                35, -80, 30, -62, 29, -50, 30.5, -38, 31.5, -32, 29.5, -31.5), material("chaqueta"), 0, 0, 0));

        // Pantalón corto hasta la rodilla y polainas
        PhongMaterial pantalon = material("pantalon");
        f.agregar("pantalon", pieza(torno(0.63, 27.5, -54, 30, -38, 33, -22, 33.5, -10, 28, 1, 15, 7.5, 0, 9.5), pantalon, 0, 0, 0));
        PhongMaterial botas = material("botas");
        for (int lado : new int[]{-1, 1}) {
            double x = lado * f.xPierna;
            f.agregar("pantalon", pieza(torno(1, 18.8, -12, 18.4, 4, 17.2, 36, 15.2, 66, 15.6, 84, 16, 92, 14.8, 93.2),
                    pantalon, x, 0, 0));
            f.agregar("botas", pieza(torno(1, 13.4, 90, 13.6, 96, 13.6, 104, 12.8, 118, 10.6, 136, 9.2, 150, 8.6, 156, 8, 158),
                    botas, x, 0, 0));
            // Abarcas: suela y tiras sobre el pie
            f.agregar("botas", pieza(GeneradorMallas.superElipsoide(9, 2.2, 18.5, 0.45, 24), botas, x + lado * 0.5, 170, -6.5));
            for (double z : new double[]{-14, -2}) {
                f.agregar("botas", pieza(GeneradorMallas.tubo(List.of(p3(x - 8, 169, z), p3(x, 161.5, z - 1), p3(x + 8, 169, z)),
                        new double[]{1.3, 1.3, 1.3}, 12, true), botas, 0, 0, 0));
            }
        }
        f.anillosCascabel = new double[][]{{140, 11.6}, {150, 10.4}};

        // Montera con forma de casco y ala levantada
        sombreroAlaAncha(f, 0, -196, 9, -195.3, 14.5, -191.5, 17.5, -184, 18.5, -176, 19, -171.5, 23, -170,
                28.5, -172.5, 30, -176.5, 28.8, -177.2, 25, -173.6, 19.8, -170.4, 17.5, -169.8, 0, -169.8);
        f.ySombrero = -196;
        f.yCopa = -182;
        f.radioCopa = 18;

        faja(f, 30.2, -60, 31.8, -56, 32, -46, 31.8, -38, 30.2, -34);
        f.yCintura = -34;
        f.radioCintura = 31.5;
        f.fondoCintura = 0.68;
    }

    /** Cholita: blusa, mantilla, pollera con vuelo y pliegues, bombín y trenzas. */
    private void cholita(Figura f, Forma forma, Brazo brazo) {
        boolean larga = forma != Forma.CAPORAL;
        PhongMaterial chaqueta = material("chaqueta");
        f.agregar("chaqueta", pieza(torno(0.66, 0, -130.5, 9, -128.5, 17, -124.5, 29.5, -118.5, 33.5, -109, 33.5, -98,
                30.5, -84, 25.5, -64, 24.5, -54, 25.5, -46), chaqueta, 0, 0, 0));
        // Mangas abullonadas en el hombro
        mangas(f, new Brazo(brazo.curva(), new double[]{11.5, 9.8, 7.6, 7, 6.4}), chaqueta);
        f.agregar("chaqueta", pieza(GeneradorMallas.tornoOndulado(GeneradorMallas.perfil(14, -128.5, 25, -124,
                36, -116, 41, -104, 42.5, -90, 42, -82), SEG, 0.72, 14, v -> 0.035 * v, true), derivado("mantilla"), 0, 0, 0));

        PhongMaterial pollera = material("pollera");
        if (larga) {
            f.agregar("pollera", pieza(GeneradorMallas.tornoOndulado(GeneradorMallas.perfil(23, -58, 26, -52, 34, -42,
                    47, -22, 62, 8, 76, 44, 86, 78, 90, 96, 90.5, 100, 87, 100.5), SEG * 2, 0.92, 20,
                    v -> 0.015 + 0.075 * Math.pow(v, 1.5), false), pollera, 0, 0, 0));
            // Enagua de encaje que asoma bajo la pollera
            f.agregar("pollera", pieza(GeneradorMallas.tornoOndulado(GeneradorMallas.perfil(86, 96, 89.5, 103, 90.5, 106, 86, 106.3),
                    SEG * 2, 0.92, 20, v -> 0.06, true), encaje, 0, 0, 0));
        } else {
            f.agregar("pollera", pieza(GeneradorMallas.tornoOndulado(GeneradorMallas.perfil(23, -58, 26, -52, 36, -40,
                    52, -20, 66, 4, 73, 22, 74.5, 27, 71, 27.5), SEG * 2, 0.92, 16,
                    v -> 0.01 + 0.08 * Math.pow(v, 1.5), false), pollera, 0, 0, 0));
        }

        // Botas altas (caporal) o botines (morenada)
        botas(f, false, larga
                ? new double[]{8.6, 144, 8.2, 150, 7.8, 160, 8.3, 166}
                : new double[]{12.5, 86, 12, 92, 11.6, 104, 11.4, 120, 9.6, 138, 8, 152, 7.6, 162, 8.2, 166});
        f.anillosCascabel = larga ? new double[][]{{151, 9.6}, {158, 9.4}} : new double[][]{{132, 11.6}, {143, 10}};

        // Bombín pequeño sobre la cabeza
        sombreroAlaAncha(f, 0, -195.5, 8, -195.5, 11, -193, 12, -187, 12.2, -181, 13.3, -179.6, 17, -178.8,
                18.2, -177.6, 16.5, -177, 12.5, -177.6, 0, -177.6);
        f.agregar("sombrero", pieza(torno(1, 12.4, -183, 12.7, -181, 12.6, -179.8), negro, 0, 0, 0));
        f.ySombrero = -195.5;
        f.yCopa = -186;
        f.radioCopa = 12;

        // Trenzas con tullmas de lana
        for (int lado : new int[]{-1, 1}) {
            f.agregar(null, pieza(GeneradorMallas.tubo(List.of(p3(lado * 7, -150, 15), p3(lado * 11, -128, 19),
                    p3(lado * 12, -104, 19), p3(lado * 11, -80, 17), p3(lado * 10, -66, 15)),
                    new double[]{3.6, 3.4, 3.1, 2.7, 2.2}, 16, true), pelo, 0, 0, 0));
            f.agregar(null, pieza(GeneradorMallas.elipsoide(3.4, 6, 3.4, 16), lana, lado * 10, -60, 15));
        }

        faja(f, 25.5, -58, 27.2, -55, 27.2, -49, 25.8, -46);
        f.yCintura = -46;
        f.radioCintura = 27;
        f.fondoCintura = 0.88;
        f.zPecho = -23;
    }

    // ------------------------------------------------------------------ prendas comunes

    private void mangas(Figura f, Brazo b, PhongMaterial mat) {
        mangas(f, b, mat, "chaqueta");
    }

    private void mangas(Figura f, Brazo b, PhongMaterial mat, String parte) {
        for (Brazo lado : new Brazo[]{b, b.espejo()}) {
            f.agregar(parte, pieza(GeneradorMallas.tubo(lado.curva(), lado.radios(), 32, false), mat, 0, 0, 0));
        }
    }

    private void hombreras(Figura f, double x, double y, double ancho, PhongMaterial mat) {
        for (int lado : new int[]{-1, 1}) {
            MeshView h = pieza(GeneradorMallas.elipsoide(ancho, 4.5, ancho * 0.9, 32), mat, lado * x, y, 0);
            h.getTransforms().add(new Rotate(lado * 14, Rotate.Z_AXIS));
            f.agregar("chaqueta", h);
            // Flecos dorados en el borde de la hombrera
            for (int k = 0; k < 9; k++) {
                double a = Math.toRadians(-70 + k * 17.5);
                double fx = lado * (x + ancho * 0.85 * Math.cos(a) + 1);
                double fz = ancho * 0.8 * Math.sin(a);
                f.agregar("chaqueta", pieza(GeneradorMallas.elipsoide(0.9, 4.5, 0.9, 10), oro, fx, y + 5 + 1.5 * Math.cos(a), fz));
            }
        }
    }

    private void botones(Figura f, double[] ys, double[] zs) {
        for (int k = 0; k < ys.length; k++) {
            f.agregar("chaqueta", pieza(GeneradorMallas.elipsoide(1.9, 1.9, 1.2, 14), oro, 0, ys[k], zs[k] - 0.6));
        }
    }

    private void botas(Figura f, boolean conTaco, double[] perfilCana) {
        PhongMaterial botas = material("botas");
        for (int lado : new int[]{-1, 1}) {
            double x = lado * f.xPierna;
            f.agregar("botas", pieza(torno(1, perfilCana), botas, x, 0, 0));
            double ancho = f.xPierna < 14 ? 8 : 9.6;
            f.agregar("botas", pieza(GeneradorMallas.elipsoide(ancho, 6.4, ancho * 2, 28), botas, x + lado * 0.5, 165.6, -8));
            if (conTaco) {
                f.agregar("botas", pieza(GeneradorMallas.superElipsoide(ancho * 0.62, 4.5, 6, 0.5, 20), botas, x, 168.5, 6.5));
            }
        }
    }

    private void sombreroAlaAncha(Figura f, double... perfil) {
        f.agregar("sombrero", pieza(torno(1, perfil), material("sombrero"), 0, 0, 0));
    }

    private void faja(Figura f, double... perfil) {
        f.agregar("faja", pieza(torno(0.67, perfil), material("faja"), 0, 0, 0));
    }

    // ------------------------------------------------------------------ implementos

    /** Dibuja los implementos elegidos; el bordado y las lentejuelas se ven en la textura de la tela. */
    public void mostrarImplementos(List<Adorno> lista) {
        adornosActuales = List.copyOf(lista);
        adornos.getChildren().clear();

        boolean bordado = false;
        boolean lentejuelas = false;
        boolean matracaDorada = false;
        for (Adorno a : adornosActuales) {
            String n = clave(a.nombre());
            bordado |= n.contains("bordado");
            lentejuelas |= n.contains("lentejuela");
            matracaDorada |= n.contains("matraca");
        }
        if (bordado != conBordado || lentejuelas != conLentejuelas) {
            conBordado = bordado;
            conLentejuelas = lentejuelas;
            retexturizar("chaqueta");
            retexturizar("pollera");
        }
        Figura f = figura;
        if (f.matracaMadera != null) {
            f.matracaMadera.setVisible(!matracaDorada);
        }

        boolean conSombrero = partes.contains("sombrero") && f.radioCopa > 0;
        double ySuperior = conSombrero ? f.ySombrero : (partes.contains("mascara") ? -186 : f.yCabeza);
        for (Adorno a : adornosActuales) {
            String n = clave(a.nombre());
            int cant = Math.max(1, a.cantidad());
            if (n.contains("cascabel")) {
                int porAnillo = Math.max(7, Math.min(16, cant / 4));
                for (double[] anillo : f.anillosCascabel) {
                    for (int lado : new int[]{-1, 1}) {
                        for (int i = 0; i < porAnillo; i++) {
                            double t = 2 * Math.PI * (i + (anillo[0] % 2) * 0.5) / porAnillo;
                            adornos.getChildren().add(pieza(esferita(), oro, lado * f.xPierna + anillo[1] * Math.cos(t),
                                    anillo[0], anillo[1] * Math.sin(t)));
                        }
                    }
                }
            } else if (n.contains("matraca")) {
                adornos.getChildren().add(matraca(oro, f.manoDerecha));
            } else if (n.contains("panuelo")) {
                Point3D m = f.manoIzquierda;
                TriangleMesh tela = GeneradorMallas.superficie(16, 16, (u, v) -> new Point3D((u - 0.5) * 30,
                        v * 42, 3 * Math.sin(u * Math.PI * 2) * v + 2 * Math.sin(v * 3)), true, 1, 1);
                adornos.getChildren().add(pieza(tela, encaje, m.getX(), m.getY() + 2, m.getZ() - 6));
            } else if (n.contains("pluma")) {
                int total = Math.max(3, Math.min(9, cant));
                for (int i = 0; i < total; i++) {
                    double ang = total == 1 ? 0 : -38 + 76.0 * i / (total - 1);
                    MeshView p = pieza(pluma(), materialPluma(COLORES_PLUMA.get(i % COLORES_PLUMA.size())), 0, 0, 0);
                    p.getTransforms().setAll(new Translate(0, ySuperior + 3, 5), new Rotate(ang, Rotate.Z_AXIS),
                            new Rotate(-12, Rotate.X_AXIS));
                    adornos.getChildren().add(p);
                }
            } else if (n.contains("espejo")) {
                int total = Math.max(3, Math.min(12, cant));
                for (int i = 0; i < total; i++) {
                    MeshView e;
                    if (conSombrero) {
                        double ang = total == 1 ? 0 : -70 + 140.0 * i / (total - 1);
                        double r = Math.toRadians(ang);
                        double radio = f.radioCopa + 0.8;
                        e = pieza(espejo(), plata, 0, 0, 0);
                        e.getTransforms().setAll(new Translate(radio * Math.sin(r), f.yCopa, -radio * Math.cos(r)),
                                new Rotate(-ang, Rotate.Y_AXIS), new Rotate(90, Rotate.X_AXIS));
                    } else {
                        double x = -18 + (i % 4) * 12;
                        e = pieza(espejo(), plata, 0, 0, 0);
                        e.getTransforms().setAll(new Translate(x, -100 + (i / 4) * 12, f.zPecho - 2),
                                new Rotate(90, Rotate.X_AXIS));
                    }
                    adornos.getChildren().add(e);
                }
            } else if (n.contains("fleco")) {
                for (int i = 0; i < 24; i++) {
                    double ang = Math.toRadians(-85 + i * 170.0 / 23);
                    double x = f.radioCintura * Math.sin(ang);
                    double z = -f.radioCintura * f.fondoCintura * Math.cos(ang) - 0.6;
                    adornos.getChildren().add(pieza(fleco(), oro, x, f.yCintura, z));
                }
            } else if (!n.contains("bordado") && !n.contains("lentejuela")) {
                adornos.getChildren().add(pieza(esferita(), oro, 0, anclaY(f, a.destino()), f.zPecho - 3));
            }
        }
    }

    /** Altura donde se pone un implemento desconocido según la parte a la que va. */
    private static double anclaY(Figura f, String destino) {
        String d = clave(destino);
        if (d.contains("bota") && f.anillosCascabel.length > 0) {
            return f.anillosCascabel[0][0];
        }
        if (d.contains("sombrero") || d.contains("mascara")) {
            return f.ySombrero;
        }
        if (d.contains("faja") || d.contains("pantalon") || d.contains("pollera")) {
            return f.yCintura;
        }
        return -80;
    }

    /** Matraca: mango de madera y cuerpo redondeado, colgando de la mano. */
    private Group matraca(PhongMaterial cuerpo, Point3D mano) {
        Group g = new Group(
                pieza(GeneradorMallas.tubo(List.of(p3(0, -4, 0), p3(0, 8, 0), p3(0, 16, 0)), new double[]{2.2, 2.2, 2.4}, 16, true), madera, 0, 0, 0),
                pieza(GeneradorMallas.superElipsoide(8, 12, 11, 0.45, 32), cuerpo, 0, 26, -2),
                pieza(GeneradorMallas.elipsoide(9.5, 1.6, 12.5, 24), negro, 0, 16, -2),
                pieza(GeneradorMallas.elipsoide(2.6, 2.6, 2.6, 16), plata, 0, 26, -13.5));
        g.getTransforms().setAll(new Translate(mano.getX(), mano.getY(), mano.getZ()));
        return g;
    }

    private PhongMaterial materialPluma(Color c) {
        return materialPluma.computeIfAbsent(c, x -> {
            PhongMaterial m = new PhongMaterial(Color.WHITE);
            m.setDiffuseMap(GeneradorTexturas.pluma(x));
            m.setSpecularColor(Color.rgb(40, 40, 40));
            return m;
        });
    }

    // ------------------------------------------------------------------ mallas compartidas

    private static TriangleMesh esferita() {
        if (esferita == null) {
            esferita = GeneradorMallas.elipsoide(2.7, 2.7, 2.7, 16);
        }
        return esferita;
    }

    private static TriangleMesh fleco() {
        if (fleco == null) {
            fleco = GeneradorMallas.tubo(List.of(p3(0, 0, 0), p3(0, 10, 0), p3(0, 20, 0)), new double[]{0.9, 0.85, 0.7}, 8, true);
        }
        return fleco;
    }

    private static TriangleMesh espejo() {
        if (espejo == null) {
            espejo = GeneradorMallas.torno(GeneradorMallas.perfil(0, -0.6, 3.6, -0.6, 3.9, 0, 3.6, 0.6, 0, 0.6), 24, 1);
        }
        return espejo;
    }

    private static TriangleMesh pluma() {
        if (pluma == null) {
            pluma = GeneradorMallas.superficie(6, 20, (u, v) -> new Point3D((u - 0.5) * 10, -(1 - v) * 62,
                    9 * Math.pow(1 - v, 2)), true, 1, 1);
        }
        return pluma;
    }

    /** Cabeza ovalada, más angosta hacia el mentón y con la nuca un poco más llena. */
    private static TriangleMesh cabeza() {
        return GeneradorMallas.superficie(SEG, 32, (u, v) -> puntoCabeza(u, v, 1), false, 1, 1);
    }

    /** Casco de cabello: la misma forma de la cabeza, un poco más grande, cubriendo arriba y atrás. */
    private static TriangleMesh peloCasco(double limiteFrente, double limiteAtras) {
        return GeneradorMallas.superficie(SEG, 18, (u, v) -> {
            double t = Math.PI / 2 + 2 * Math.PI * u;
            double limite = (limiteFrente + limiteAtras) / 2 + (limiteAtras - limiteFrente) / 2 * Math.sin(t);
            return puntoCabeza(u, v * limite, 1.055);
        }, false, 1, 1);
    }

    private static Point3D puntoCabeza(double u, double v, double escala) {
        double t = Math.PI / 2 + 2 * Math.PI * u;
        double f = Math.PI * v;
        double k = 1;
        if (v > 0.55) {
            double q = (v - 0.55) / 0.45;
            k = 1 - 0.30 * q * q;
        }
        double x = 14.5 * Math.sin(f) * Math.cos(t) * k;
        double z = 17.5 * Math.sin(f) * Math.sin(t) * k;
        if (z > 0) {
            z *= 1.06;
        }
        if (Math.sin(t) < 0 && v > 0.7) {
            z -= 2.0 * (v - 0.7) / 0.3 * (-Math.sin(t));
        }
        double y = -21.5 * Math.cos(f);
        return new Point3D(x * escala, y * escala, z * escala);
    }

    // ------------------------------------------------------------------ utilidades

    private static MeshView pieza(TriangleMesh m, PhongMaterial mat, double x, double y, double z) {
        MeshView v = new MeshView(m);
        v.setMaterial(mat);
        v.setTranslateX(x);
        v.setTranslateY(y);
        v.setTranslateZ(z);
        return v;
    }

    private static TriangleMesh torno(double fondo, double... radioAltura) {
        List<Point2D> p = GeneradorMallas.perfil(radioAltura);
        return GeneradorMallas.torno(p, SEG, fondo);
    }

    private static double[] escalarRadios(double e, double... radioAltura) {
        double[] r = radioAltura.clone();
        for (int i = 0; i < r.length; i += 2) {
            r[i] *= e;
        }
        return r;
    }

    private static double interpolar(double y, double[] ys, double[] rs) {
        if (y <= ys[0]) {
            return rs[0];
        }
        for (int i = 1; i < ys.length; i++) {
            if (y <= ys[i]) {
                double t = (y - ys[i - 1]) / (ys[i] - ys[i - 1]);
                return rs[i - 1] + (rs[i] - rs[i - 1]) * t;
            }
        }
        return rs[rs.length - 1];
    }

    private static Point3D p3(double x, double y, double z) {
        return new Point3D(x, y, z);
    }
}
