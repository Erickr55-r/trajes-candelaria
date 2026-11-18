package com.trajes.fx;

import javafx.scene.Group;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.Locale;
import java.util.Map;

public final class Adornos {

    private static final String CHAKANA_9X9 =
            "M3 0 H6 V1 H7 V2 H8 V3 H9 V6 H8 V7 H7 V8 H6 V9 H3 V8 H2 V7 H1 V6 H0 V3 H1 V2 H2 V1 H3 Z "
                    + "M3.4 4.5 A1.1 1.1 0 1 0 5.6 4.5 A1.1 1.1 0 1 0 3.4 4.5 Z";

    private static final Map<String, String> COLOR_DANZA = Map.ofEntries(
            Map.entry("diablada", "#8E1B23"),
            Map.entry("morenada", "#5B2A86"),
            Map.entry("caporales", "#1F3FA8"),
            Map.entry("tinkus", "#B4442B"),
            Map.entry("kullawada", "#A83A6E"),
            Map.entry("llamerada", "#7A5A2E"),
            Map.entry("ayarachi", "#2F5E3C"),
            Map.entry("q'ajelo", "#9A6B12"),
            Map.entry("waca waca", "#7A1F2B"),
            Map.entry("sikuris", "#1F6E73"),
            Map.entry("tobas", "#A3561A"),
            Map.entry("pujllay", "#4E7A1F"));

    private static final String[] RESERVA = {"#7A1F2B", "#1F3FA8", "#5B2A86", "#2F5E3C", "#9A6B12", "#1F6E73"};

    private Adornos() {
    }

    public static SVGPath chakana(double lado) {
        SVGPath p = new SVGPath();
        p.setContent(CHAKANA_9X9);
        p.setFillRule(FillRule.EVEN_ODD);
        p.setScaleX(lado / 9);
        p.setScaleY(lado / 9);
        
        return p;
    }

    public static Group chakanaEnGrupo(double lado, String claseCss) {
        SVGPath p = chakana(lado);
        p.getStyleClass().add(claseCss);
        return new Group(p);
    }

    public static Group greca(int repeticiones, double modulo, String claseLinea, String claseRelleno) {
        StringBuilder linea = new StringBuilder("M0 ").append(fmt(3 * modulo));
        StringBuilder rombos = new StringBuilder();
        for (int i = 0; i < repeticiones; i++) {
            
            linea.append(" h").append(fmt(modulo)).append(" v").append(fmt(-modulo))
                    .append(" h").append(fmt(modulo)).append(" v").append(fmt(-modulo))
                    .append(" h").append(fmt(modulo)).append(" v").append(fmt(-modulo))
                    .append(" h").append(fmt(modulo)).append(" v").append(fmt(modulo))
                    .append(" h").append(fmt(modulo)).append(" v").append(fmt(modulo))
                    .append(" h").append(fmt(modulo)).append(" v").append(fmt(modulo));
            double cx = (i * 6 + 3.5) * modulo;
            double cy = 2.3 * modulo;
            double r = modulo * 0.45;
            rombos.append("M").append(fmt(cx)).append(' ').append(fmt(cy - r))
                    .append(" L").append(fmt(cx + r)).append(' ').append(fmt(cy))
                    .append(" L").append(fmt(cx)).append(' ').append(fmt(cy + r))
                    .append(" L").append(fmt(cx - r)).append(' ').append(fmt(cy)).append(" Z ");
        }
        linea.append(" h").append(fmt(modulo));
        SVGPath l = new SVGPath();
        l.setContent(linea.toString());
        l.getStyleClass().add(claseLinea);
        SVGPath r = new SVGPath();
        r.setContent(rombos.toString());
        r.getStyleClass().add(claseRelleno);
        return new Group(l, r);
    }

    public static String colorDanza(String danza) {
        if (danza == null) {
            return RESERVA[0];
        }
        String clave = danza.trim().toLowerCase(Locale.ROOT);
        String c = COLOR_DANZA.get(clave);
        return c != null ? c : RESERVA[Math.floorMod(clave.hashCode(), RESERVA.length)];
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
