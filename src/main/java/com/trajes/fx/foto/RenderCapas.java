package com.trajes.fx.foto;

import com.trajes.servicio.ReglaNegocioException;
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

public final class RenderCapas {

    public static final String BASE = "base.png";

    private static final Map<String, String> NOMBRES = Map.of(
            "pantalon", "Pantalón", "pollera", "Pollera", "mascara", "Máscara", "boton", "Botón",
            "botones", "Botones", "cinturon", "Cinturón", "camison", "Camisón");

    private final Image base;
    private final List<PrendaTenible> prendas;

    private RenderCapas(Image base, List<PrendaTenible> prendas) {
        this.base = base;
        this.prendas = prendas;
    }

    public static RenderCapas cargar(String carpeta) {
        Resource[] pngs;
        try {
            pngs = new PathMatchingResourcePatternResolver().getResources("classpath*:" + carpeta + "/*.png");
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer la carpeta " + carpeta + ": " + e.getMessage());
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
            throw new ReglaNegocioException("Falta " + BASE + " en src/main/resources/" + carpeta + ".");
        }
        for (PrendaTenible p : prendas) {
            if (p.imagen().getWidth() != base.getWidth() || p.imagen().getHeight() != base.getHeight()) {
                throw new ReglaNegocioException(p.archivo() + " mide " + (int) p.imagen().getWidth() + "x"
                        + (int) p.imagen().getHeight() + " pero " + BASE + " mide " + (int) base.getWidth() + "x"
                        + (int) base.getHeight() + ". Todas las capas deben renderizarse con el mismo tamaño.");
            }
        }
        return new RenderCapas(base, List.copyOf(prendas));
    }

    static String nombreVisible(String archivo) {
        String n = archivo.replaceFirst("(?i)\\.png$", "")
                .replaceFirst("^\\d+[_\\-\\s]*", "")   
                .replaceFirst("[_\\-\\s]*\\d+$", "")   
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
                throw new ReglaNegocioException("No se pudo leer la imagen " + r.getFilename() + ".");
            }
            return img;
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer la imagen " + r.getFilename() + ": " + e.getMessage());
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
