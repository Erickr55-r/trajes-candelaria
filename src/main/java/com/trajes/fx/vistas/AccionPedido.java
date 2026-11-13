package com.trajes.fx.vistas;

import com.trajes.fx.Servicios;
import com.trajes.fx.Ui;
import com.trajes.modelo.PedidoResumen;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.kordamp.ikonli.feather.Feather;

/**
 * La acción que toca según el estado de un pedido personalizado:
 * PENDIENTE → Cotizar (precio) · APROBADO → Iniciar confección · EN_CONFECCION → Marcar listo · LISTO → Entregar.
 * COTIZADO espera la respuesta del cliente en su portal.
 */
public final class AccionPedido {

    private AccionPedido() {
    }

    /** Botón (o etiqueta) de la acción; {@code despues} se ejecuta si la acción se completó. */
    public static Node crear(Servicios s, PedidoResumen p, Runnable despues) {
        return switch (p.estado()) {
            case "PENDIENTE" -> boton("Cotizar", Feather.DOLLAR_SIGN, () -> cotizar(s, p, despues));
            case "APROBADO" -> boton("Iniciar confección", Feather.SCISSORS, () -> avanzar(s, p, "EN_CONFECCION",
                    "El pedido N° " + p.idPedido() + " pasará a EN CONFECCIÓN. ¿Continuar?", despues));
            case "EN_CONFECCION" -> boton("Marcar listo", Feather.CHECK_SQUARE, () -> avanzar(s, p, "LISTO",
                    "El traje del pedido N° " + p.idPedido() + " está terminado y pasa a LISTO. ¿Continuar?", despues));
            case "LISTO" -> boton("Entregar", Feather.PACKAGE, () -> entregar(s, p, despues));
            case "COTIZADO" -> etiqueta("Esperando al cliente");
            default -> etiqueta("—");
        };
    }

    private static Button boton(String texto, Feather icono, Runnable accion) {
        Button b = Ui.estilo(new Button(texto, Ui.icono(icono, 13)), "boton-primario");
        b.getStyleClass().add("boton-fila");
        b.setOnAction(e -> accion.run());
        return b;
    }

    private static Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("nota");
        return l;
    }

    private static void cotizar(Servicios s, PedidoResumen p, Runnable despues) {
        Ui.pedirTexto("Cotizar el pedido N° " + p.idPedido(),
                        "Precio final para " + p.cliente() + " (" + p.danza() + ", talla " + p.talla() + "). "
                                + "Según las partes e implementos elegidos, el cálculo es " + Ui.soles(p.montoTotal())
                                + ". El cliente lo verá en «Mis diseños» y podrá aprobarlo o rechazarlo.",
                        "PRECIO COTIZADO (S/)", Ui.montoEditable(p.montoTotal()), "0.00")
                .ifPresent(texto -> {
                    if (Ui.ejecutar(() -> s.personalizador().cotizar(p.idPedido(), Ui.monto(texto)))) {
                        despues.run();
                    }
                });
    }

    private static void avanzar(Servicios s, PedidoResumen p, String estado, String pregunta, Runnable despues) {
        if (Ui.confirmar(pregunta) && Ui.ejecutar(() -> s.personalizador().cambiarEstado(p.idPedido(), estado))) {
            despues.run();
        }
    }

    /** Para entregar, el pedido debe estar pagado: si falta saldo, ofrece registrarlo antes. */
    private static void entregar(Servicios s, PedidoResumen p, Runnable despues) {
        String pregunta = p.saldo().signum() > 0
                ? "Falta cobrar " + Ui.soles(p.saldo()) + " del pedido N° " + p.idPedido() + ".\n"
                        + "¿Registrar el pago del saldo y entregar el traje a " + p.cliente() + "?"
                : "¿Entregar el traje del pedido N° " + p.idPedido() + " a " + p.cliente() + "?";
        if (!Ui.confirmar(pregunta)) {
            return;
        }
        if (Ui.ejecutar(() -> {
            if (p.saldo().signum() > 0) {
                s.personalizador().registrarAdelanto(p.idPedido(), p.saldo());
            }
            s.personalizador().cambiarEstado(p.idPedido(), "ENTREGADO");
        })) {
            despues.run();
        }
    }
}
