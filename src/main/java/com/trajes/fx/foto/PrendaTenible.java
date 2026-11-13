package com.trajes.fx.foto;

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
 */
public final class PrendaTenible {

    private final String archivo;
    private final String nombre;
    private final int ancho;
    private final int alto;
    /** Píxeles originales en ARGB (sin premultiplicar). No se modifican nunca. */
    private final int[] original;
    /** Buffer reutilizable para el resultado del teñido. */
    private final int[] resultado;
    /** Imagen que se muestra; se reescribe en cada cambio de color. */
    private final WritableImage imagen;
    private final double fraccionTransparente;
    private Color color = Color.WHITE;

    public PrendaTenible(String archivo, String nombre, Image fuente) {
        this.archivo = archivo;
        this.nombre = nombre;
        this.ancho = (int) fuente.getWidth();
        this.alto = (int) fuente.getHeight();
        this.original = new int[ancho * alto];
        this.resultado = new int[ancho * alto];
        fuente.getPixelReader().getPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), original, 0, ancho);
        this.imagen = new WritableImage(ancho, alto);
        int transparentes = 0;
        for (int p : original) {
            if (p >>> 24 == 0) {
                transparentes++;
            }
        }
        this.fraccionTransparente = original.length == 0 ? 0 : (double) transparentes / original.length;
        aplicarColor(Color.WHITE);
    }

    /** rgb = rgb_original × color, conservando el alfa de cada píxel. */
    public void aplicarColor(Color nuevo) {
        this.color = nuevo;
        int cr = (int) Math.round(nuevo.getRed() * 255);
        int cg = (int) Math.round(nuevo.getGreen() * 255);
        int cb = (int) Math.round(nuevo.getBlue() * 255);
        for (int i = 0; i < original.length; i++) {
            int p = original[i];
            int a = p >>> 24;
            if (a == 0) {
                resultado[i] = 0;
                continue;
            }
            int r = ((p >> 16) & 0xFF) * cr / 255;
            int g = ((p >> 8) & 0xFF) * cg / 255;
            int b = (p & 0xFF) * cb / 255;
            resultado[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        imagen.getPixelWriter().setPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), resultado, 0, ancho);
    }

    public String archivo() {
        return archivo;
    }

    public String nombre() {
        return nombre;
    }

    /** Proporción de píxeles totalmente transparentes (una capa de prenda bien hecha suele pasar de 0.5). */
    public double fraccionTransparente() {
        return fraccionTransparente;
    }

    public Color color() {
        return color;
    }

    public WritableImage imagen() {
        return imagen;
    }
}
