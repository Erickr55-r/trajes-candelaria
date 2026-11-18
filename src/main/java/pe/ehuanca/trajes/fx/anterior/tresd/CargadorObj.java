/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.anterior.tresd;

import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector simple de archivos Wavefront .obj (v, vt, vn, f, g, o y usemtl).
 * Devuelve un MeshView por grupo, para pintar cada grupo como una parte del traje.
 *
 * <p>El nombre del grupo sale de {@code usemtl} si existe y si no de {@code g} / {@code o}.
 * Las caras de más de 3 vértices se dividen en triángulos (abanico). Si el archivo no trae
 * normales se calculan suavizadas; si no trae UV se proyectan de forma cilíndrica.</p>
 *
 * <p>El OBJ usa Y hacia arriba; JavaFX usa Y hacia abajo. Se gira 180° alrededor del eje X
 * (y → -y, z → -z), que es una rotación y no un espejo, así el sentido de los triángulos se
 * conserva. Después el modelo se escala a {@code altura} y se apoya con los pies en {@code yPies}.</p>
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class CargadorObj {

    private CargadorObj() {
    }

    /** Lee un .obj del classpath (por ejemplo "/modelos/caporales.obj"); null si no existe. */
    public static Map<String, MeshView> desdeRecurso(String ruta, double altura, double yPies) {
        InputStream in = CargadorObj.class.getResourceAsStream(ruta);
        if (in == null) {
            return null;
        }
        try (in) {
            return cargar(in, altura, yPies);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Map<String, MeshView> cargar(InputStream in, double altura, double yPies) throws IOException {
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        Map<String, List<int[]>> grupos = new LinkedHashMap<>();
        String actual = "default";
        String grupoG = "default";

        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String linea;
        while ((linea = r.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            String[] t = linea.split("\\s+");
            switch (t[0]) {
                case "v" -> v.add(new float[]{f(t[1]), -f(t[2]), -f(t[3])});
                case "vt" -> vt.add(new float[]{f(t[1]), 1 - (t.length > 2 ? f(t[2]) : 0)});
                case "vn" -> vn.add(new float[]{f(t[1]), -f(t[2]), -f(t[3])});
                case "g", "o" -> {
                    grupoG = t.length > 1 ? t[1] : "default";
                    actual = grupoG;
                }
                case "usemtl" -> actual = t.length > 1 ? t[1] : grupoG;
                case "f" -> {
                    List<int[]> caras = grupos.computeIfAbsent(actual, k -> new ArrayList<>());
                    int[][] vert = new int[t.length - 1][];
                    for (int k = 1; k < t.length; k++) {
                        vert[k - 1] = indices(t[k], v.size(), vt.size(), vn.size());
                    }
                    for (int k = 1; k + 1 < vert.length; k++) {
                        caras.add(new int[]{vert[0][0], vert[0][1], vert[0][2],
                                vert[k][0], vert[k][1], vert[k][2],
                                vert[k + 1][0], vert[k + 1][1], vert[k + 1][2]});
                    }
                }
                default -> {
                    // mtllib, s y otras líneas no se usan
                }
            }
        }
        if (v.isEmpty()) {
            throw new IOException("El archivo OBJ no tiene vértices.");
        }

        // Escala y posición: el modelo mide 'altura' y se apoya en yPies, centrado en X y Z
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (float[] p : v) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
            minZ = Math.min(minZ, p[2]);
            maxZ = Math.max(maxZ, p[2]);
        }
        double escala = altura / Math.max(maxY - minY, 1e-6);
        double cx = (minX + maxX) / 2;
        double cz = (minZ + maxZ) / 2;

        Map<String, MeshView> salida = new LinkedHashMap<>();
        for (Map.Entry<String, List<int[]>> g : grupos.entrySet()) {
            List<int[]> caras = g.getValue();
            if (caras.isEmpty()) {
                continue;
            }
            int total = caras.size() * 3;
            float[] puntos = new float[total * 3];
            float[] tex = new float[total * 2];
            float[] normales = new float[total * 3];
            List<int[]> triangulos = new ArrayList<>();
            boolean faltanNormales = false;
            int w = 0;
            for (int[] c : caras) {
                for (int k = 0; k < 3; k++) {
                    float[] p = v.get(c[k * 3]);
                    float x = (float) ((p[0] - cx) * escala);
                    float y = (float) (yPies - (maxY - p[1]) * escala);
                    float z = (float) ((p[2] - cz) * escala);
                    puntos[w * 3] = x;
                    puntos[w * 3 + 1] = y;
                    puntos[w * 3 + 2] = z;
                    if (c[k * 3 + 1] >= 0) {
                        tex[w * 2] = vt.get(c[k * 3 + 1])[0];
                        tex[w * 2 + 1] = vt.get(c[k * 3 + 1])[1];
                    } else {
                        // Proyección cilíndrica alrededor del eje vertical
                        tex[w * 2] = (float) ((Math.atan2(z, x) / (2 * Math.PI)) + 0.5);
                        tex[w * 2 + 1] = (float) ((y - (yPies - altura)) / altura);
                    }
                    if (c[k * 3 + 2] >= 0) {
                        float[] n = vn.get(c[k * 3 + 2]);
                        normales[w * 3] = n[0];
                        normales[w * 3 + 1] = n[1];
                        normales[w * 3 + 2] = n[2];
                    } else {
                        faltanNormales = true;
                    }
                    w++;
                }
                triangulos.add(new int[]{w - 3, w - 2, w - 1});
            }
            if (faltanNormales) {
                normales = GeneradorMallas.normalesPromediadas(puntos, triangulos);
            }
            int[] f = new int[total * 3];
            for (int k = 0; k < total; k++) {
                f[k * 3] = k;
                f[k * 3 + 1] = k;
                f[k * 3 + 2] = k;
            }
            TriangleMesh m = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
            m.getPoints().setAll(puntos);
            m.getTexCoords().setAll(tex);
            m.getNormals().setAll(normales);
            m.getFaces().setAll(f);
            MeshView vista = new MeshView(m);
            // Los modelos hechos a mano a veces traen caras invertidas: se dibujan por los dos lados
            vista.setCullFace(CullFace.NONE);
            salida.put(g.getKey(), vista);
        }
        return salida;
    }

    /** "v", "v/vt", "v//vn" o "v/vt/vn" → índices desde 0 (-1 si falta). Acepta índices negativos. */
    private static int[] indices(String s, int nv, int nvt, int nvn) {
        String[] p = s.split("/", -1);
        return new int[]{indice(p[0], nv),
                p.length > 1 && !p[1].isEmpty() ? indice(p[1], nvt) : -1,
                p.length > 2 && !p[2].isEmpty() ? indice(p[2], nvn) : -1};
    }

    private static int indice(String s, int cantidad) {
        int i = Integer.parseInt(s);
        return i < 0 ? cantidad + i : i - 1;
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }
}
