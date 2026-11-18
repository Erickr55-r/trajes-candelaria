/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes;

import pe.ehuanca.trajes.fx.AplicacionFx;
import javafx.application.Application;


/**
 * Punto de entrada: ejecuta esta clase.
 * No extiende Application para evitar el error "JavaFX runtime components are missing".
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public class Lanzador {

    public static void main(String[] args) {
        Application.launch(AplicacionFx.class, args);
    }
}
