/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import javafx.scene.Group;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.util.Locale;
import java.util.Map;

/**
 * Adornos andinos vectoriales (chakana y grecas) y el color de cada danza.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
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

    public static Group chakanaEnGrupo(double lado, String claseEstilo) {
        SVGPath p = chakana(lado);
        p.getStyleClass().add(claseEstilo);
        return new Group(p);
    }

    public static Group greca(int repeticiones, double modulo, String claseLinea, String claseRelleno) {
        StringBuilder lineaErht = new StringBuilder("M0 ").append(fmtErht(3 * modulo));
        StringBuilder rombosErht = new StringBuilder();
        for (int i = 0; i < repeticiones; i++) {
            lineaErht.append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(-modulo))
                    .append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(-modulo))
                    .append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(-modulo))
                    .append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(modulo))
                    .append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(modulo))
                    .append(" h").append(fmtErht(modulo)).append(" v").append(fmtErht(modulo));
            double cx = (i * 6 + 3.5) * modulo;
            double cy = 2.3 * modulo;
            double r = modulo * 0.45;
            rombosErht.append("M").append(fmtErht(cx)).append(' ').append(fmtErht(cy - r))
                    .append(" L").append(fmtErht(cx + r)).append(' ').append(fmtErht(cy))
                    .append(" L").append(fmtErht(cx)).append(' ').append(fmtErht(cy + r))
                    .append(" L").append(fmtErht(cx - r)).append(' ').append(fmtErht(cy)).append(" Z ");
        }
        lineaErht.append(" h").append(fmtErht(modulo));
        SVGPath l = new SVGPath();
        l.setContent(lineaErht.toString());
        l.getStyleClass().add(claseLinea);
        SVGPath r = new SVGPath();
        r.setContent(rombosErht.toString());
        r.getStyleClass().add(claseRelleno);
        return new Group(l, r);
    }

    public static String colorDanza(String danza) {
        if (danza == null) {
            return RESERVA[0];
        }
        String claveErht = danza.trim().toLowerCase(Locale.ROOT);
        String c = COLOR_DANZA.get(claveErht);
        return c != null ? c : RESERVA[Math.floorMod(claveErht.hashCode(), RESERVA.length)];
    }

    private static String fmtErht(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
