package com.trajes.fx.legacy.tresd;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class GeneradorTexturas {

    public static final int N = 512;

    public enum Tipo {
        TELA(0.10, 8, 0.45),
        TERCIOPELO(0.16, 5, 0.9),
        CUERO(0.34, 24, 0.5),
        PIEL(0.16, 12, 0.7),
        LACA(0.65, 48, 0.4),
        METAL(1.00, 80, 0.6),
        MADERA(0.14, 16, 1.2),
        AGUAYO(0.10, 8, 0.6),
        ENCAJE(0.12, 10, 1.0),
        PLUMA(0.12, 10, 0.6);

        final double brillo;
        final double potencia;
        final double relieve;

        Tipo(double brillo, double potencia, double relieve) {
            this.brillo = brillo;
            this.potencia = potencia;
            this.relieve = relieve;
        }
    }

    private static final Color ORO = Color.web("#D9B24A");
    private static final Map<String, Image> CACHE = new HashMap<>();
    private static final Map<Tipo, float[]> ALTURAS = new EnumMap<>(Tipo.class);
    private static float[] bordado;
    private static float[] lentejuela;
    private static float[] brilloLentejuela;

    private GeneradorTexturas() {
    }

    public static void aplicar(PhongMaterial m, Tipo tipo, Color color, boolean conBordado, boolean conLentejuelas) {
        m.setDiffuseColor(Color.WHITE);
        m.setDiffuseMap(difusa(tipo, color, conBordado, conLentejuelas));
        m.setSpecularColor(Color.WHITE);
        m.setSpecularMap(especular(tipo, conBordado, conLentejuelas));
        m.setBumpMap(relieve(tipo, conBordado, conLentejuelas));
        double potencia = tipo.potencia;
        if (conBordado || conLentejuelas) {
            potencia = Math.max(potencia, 34);
        }
        m.setSpecularPower(potencia);
    }

    public static PhongMaterial material(Tipo tipo, Color color) {
        PhongMaterial m = new PhongMaterial();
        aplicar(m, tipo, color, false, false);
        return m;
    }

    public static int imagenesEnCache() {
        return CACHE.size();
    }

    public static Image difusa(Tipo tipo, Color c, boolean conBordado, boolean conLentejuelas) {
        String clave = "D|" + tipo + "|" + c + "|" + conBordado + "|" + conLentejuelas;
        return CACHE.computeIfAbsent(clave, k -> {
            float[] h = altura(tipo);
            float[] b = conBordado ? bordado() : null;
            float[] s = conLentejuelas ? lentejuela() : null;
            float[] sb = conLentejuelas ? brilloLentejuela : null;
            int[] px = new int[N * N];
            double r0 = c.getRed();
            double g0 = c.getGreen();
            double b0 = c.getBlue();
            for (int i = 0; i < N * N; i++) {
                double sombra = sombreado(tipo, h[i]);
                double r = r0 * sombra + (sombra - 1) * 0.05;
                double g = g0 * sombra + (sombra - 1) * 0.05;
                double bl = b0 * sombra + (sombra - 1) * 0.05;
                if (tipo == Tipo.AGUAYO) {
                    double[] franja = franjaAguayo(i / N, i % N, r, g, bl);
                    r = franja[0];
                    g = franja[1];
                    bl = franja[2];
                }
                if (s != null && s[i] > 0) {
                    
                    double luz = 0.75 + 0.55 * sb[i];
                    double lr = (r0 * 0.75 + 0.25) * luz;
                    double lg = (g0 * 0.75 + 0.25) * luz;
                    double lb = (b0 * 0.75 + 0.25) * luz;
                    r = mezcla(r, lr, s[i]);
                    g = mezcla(g, lg, s[i]);
                    bl = mezcla(bl, lb, s[i]);
                }
                if (b != null && b[i] > 0) {
                    double hilo = 0.72 + 0.4 * h[(i * 7) % (N * N)];
                    r = mezcla(r, ORO.getRed() * hilo, b[i]);
                    g = mezcla(g, ORO.getGreen() * hilo, b[i]);
                    bl = mezcla(bl, ORO.getBlue() * hilo, b[i]);
                }
                px[i] = argb(1, r, g, bl);
            }
            if (tipo == Tipo.ENCAJE) {
                aplicarAlfaEncaje(px);
            }
            return imagen(px);
        });
    }

    public static Image especular(Tipo tipo, boolean conBordado, boolean conLentejuelas) {
        String clave = "S|" + tipo + "|" + conBordado + "|" + conLentejuelas;
        return CACHE.computeIfAbsent(clave, k -> {
            float[] h = altura(tipo);
            float[] b = conBordado ? bordado() : null;
            float[] s = conLentejuelas ? lentejuela() : null;
            int[] px = new int[N * N];
            for (int i = 0; i < N * N; i++) {
                double v = tipo.brillo * (0.7 + 0.6 * h[i]);
                double r = v, g = v, bl = v;
                if (s != null) {
                    double e = s[i] * (0.8 + 0.2 * brilloLentejuela[i]);
                    r = mezcla(r, 1, e);
                    g = mezcla(g, 1, e);
                    bl = mezcla(bl, 1, e);
                }
                if (b != null) {
                    r = mezcla(r, 1.0, b[i]);
                    g = mezcla(g, 0.88, b[i]);
                    bl = mezcla(bl, 0.6, b[i]);
                }
                px[i] = argb(1, r, g, bl);
            }
            return imagen(px);
        });
    }

    public static Image relieve(Tipo tipo, boolean conBordado, boolean conLentejuelas) {
        String clave = "B|" + tipo + "|" + conBordado + "|" + conLentejuelas;
        return CACHE.computeIfAbsent(clave, k -> {
            float[] h = altura(tipo);
            float[] b = conBordado ? bordado() : null;
            float[] s = conLentejuelas ? lentejuela() : null;
            float[] total = new float[N * N];
            for (int i = 0; i < N * N; i++) {
                double v = h[i] * tipo.relieve;
                if (s != null) {
                    v = mezcla(v, 1.6 + 0.4 * h[i], s[i]);
                }
                if (b != null) {
                    v += b[i] * (1.8 + 0.8 * Math.sin((i % N) * 1.7 + (i / N) * 0.9));
                }
                total[i] = (float) v;
            }
            return mapaDeNormales(total, 1.5);
        });
    }

    public static Image pluma(Color c) {
        return CACHE.computeIfAbsent("PLUMA|" + c, k -> {
            float[] ruido = altura(Tipo.PLUMA);
            int[] px = new int[N * N];
            for (int y = 0; y < N; y++) {
                double v = (double) y / N;
                double ancho = 0.44 * Math.pow(Math.sin(Math.PI * Math.min(1, v * 1.08)), 0.55) * (1 - 0.35 * v);
                for (int x = 0; x < N; x++) {
                    double u = (double) x / N - 0.5;
                    int i = y * N + x;
                    double borde = ancho * (0.86 + 0.14 * ruido[i]);
                    double a = suave(borde, borde - 0.03, Math.abs(u));
                    if (v > 0.93) {
                        a = Math.abs(u) < 0.012 ? 1 : 0;
                    }
                    double barba = 0.5 + 0.5 * Math.sin((v * 90 + Math.abs(u) * 55) * Math.PI);
                    double luz = 0.72 + 0.28 * barba;
                    double r = c.getRed() * luz, g = c.getGreen() * luz, b = c.getBlue() * luz;
                    if (Math.abs(u) < 0.012) {
                        r = g = b = 0.92;
                        a = Math.max(a, v > 0.02 ? 1 : 0);
                    }
                    px[i] = argb(a, r, g, b);
                }
            }
            return imagen(px);
        });
    }

    public static Image sombra() {
        return CACHE.computeIfAbsent("SOMBRA", k -> {
            int[] px = new int[N * N];
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    double dx = (x + 0.5) / N - 0.5;
                    double dy = (y + 0.5) / N - 0.5;
                    double r = Math.sqrt(dx * dx + dy * dy) / 0.5;
                    double a = r >= 1 ? 0 : 0.8 * Math.pow(1 - r, 1.5);
                    px[y * N + x] = argb(a, 0.05, 0.04, 0.04);
                }
            }
            return imagen(px);
        });
    }

    private static double sombreado(Tipo tipo, double h) {
        return switch (tipo) {
            case TELA -> 0.90 + 0.13 * h;
            case TERCIOPELO -> 0.78 + 0.32 * h;
            case CUERO -> 0.9 + 0.12 * h;
            case PIEL -> 0.93 + 0.12 * h;
            case LACA -> 0.94 + 0.08 * h;
            case METAL -> 0.82 + 0.28 * h;
            case MADERA -> 0.62 + 0.42 * h;
            case AGUAYO -> 0.84 + 0.24 * h;
            case ENCAJE -> 0.90 + 0.12 * h;
            case PLUMA -> 0.85 + 0.2 * h;
        };
    }

    private static float[] altura(Tipo tipo) {
        return ALTURAS.computeIfAbsent(tipo, t -> switch (t) {
            case TELA, AGUAYO -> trama(128, 0.22, 11);
            case TERCIOPELO -> combinar(fbm(6, 5, 21), fbm(48, 2, 22), 0.8);
            case CUERO -> cuero();
            case PIEL -> combinar(fbm(5, 4, 41), fbm(96, 2, 42), 0.75);
            case LACA -> fbm(4, 3, 51);
            case METAL -> metalCepillado();
            case MADERA -> madera();
            case ENCAJE -> trama(128, 0.4, 61);
            case PLUMA -> fbm(16, 3, 71);
        });
    }

    private static float[] trama(int hilos, double pesoHilo, int semilla) {
        float[] r = fbm(8, 4, semilla);
        float[] out = new float[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double cx = (double) x * hilos / N;
                double cy = (double) y * hilos / N;
                int ix = (int) cx;
                int iy = (int) cy;
                boolean urdimbre = ((ix + iy) & 1) == 0;
                double hilo = urdimbre ? Math.sin(Math.PI * (cx - ix)) : Math.sin(Math.PI * (cy - iy));
                out[y * N + x] = (float) (pesoHilo * hilo + (1 - pesoHilo) * r[y * N + x]);
            }
        }
        return out;
    }

    private static float[] cuero() {
        int celdas = 52;
        float[] base = fbm(6, 4, 31);
        float[] out = new float[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double px = (double) x * celdas / N;
                double py = (double) y * celdas / N;
                int cx = (int) px;
                int cy = (int) py;
                double f1 = 9;
                double f2 = 9;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int gx = cx + dx;
                        int gy = cy + dy;
                        double fx = gx + hash(Math.floorMod(gx, celdas), Math.floorMod(gy, celdas), 3);
                        double fy = gy + hash(Math.floorMod(gx, celdas), Math.floorMod(gy, celdas), 4);
                        double d = Math.hypot(fx - px, fy - py);
                        if (d < f1) {
                            f2 = f1;
                            f1 = d;
                        } else if (d < f2) {
                            f2 = d;
                        }
                    }
                }
                double grieta = suave(0, 0.18, f2 - f1);
                out[y * N + x] = (float) (0.4 * grieta + 0.6 * base[y * N + x]);
            }
        }
        return out;
    }

    private static float[] metalCepillado() {
        float[] fino = new float[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                
                fino[y * N + x] = ruido(x * 2.0 / N, y * 160.0 / N, 2, 160, 81);
            }
        }
        return combinar(fino, fbm(4, 3, 82), 0.6);
    }

    private static float[] madera() {
        float[] out = new float[N * N];
        int tablas = 8;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int t = x * tablas / N;
                double desfase = hash(t, 0, 91) * 10;
                double veta = ruido(x * 24.0 / N, y * 3.0 / N + desfase, 24, 3, 92);
                double anillo = 0.5 + 0.5 * Math.sin((x * 0.21 + veta * 9 + desfase) * 1.3);
                double junta = (x % (N / tablas)) < 2 ? 0 : 1;
                double tono = 0.75 + 0.25 * hash(t, 1, 93);
                out[y * N + x] = (float) (junta * tono * (0.55 + 0.3 * anillo + 0.15 * veta));
            }
        }
        return out;
    }

    private static double[] franjaAguayo(int y, int x, double r, double g, double b) {
        int banda = (y * 16 / N);
        double local = (y * 16.0 / N) - banda;
        Color[] acentos = {Color.web("#B71C1C"), Color.web("#1B7F3A"), Color.web("#E0B43A"), Color.web("#F2EDE4")};
        switch (banda % 4) {
            case 1 -> {
                
                if (local > 0.3 && local < 0.7) {
                    Color a = acentos[(banda / 4) % acentos.length];
                    return new double[]{a.getRed() * 0.9, a.getGreen() * 0.9, a.getBlue() * 0.9};
                }
            }
            case 2 -> {
                
                double cx = (x % 32) / 32.0 - 0.5;
                double cy = local - 0.5;
                if (Math.abs(cx) + Math.abs(cy) < 0.32 && Math.abs(cx) + Math.abs(cy) > 0.18) {
                    Color a = acentos[(x / 32 + banda) % acentos.length];
                    return new double[]{a.getRed(), a.getGreen(), a.getBlue()};
                }
                return new double[]{r * 0.62, g * 0.62, b * 0.62};
            }
            default -> {
            }
        }
        return new double[]{r, g, b};
    }

    private static void aplicarAlfaEncaje(int[] px) {
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double cx = (x % 32) - 16;
                double cy = (y % 32) - 16;
                double d = Math.hypot(cx, cy);
                double a = d < 5.5 ? 0.0 : (d < 7 ? 0.6 : 1.0);
                double borde = Math.min(Math.min(x, N - 1 - x), Math.min(y, N - 1 - y));
                double festonDist = borde - (10 + 8 * Math.abs(Math.sin(Math.PI * (x + y) / 48.0)));
                if (festonDist < 0) {
                    a = 0;
                }
                int i = y * N + x;
                int alfa = (int) Math.round(a * 255);
                px[i] = (alfa << 24) | (px[i] & 0xFFFFFF);
            }
        }
    }

    private static float[] bordado() {
        if (bordado != null) {
            return bordado;
        }
        Canvas lienzo = new Canvas(N, N);
        GraphicsContext g = lienzo.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, N, N);
        g.setStroke(Color.WHITE);
        g.setFill(Color.WHITE);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);

        g.setLineWidth(5);
        for (int lado = 0; lado < 2; lado++) {
            double base = lado == 0 ? 0 : N;
            double s = lado == 0 ? 1 : -1;
            g.strokeLine(0, base + s * 14, N, base + s * 14);
            g.strokeLine(0, base + s * 90, N, base + s * 90);
            for (int x0 = 0; x0 < N; x0 += 64) {
                double[] xs = {x0, x0, x0 + 12, x0 + 12, x0 + 24, x0 + 24, x0 + 40, x0 + 40, x0 + 52, x0 + 52, x0 + 64, x0 + 64};
                double[] ys = {78, 62, 62, 46, 46, 30, 30, 46, 46, 62, 62, 78};
                for (int k = 0; k < ys.length; k++) {
                    ys[k] = base + s * ys[k];
                }
                g.strokePolyline(xs, ys, xs.length);
                g.fillOval(x0 + 28, base + s * 58 - 4, 8, 8);
            }
        }

        g.setLineWidth(4.5);
        for (int fila = 0; fila < 2; fila++) {
            double cy = 100 + 78 + fila * 156;
            for (int col = 0; col < 4; col++) {
                double cx = 64 + col * 128;
                int motivo = (col + fila) % 2 == 0 ? 0 : (fila == 0 ? 1 : 2);
                switch (motivo) {
                    case 0 -> rombo(g, cx, cy);
                    case 1 -> sol(g, cx, cy);
                    default -> espiral(g, cx, cy);
                }
            }
        }

        SnapshotParameters p = new SnapshotParameters();
        p.setFill(Color.BLACK);
        WritableImage img = lienzo.snapshot(p, null);
        int[] px = new int[N * N];
        img.getPixelReader().getPixels(0, 0, N, N, PixelFormat.getIntArgbInstance(), px, 0, N);
        bordado = new float[N * N];
        for (int i = 0; i < N * N; i++) {
            bordado[i] = ((px[i] >> 8) & 0xFF) / 255f;
        }
        return bordado;
    }

    private static void rombo(GraphicsContext g, double cx, double cy) {
        for (double r : new double[]{58, 40, 22}) {
            g.strokePolygon(new double[]{cx, cx + r, cx, cx - r}, new double[]{cy - r, cy, cy + r, cy}, 4);
        }
        g.fillOval(cx - 6, cy - 6, 12, 12);
    }

    private static void sol(GraphicsContext g, double cx, double cy) {
        g.strokeOval(cx - 24, cy - 24, 48, 48);
        g.fillOval(cx - 10, cy - 10, 20, 20);
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            double r1 = 32;
            double r2 = k % 2 == 0 ? 56 : 46;
            g.strokeLine(cx + r1 * Math.cos(a), cy + r1 * Math.sin(a), cx + r2 * Math.cos(a), cy + r2 * Math.sin(a));
        }
    }

    private static void espiral(GraphicsContext g, double cx, double cy) {
        int n = 120;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int k = 0; k < n; k++) {
            double t = k * 5 * Math.PI / n;
            double r = 4 + 3.4 * t;
            xs[k] = cx + r * Math.cos(t);
            ys[k] = cy + r * Math.sin(t);
        }
        g.strokePolyline(xs, ys, n);
    }

    private static float[] lentejuela() {
        if (lentejuela != null) {
            return lentejuela;
        }
        lentejuela = new float[N * N];
        brilloLentejuela = new float[N * N];
        int cols = 40;
        int filas = 46;
        double dx = (double) N / cols;
        double dy = (double) N / filas;
        double radio = dx * 0.42;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int fila = (int) (y / dy);
                double desfase = (fila % 2) * dx / 2;
                double fx = (x - desfase) / dx;
                int col = (int) Math.floor(fx);
                double cx = (col + 0.5) * dx + desfase;
                double cy = (fila + 0.5) * dy;
                double d = Math.hypot(x - cx, y - cy);
                if (d < radio + 1) {
                    int i = y * N + x;
                    lentejuela[i] = (float) suave(radio + 1, radio - 1, d);
                    double propio = hash(Math.floorMod(col, cols), fila, 101);
                    
                    double brillo = Math.max(0, 1 - Math.hypot(x - cx + radio * 0.35, y - cy + radio * 0.35) / radio);
                    brilloLentejuela[i] = (float) Math.min(1, 0.45 * propio + 0.9 * brillo * brillo);
                }
            }
        }
        return lentejuela;
    }

    private static float[] fbm(int frecuencia, int octavas, int semilla) {
        float[] out = new float[N * N];
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double u = (double) x / N;
                double v = (double) y / N;
                double suma = 0;
                double amp = 1;
                int f = frecuencia;
                for (int o = 0; o < octavas; o++) {
                    suma += amp * ruido(u * f, v * f, f, f, semilla + o);
                    amp *= 0.5;
                    f *= 2;
                }
                float s = (float) suma;
                out[y * N + x] = s;
                min = Math.min(min, s);
                max = Math.max(max, s);
            }
        }
        float rango = Math.max(max - min, 1e-6f);
        for (int i = 0; i < out.length; i++) {
            out[i] = (out[i] - min) / rango;
        }
        return out;
    }

    private static float[] combinar(float[] a, float[] b, double pesoA) {
        float[] out = new float[N * N];
        for (int i = 0; i < out.length; i++) {
            out[i] = (float) (pesoA * a[i] + (1 - pesoA) * b[i]);
        }
        return out;
    }

    private static float ruido(double x, double y, int periodoX, int periodoY, int semilla) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = x - x0;
        double fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        int xa = Math.floorMod(x0, periodoX);
        int xb = Math.floorMod(x0 + 1, periodoX);
        int ya = Math.floorMod(y0, periodoY);
        int yb = Math.floorMod(y0 + 1, periodoY);
        double a = hash(xa, ya, semilla);
        double b = hash(xb, ya, semilla);
        double c = hash(xa, yb, semilla);
        double d = hash(xb, yb, semilla);
        return (float) (a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy);
    }

    private static float hash(int x, int y, int semilla) {
        int h = x * 374761393 + y * 668265263 + semilla * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static Image mapaDeNormales(float[] h, double fuerza) {
        int[] px = new int[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                double dx = h[y * N + Math.floorMod(x + 1, N)] - h[y * N + Math.floorMod(x - 1, N)];
                double dy = h[Math.floorMod(y + 1, N) * N + x] - h[Math.floorMod(y - 1, N) * N + x];
                double nx = -dx * fuerza;
                double ny = -dy * fuerza;
                double nz = 1;
                double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
                px[y * N + x] = argb(1, (nx / l) * 0.5 + 0.5, (ny / l) * 0.5 + 0.5, (nz / l) * 0.5 + 0.5);
            }
        }
        return imagen(px);
    }

    private static WritableImage imagen(int[] px) {
        WritableImage img = new WritableImage(N, N);
        img.getPixelWriter().setPixels(0, 0, N, N, PixelFormat.getIntArgbInstance(), px, 0, N);
        return img;
    }

    private static int argb(double a, double r, double g, double b) {
        return (canal(a) << 24) | (canal(r) << 16) | (canal(g) << 8) | canal(b);
    }

    private static int canal(double v) {
        return (int) Math.round(Math.max(0, Math.min(1, v)) * 255);
    }

    private static double mezcla(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double suave(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }
}
