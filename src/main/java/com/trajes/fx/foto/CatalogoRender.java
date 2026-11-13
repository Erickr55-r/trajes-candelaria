package com.trajes.fx.foto;

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
 */
public final class CatalogoRender {

    /** Orden en que se recorren los ángulos con las flechas ◀ ▶ (los demás van después, por nombre). */
    private static final List<String> ORDEN_ANGULOS = List.of("frente", "perfil", "espalda");

    private CatalogoRender() {
    }

    /** carpeta de danza → ángulos disponibles (en orden), p. ej. "caporal" → [frente, perfil, espalda]. */
    public static Map<String, List<String>> danzas() {
        Resource[] bases;
        try {
            bases = new PathMatchingResourcePatternResolver().getResources("classpath*:render/*/*/base.png");
        } catch (IOException e) {
            return Map.of();
        }
        Map<String, List<String>> mapa = new TreeMap<>();
        for (Resource r : bases) {
            String url;
            try {
                url = r.getURL().toString().replace('\\', '/');
            } catch (IOException e) {
                continue;
            }
            int i = url.lastIndexOf("/render/");
            if (i < 0) {
                continue;
            }
            String[] partes = url.substring(i + "/render/".length()).split("/");
            if (partes.length >= 3) {
                mapa.computeIfAbsent(partes[0], k -> new ArrayList<>()).add(partes[1]);
            }
        }
        Map<String, List<String>> ordenado = new LinkedHashMap<>();
        mapa.forEach((danza, angulos) -> {
            List<String> l = new ArrayList<>(angulos.stream().distinct().toList());
            l.sort(Comparator.comparing((String a) -> {
                int k = ORDEN_ANGULOS.indexOf(a);
                return k < 0 ? 99 : k;
            }).thenComparing(a -> a));
            ordenado.put(danza, List.copyOf(l));
        });
        return ordenado;
    }

    /** Carpeta de render que corresponde a una danza de la base ("Caporales" ↔ render/caporal). */
    public static String carpetaDe(String danza, Map<String, List<String>> disponibles) {
        String d = clave(danza);
        for (String carpeta : disponibles.keySet()) {
            String c = clave(carpeta);
            if (!c.isEmpty() && (d.startsWith(c) || c.startsWith(d))) {
                return carpeta;
            }
        }
        return null;
    }

    /** "Pantalón" → "pantalon"; sin tildes, en minúsculas y sin espacios. */
    public static String clave(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
