/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.foto;

import pe.ehuanca.trajes.servicio.ExcepcionReglaNegocio;
import javafx.scene.image.Image;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Carga un render por capas desde una carpeta del classpath (por ejemplo {@code render/caporal/frente}):
 * <ul>
 *     <li>{@code base.png}: el bailarín completo (capa de abajo, no se tiñe).</li>
 *     <li>cualquier otro {@code *.png}: una prenda teñible (pantalon.png, chaqueta.png, botas.png...).</li>
 * </ul>
 * Las prendas se apilan en orden alfabético del nombre de archivo. Para forzar un orden se puede
 * anteponer un número: {@code 10_botas.png}, {@code 20_pantalon.png}, {@code 30_chaqueta.png}
 * (el número no aparece en pantalla).
 *
 * <p>Funciona igual desde IntelliJ (carpeta) y desde el .jar empaquetado, porque usa el buscador de
 * recursos de Spring en lugar de listar archivos del disco.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class RenderizadoCapas {

    public static final String BASE = "base.png";

    /** Nombres con tilde para los archivos conocidos (el resto se muestra tal cual, con mayúscula inicial). */
    private static final Map<String, String> NOMBRES = Map.of(
            "pantalon", "Pantalón", "pollera", "Pollera", "mascara", "Máscara", "boton", "Botón",
            "botones", "Botones", "cinturon", "Cinturón", "camison", "Camisón");

    private final Image base;
    private final List<PrendaTenible> prendas;

    private RenderizadoCapas(Image base, List<PrendaTenible> prendas) {
        this.base = base;
        this.prendas = prendas;
    }

    /** @param carpeta ruta dentro de resources, sin barra inicial: "render/caporal/frente" */
    public static RenderizadoCapas cargar(String carpeta) {
        Resource[] pngs;
        try {
            pngs = new PathMatchingResourcePatternResolver().getResources("classpath*:" + carpeta + "/*.png");
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo leer la carpeta " + carpeta + ": " + e.getMessage());
        }
        Arrays.sort(pngs, Comparator.comparing(r -> nombreArchivo(r).toLowerCase(Locale.ROOT)));

        Image base = null;
        List<PrendaTenible> prendas = new ArrayList<>();
        for (Resource r : pngs) {
            String archivo = nombreArchivo(r);
            Image img = leer(r);
            if (archivo.equalsIgnoreCase(BASE)) {
                base = img;
            } else {
                prendas.add(new PrendaTenible(archivo, nombreVisible(archivo), img));
            }
        }
        if (base == null) {
            throw new ExcepcionReglaNegocio("Falta " + BASE + " en src/main/resources/" + carpeta + ".");
        }
        for (PrendaTenible p : prendas) {
            if (p.imagen().getWidth() != base.getWidth() || p.imagen().getHeight() != base.getHeight()) {
                throw new ExcepcionReglaNegocio(p.archivo() + " mide " + (int) p.imagen().getWidth() + "x"
                        + (int) p.imagen().getHeight() + " pero " + BASE + " mide " + (int) base.getWidth() + "x"
                        + (int) base.getHeight() + ". Todas las capas deben renderizarse con el mismo tamaño.");
            }
        }
        return new RenderizadoCapas(base, List.copyOf(prendas));
    }

    /** "20_pantalon1.png" → "Pantalón"; "chaqueta.png" → "Chaqueta"; "botas_altas.png" → "Botas altas". */
    static String nombreVisible(String archivo) {
        String n = archivo.replaceFirst("(?i)\\.png$", "")
                .replaceFirst("^\\d+[_\\-\\s]*", "")   // prefijo de orden
                .replaceFirst("[_\\-\\s]*\\d+$", "")   // número de versión al final
                .replace('_', ' ').replace('-', ' ').trim();
        if (n.isEmpty()) {
            return archivo;
        }
        String conocido = NOMBRES.get(n.toLowerCase(Locale.ROOT));
        if (conocido != null) {
            return conocido;
        }
        return n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1);
    }

    private static String nombreArchivo(Resource r) {
        String n = r.getFilename();
        return n == null ? "" : n;
    }

    private static Image leer(Resource r) {
        try (InputStream in = r.getInputStream()) {
            Image img = new Image(in);
            if (img.isError()) {
                throw new ExcepcionReglaNegocio("No se pudo leer la imagen " + r.getFilename() + ".");
            }
            return img;
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo leer la imagen " + r.getFilename() + ": " + e.getMessage());
        }
    }

    public Image base() {
        return base;
    }

    public List<PrendaTenible> prendas() {
        return prendas;
    }

    public double ancho() {
        return base.getWidth();
    }

    public double alto() {
        return base.getHeight();
    }
}
