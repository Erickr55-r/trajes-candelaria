/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.foto;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Descubre los renders por capas que hay en el classpath:
 * {@code render/<danza>/<ángulo>/base.png} (+ un PNG por prenda). No hay que tocar código para agregar
 * una danza o un ángulo: basta con crear la carpeta.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class CatalogoRenderizados {

    /** Orden en que se recorren los ángulos con las flechas ◀ ▶ (los demás van después, por nombre). */
    private static final List<String> ORDEN_ANGULOS = List.of("frente", "perfil", "espalda");

    private CatalogoRenderizados() {
    }

    /** carpeta de danza → ángulos disponibles (en orden), p. ej. "caporal" → [frente, perfil, espalda]. */
    public static Map<String, List<String>> danzas() {
        Resource[] basesErht;
        try {
            basesErht = new PathMatchingResourcePatternResolver().getResources("classpath*:render/*/*/base.png");
        } catch (IOException e) {
            return Map.of();
        }
        Map<String, List<String>> mapaErht = new TreeMap<>();
        for (Resource r : basesErht) {
            String rutaErht;
            try {
                rutaErht = r.getURL().toString().replace('\\', '/');
            } catch (IOException e) {
                continue;
            }
            int i = rutaErht.lastIndexOf("/render/");
            if (i < 0) {
                continue;
            }
            String[] partesErht = rutaErht.substring(i + "/render/".length()).split("/");
            if (partesErht.length >= 3) {
                mapaErht.computeIfAbsent(partesErht[0], k -> new ArrayList<>()).add(partesErht[1]);
            }
        }
        Map<String, List<String>> ordenadoErht = new LinkedHashMap<>();
        mapaErht.forEach((danza, angulos) -> {
            List<String> l = new ArrayList<>(angulos.stream().distinct().toList());
            l.sort(Comparator.comparing((String a) -> {
                int k = ORDEN_ANGULOS.indexOf(a);
                return k < 0 ? 99 : k;
            }).thenComparing(a -> a));
            ordenadoErht.put(danza, List.copyOf(l));
        });
        return ordenadoErht;
    }

    /** Carpeta de render que corresponde a una danza de la base ("Caporales" ↔ render/caporal). */
    public static String carpetaDe(String danza, Map<String, List<String>> disponibles) {
        String d = clave(danza);
        for (String carpetaErht : disponibles.keySet()) {
            String c = clave(carpetaErht);
            if (!c.isEmpty() && (d.startsWith(c) || c.startsWith(d))) {
                return carpetaErht;
            }
        }
        return null;
    }

    /** "Pantalón" → "pantalon"; sin tildes, en minúsculas y sin espacios. */
    public static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildesErht = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildesErht.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
