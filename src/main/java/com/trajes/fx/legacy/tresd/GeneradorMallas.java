package com.trajes.fx.legacy.tresd;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public final class GeneradorMallas {

    public static final int SEGMENTOS = 48;
     
    public static final double TAMANIO_TEXTURA = 150;

    private GeneradorMallas() {
    }

    @FunctionalInterface
    public interface Superficie {
        Point3D punto(double u, double v);
    }

    public static List<Point2D> perfil(double... radioAltura) {
        List<Point2D> control = new ArrayList<>();
        for (int i = 0; i + 1 < radioAltura.length; i += 2) {
            control.add(new Point2D(radioAltura[i], radioAltura[i + 1]));
        }
        return catmullRom(control, 6);
    }

    public static List<Point2D> catmullRom(List<Point2D> control, int muestras) {
        List<Point2D> salida = new ArrayList<>();
        int n = control.size();
        for (int i = 0; i < n - 1; i++) {
            Point2D p0 = control.get(Math.max(i - 1, 0));
            Point2D p1 = control.get(i);
            Point2D p2 = control.get(i + 1);
            Point2D p3 = control.get(Math.min(i + 2, n - 1));
            for (int k = 0; k < muestras; k++) {
                double t = (double) k / muestras;
                salida.add(new Point2D(cr(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t),
                        cr(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t)));
            }
        }
        salida.add(control.get(n - 1));
        return salida;
    }

    public static List<Point3D> catmullRom3D(List<Point3D> control, int muestras) {
        List<Point3D> salida = new ArrayList<>();
        int n = control.size();
        for (int i = 0; i < n - 1; i++) {
            Point3D p0 = control.get(Math.max(i - 1, 0));
            Point3D p1 = control.get(i);
            Point3D p2 = control.get(i + 1);
            Point3D p3 = control.get(Math.min(i + 2, n - 1));
            for (int k = 0; k < muestras; k++) {
                double t = (double) k / muestras;
                salida.add(new Point3D(cr(p0.getX(), p1.getX(), p2.getX(), p3.getX(), t),
                        cr(p0.getY(), p1.getY(), p2.getY(), p3.getY(), t),
                        cr(p0.getZ(), p1.getZ(), p2.getZ(), p3.getZ(), t)));
            }
        }
        salida.add(control.get(n - 1));
        return salida;
    }

    private static double cr(double a, double b, double c, double d, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }

    public static TriangleMesh torno(List<Point2D> perfil, int segmentos, double fondo) {
        return tornoOndulado(perfil, segmentos, fondo, 0, v -> 0, false);
    }

    public static TriangleMesh torno(List<Point2D> perfil) {
        return torno(perfil, SEGMENTOS, 1);
    }

    public static TriangleMesh tornoOndulado(List<Point2D> perfil, int segmentos, double fondo,
                                             int pliegues, DoubleUnaryOperator amplitud, boolean dobleCara) {
        int filas = perfil.size();
        int cols = segmentos + 1;
        double[] largo = largoAcumulado2D(perfil);
        Point3D[][] p = new Point3D[filas][cols];
        double radioMax = 0;
        for (int j = 0; j < filas; j++) {
            Point2D q = perfil.get(j);
            double a = amplitud.applyAsDouble(largo[j] / Math.max(largo[filas - 1], 1e-6));
            radioMax = Math.max(radioMax, q.getX());
            for (int i = 0; i < cols; i++) {
                
                double t = Math.PI / 2 + 2 * Math.PI * i / segmentos;
                double r = q.getX() * (1 + a * Math.sin(pliegues * t));
                p[j][i] = new Point3D(r * Math.cos(t), q.getY(), r * Math.sin(t) * fondo);
            }
        }
        double circunferencia = 2 * Math.PI * radioMax * (1 + fondo) / 2;
        int repU = (int) Math.max(1, Math.round(circunferencia / TAMANIO_TEXTURA));
        double repV = Math.max(largo[filas - 1] / TAMANIO_TEXTURA, 0.25);
        return rejilla(p, uvRegular(filas, cols, repU, largo, repV), dobleCara);
    }

    public static TriangleMesh tubo(List<Point3D> control, double[] radios, int segmentos, boolean cerrar) {
        int muestras = 8;
        List<Point3D> curva = catmullRom3D(control, muestras);
        int n = curva.size();
        double[] r = new double[n];
        for (int k = 0; k < n; k++) {
            double pos = Math.min((double) k / muestras, control.size() - 1);
            int i = (int) Math.min(Math.floor(pos), control.size() - 2);
            double f = pos - i;
            r[k] = radios[i] * (1 - f) + radios[i + 1] * f;
        }
        
        Point3D[] tangente = new Point3D[n];
        for (int k = 0; k < n; k++) {
            Point3D a = curva.get(Math.max(k - 1, 0));
            Point3D b = curva.get(Math.min(k + 1, n - 1));
            tangente[k] = b.subtract(a).normalize();
        }
        Point3D normal = perpendicular(tangente[0]);
        int extra = cerrar ? 2 : 0;
        int filas = n + extra;
        int cols = segmentos + 1;
        Point3D[][] p = new Point3D[filas][cols];
        double[] largo = new double[filas];
        int fila = 0;
        if (cerrar) {
            for (int i = 0; i < cols; i++) {
                p[0][i] = curva.get(0).subtract(tangente[0].multiply(r[0] * 0.35));
            }
            fila = 1;
        }
        for (int k = 0; k < n; k++, fila++) {
            Point3D t = tangente[k];
            normal = normal.subtract(t.multiply(normal.dotProduct(t))).normalize();
            Point3D binormal = normal.crossProduct(t).normalize();
            for (int i = 0; i < cols; i++) {
                double ang = 2 * Math.PI * i / segmentos;
                Point3D dir = normal.multiply(Math.cos(ang)).add(binormal.multiply(Math.sin(ang)));
                p[fila][i] = curva.get(k).add(dir.multiply(r[k]));
            }
        }
        if (cerrar) {
            for (int i = 0; i < cols; i++) {
                p[filas - 1][i] = curva.get(n - 1).add(tangente[n - 1].multiply(r[n - 1] * 0.35));
            }
        }
        for (int j = 1; j < filas; j++) {
            largo[j] = largo[j - 1] + p[j][0].distance(p[j - 1][0]);
        }
        double radioMedio = 0;
        for (double x : r) {
            radioMedio += x / n;
        }
        int repU = (int) Math.max(1, Math.round(2 * Math.PI * radioMedio / TAMANIO_TEXTURA));
        return rejilla(p, uvRegular(filas, cols, repU, largo, Math.max(largo[filas - 1] / TAMANIO_TEXTURA, 0.25)), false);
    }

    public static TriangleMesh elipsoide(double rx, double ry, double rz, int segmentos) {
        return superficie(segmentos, Math.max(8, segmentos / 2), (u, v) -> {
            double t = Math.PI / 2 + 2 * Math.PI * u;
            double f = Math.PI * v;
            return new Point3D(rx * Math.sin(f) * Math.cos(t), -ry * Math.cos(f), rz * Math.sin(f) * Math.sin(t));
        }, false, 1, 1);
    }

    public static TriangleMesh superElipsoide(double rx, double ry, double rz, double exponente, int segmentos) {
        return superficie(segmentos, Math.max(8, segmentos / 2), (u, v) -> {
            double t = Math.PI / 2 + 2 * Math.PI * u;
            double f = Math.PI * v;
            double sf = potencia(Math.sin(f), exponente);
            return new Point3D(rx * sf * potencia(Math.cos(t), exponente), -ry * potencia(Math.cos(f), exponente),
                    rz * sf * potencia(Math.sin(t), exponente));
        }, false, 1, 1);
    }

    public static TriangleMesh discoPlano(double radio, int segmentos) {
        int filas = 2;
        int cols = segmentos + 1;
        Point3D[][] p = new Point3D[filas][cols];
        double[][][] uv = new double[filas][cols][2];
        for (int i = 0; i < cols; i++) {
            
            double t = 2 * Math.PI * i / segmentos;
            p[0][i] = new Point3D(0, 0, 0);
            p[1][i] = new Point3D(radio * Math.cos(t), 0, radio * Math.sin(t));
            uv[0][i] = new double[]{0.5, 0.5};
            uv[1][i] = new double[]{0.5 + 0.5 * Math.cos(t), 0.5 + 0.5 * Math.sin(t)};
        }
        return rejilla(p, uv, false);
    }

    public static TriangleMesh superficie(int columnas, int filas, Superficie f, boolean dobleCara,
                                          double repU, double repV) {
        Point3D[][] p = new Point3D[filas + 1][columnas + 1];
        double[][][] uv = new double[filas + 1][columnas + 1][2];
        for (int j = 0; j <= filas; j++) {
            for (int i = 0; i <= columnas; i++) {
                double u = (double) i / columnas;
                double v = (double) j / filas;
                p[j][i] = f.punto(u, v);
                uv[j][i] = new double[]{u * repU, v * repV};
            }
        }
        return rejilla(p, uv, dobleCara);
    }

    private static double[][][] uvRegular(int filas, int cols, int repU, double[] largo, double repV) {
        double total = Math.max(largo[filas - 1], 1e-6);
        double[][][] uv = new double[filas][cols][2];
        for (int j = 0; j < filas; j++) {
            for (int i = 0; i < cols; i++) {
                uv[j][i] = new double[]{(double) i / (cols - 1) * repU, largo[j] / total * repV};
            }
        }
        return uv;
    }

    private static double[] largoAcumulado2D(List<Point2D> perfil) {
        double[] largo = new double[perfil.size()];
        for (int j = 1; j < perfil.size(); j++) {
            largo[j] = largo[j - 1] + perfil.get(j).distance(perfil.get(j - 1));
        }
        return largo;
    }

    static TriangleMesh rejilla(Point3D[][] p, double[][][] uv, boolean dobleCara) {
        int filas = p.length;
        int cols = p[0].length;
        int total = filas * cols;
        TriangleMesh m = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
        float[] puntos = new float[total * 3];
        float[] tex = new float[total * 2];
        for (int j = 0; j < filas; j++) {
            for (int i = 0; i < cols; i++) {
                int k = j * cols + i;
                puntos[k * 3] = (float) p[j][i].getX();
                puntos[k * 3 + 1] = (float) p[j][i].getY();
                puntos[k * 3 + 2] = (float) p[j][i].getZ();
                tex[k * 2] = (float) uv[j][i][0];
                tex[k * 2 + 1] = (float) uv[j][i][1];
            }
        }
        List<int[]> triangulos = new ArrayList<>();
        for (int j = 0; j < filas - 1; j++) {
            for (int i = 0; i < cols - 1; i++) {
                int a = j * cols + i;
                int b = (j + 1) * cols + i;
                int c = j * cols + i + 1;
                int d = (j + 1) * cols + i + 1;
                triangulos.add(new int[]{a, b, c});
                triangulos.add(new int[]{c, b, d});
            }
        }
        float[] normales = normalesPromediadas(puntos, triangulos);
        float[] todasNormales = normales;
        if (dobleCara) {
            todasNormales = new float[normales.length * 2];
            System.arraycopy(normales, 0, todasNormales, 0, normales.length);
            for (int k = 0; k < normales.length; k++) {
                todasNormales[normales.length + k] = -normales[k];
            }
        }
        int caras = triangulos.size() * (dobleCara ? 2 : 1);
        int[] f = new int[caras * 9];
        int w = 0;
        for (int[] t : triangulos) {
            for (int v : t) {
                f[w++] = v;
                f[w++] = v;
                f[w++] = v;
            }
        }
        if (dobleCara) {
            for (int[] t : triangulos) {
                for (int v : new int[]{t[0], t[2], t[1]}) {
                    f[w++] = v;
                    f[w++] = total + v;
                    f[w++] = v;
                }
            }
        }
        m.getPoints().setAll(puntos);
        m.getTexCoords().setAll(tex);
        m.getNormals().setAll(todasNormales);
        m.getFaces().setAll(f);
        return m;
    }

    static float[] normalesPromediadas(float[] puntos, List<int[]> triangulos) {
        int n = puntos.length / 3;
        Map<Long, double[]> acumulado = new HashMap<>();
        long[] clave = new long[n];
        for (int k = 0; k < n; k++) {
            clave[k] = claveDePosicion(puntos[k * 3], puntos[k * 3 + 1], puntos[k * 3 + 2]);
        }
        for (int[] t : triangulos) {
            double ax = puntos[t[0] * 3], ay = puntos[t[0] * 3 + 1], az = puntos[t[0] * 3 + 2];
            double ux = puntos[t[1] * 3] - ax, uy = puntos[t[1] * 3 + 1] - ay, uz = puntos[t[1] * 3 + 2] - az;
            double vx = puntos[t[2] * 3] - ax, vy = puntos[t[2] * 3 + 1] - ay, vz = puntos[t[2] * 3 + 2] - az;
            double nx = uy * vz - uz * vy;
            double ny = uz * vx - ux * vz;
            double nz = ux * vy - uy * vx;
            for (int v : t) {
                double[] s = acumulado.computeIfAbsent(clave[v], x -> new double[3]);
                s[0] += nx;
                s[1] += ny;
                s[2] += nz;
            }
        }
        float[] normales = new float[n * 3];
        for (int k = 0; k < n; k++) {
            double[] s = acumulado.getOrDefault(clave[k], new double[]{0, -1, 0});
            double largo = Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
            if (largo < 1e-9) {
                s = new double[]{0, -1, 0};
                largo = 1;
            }
            normales[k * 3] = (float) (s[0] / largo);
            normales[k * 3 + 1] = (float) (s[1] / largo);
            normales[k * 3 + 2] = (float) (s[2] / largo);
        }
        return normales;
    }

    private static long claveDePosicion(double x, double y, double z) {
        long qx = Math.round(x * 100) & 0x1FFFFF;
        long qy = Math.round(y * 100) & 0x1FFFFF;
        long qz = Math.round(z * 100) & 0x1FFFFF;
        return (qx << 42) | (qy << 21) | qz;
    }

    private static Point3D perpendicular(Point3D t) {
        Point3D eje = Math.abs(t.getX()) < 0.9 ? new Point3D(1, 0, 0) : new Point3D(0, 0, 1);
        return eje.subtract(t.multiply(eje.dotProduct(t))).normalize();
    }

    private static double potencia(double base, double exponente) {
        return Math.signum(base) * Math.pow(Math.abs(base), exponente);
    }
}
