/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.foto;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Una prenda del render (por ejemplo pantalon.png): una capa PNG con transparencia, pintada en blanco,
 * que se tiñe multiplicando cada píxel por el color elegido.
 *
 * <p>La imagen original se guarda en memoria (como arreglo ARGB) para recalcular el teñido cada vez
 * que cambia el color sin volver a leer el archivo.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class PrendaTenible {

    private final String archivoErht;
    private final String nombreErht;
    private final int anchoErht;
    private final int altoErht;
    private final int[] originalErht;
    private final int[] resultadoErht;
    private final WritableImage imagenErht;
    private final double fraccionTransparenteErht;
    private Color colorErht = Color.WHITE;

    public PrendaTenible(String archivo, String nombre, Image fuente) {
        this.archivoErht = archivo;
        this.nombreErht = nombre;
        this.anchoErht = (int) fuente.getWidth();
        this.altoErht = (int) fuente.getHeight();
        this.originalErht = new int[anchoErht * altoErht];
        this.resultadoErht = new int[anchoErht * altoErht];
        fuente.getPixelReader().getPixels(0, 0, anchoErht, altoErht, PixelFormat.getIntArgbInstance(), originalErht, 0, anchoErht);
        this.imagenErht = new WritableImage(anchoErht, altoErht);
        int transparentesErht = 0;
        for (int p : originalErht) {
            if (p >>> 24 == 0) {
                transparentesErht++;
            }
        }
        this.fraccionTransparenteErht = originalErht.length == 0 ? 0 : (double) transparentesErht / originalErht.length;
        aplicarColor(Color.WHITE);
    }

    public void aplicarColor(Color nuevo) {
        this.colorErht = nuevo;
        int cr = (int) Math.round(nuevo.getRed() * 255);
        int cg = (int) Math.round(nuevo.getGreen() * 255);
        int cb = (int) Math.round(nuevo.getBlue() * 255);
        for (int i = 0; i < originalErht.length; i++) {
            int p = originalErht[i];
            int a = p >>> 24;
            if (a == 0) {
                resultadoErht[i] = 0;
                continue;
            }
            int r = ((p >> 16) & 0xFF) * cr / 255;
            int g = ((p >> 8) & 0xFF) * cg / 255;
            int b = (p & 0xFF) * cb / 255;
            resultadoErht[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        imagenErht.getPixelWriter().setPixels(0, 0, anchoErht, altoErht, PixelFormat.getIntArgbInstance(), resultadoErht, 0, anchoErht);
    }

    public String archivo() {
        return archivoErht;
    }

    public String nombre() {
        return nombreErht;
    }

    public double fraccionTransparente() {
        return fraccionTransparenteErht;
    }

    public Color color() {
        return colorErht;
    }

    public WritableImage imagen() {
        return imagenErht;
    }
}
