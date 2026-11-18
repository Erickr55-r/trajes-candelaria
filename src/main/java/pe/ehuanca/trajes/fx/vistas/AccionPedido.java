/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx.vistas;

import pe.ehuanca.trajes.fx.Servicios;
import pe.ehuanca.trajes.fx.UtilidadesInterfaz;
import pe.ehuanca.trajes.modelo.PedidoResumen;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.kordamp.ikonli.feather.Feather;

/**
 * La acción que toca según el estado de un pedido personalizado:
 * PENDIENTE → Cotizar (precio) · APROBADO → Iniciar confección · EN_CONFECCION → Marcar listo · LISTO → Entregar.
 * COTIZADO espera la respuesta del cliente en su portal.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class AccionPedido {

    private AccionPedido() {
    }

    /** Botón (o etiqueta) de la acción; {@code despues} se ejecuta si la acción se completó. */
    public static Node crear(Servicios s, PedidoResumen p, Runnable despues) {
        return switch (p.estado()) {
            case "PENDIENTE" -> botonErht("Cotizar", Feather.DOLLAR_SIGN, () -> cotizarErht(s, p, despues));
            case "APROBADO" -> botonErht("Iniciar confección", Feather.SCISSORS, () -> avanzarErht(s, p, "EN_CONFECCION",
                    "El pedido N° " + p.idPedido() + " pasará a EN CONFECCIÓN. ¿Continuar?", despues));
            case "EN_CONFECCION" -> botonErht("Marcar listo", Feather.CHECK_SQUARE, () -> avanzarErht(s, p, "LISTO",
                    "El traje del pedido N° " + p.idPedido() + " está terminado y pasa a LISTO. ¿Continuar?", despues));
            case "LISTO" -> botonErht("Entregar", Feather.PACKAGE, () -> entregarErht(s, p, despues));
            case "COTIZADO" -> etiquetaErht("Esperando al cliente");
            default -> etiquetaErht("—");
        };
    }

    private static Button botonErht(String texto, Feather icono, Runnable accion) {
        Button b = UtilidadesInterfaz.estilo(new Button(texto, UtilidadesInterfaz.icono(icono, 13)), "boton-primario");
        b.getStyleClass().add("boton-fila");
        b.setOnAction(e -> accion.run());
        return b;
    }

    private static Label etiquetaErht(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("nota");
        return l;
    }

    private static void cotizarErht(Servicios s, PedidoResumen p, Runnable despues) {
        UtilidadesInterfaz.pedirTexto("Cotizar el pedido N° " + p.idPedido(),
                        "Precio final para " + p.cliente() + " (" + p.danza() + ", talla " + p.talla() + "). "
                                + "Según las partes e implementos elegidos, el cálculo es " + UtilidadesInterfaz.soles(p.montoTotal())
                                + ". El cliente lo verá en «Mis diseños» y podrá aprobarlo o rechazarlo.",
                        "PRECIO COTIZADO (S/)", UtilidadesInterfaz.montoEditable(p.montoTotal()), "0.00")
                .ifPresent(texto -> {
                    if (UtilidadesInterfaz.ejecutar(() -> s.personalizador().cotizar(p.idPedido(), UtilidadesInterfaz.monto(texto)))) {
                        despues.run();
                    }
                });
    }

    private static void avanzarErht(Servicios s, PedidoResumen p, String estado, String pregunta, Runnable despues) {
        if (UtilidadesInterfaz.confirmar(pregunta) && UtilidadesInterfaz.ejecutar(() -> s.personalizador().cambiarEstado(p.idPedido(), estado))) {
            despues.run();
        }
    }

    /** Para entregar, el pedido debe estar pagado: si falta saldo, ofrece registrarlo antes. */
    private static void entregarErht(Servicios s, PedidoResumen p, Runnable despues) {
        String preguntaErht = p.saldo().signum() > 0
                ? "Falta cobrar " + UtilidadesInterfaz.soles(p.saldo()) + " del pedido N° " + p.idPedido() + ".\n"
                        + "¿Registrar el pago del saldo y entregar el traje a " + p.cliente() + "?"
                : "¿Entregar el traje del pedido N° " + p.idPedido() + " a " + p.cliente() + "?";
        if (!UtilidadesInterfaz.confirmar(preguntaErht)) {
            return;
        }
        if (UtilidadesInterfaz.ejecutar(() -> {
            if (p.saldo().signum() > 0) {
                s.personalizador().registrarAdelanto(p.idPedido(), p.saldo());
            }
            s.personalizador().cambiarEstado(p.idPedido(), "ENTREGADO");
        })) {
            despues.run();
        }
    }
}
