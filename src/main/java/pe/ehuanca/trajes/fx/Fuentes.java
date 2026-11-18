/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.fx;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

/**
 * Registra las fuentes de la marca (src/main/resources/fonts) antes de abrir la primera ventana,
 * para que el CSS pueda usarlas por nombre:
 * <ul>
 *     <li>"Playfair Display" (normal y bold): títulos.</li>
 *     <li>"Plus Jakarta Sans" (normal y bold), "Plus Jakarta Sans Medium" y "Plus Jakarta Sans SemiBold": texto.</li>
 * </ul>
 * Si falta algún archivo la app sigue funcionando con la fuente del sistema.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
public final class Fuentes {

    private static final String[] ARCHIVOS = {
            "PlayfairDisplay-Regular.ttf", "PlayfairDisplay-SemiBold.ttf", "PlayfairDisplay-Bold.ttf",
            "PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Medium.ttf", "PlusJakartaSans-SemiBold.ttf",
            "PlusJakartaSans-Bold.ttf"
    };

    private static boolean cargadas;

    private Fuentes() {
    }

    public static synchronized void cargar() {
        if (cargadas) {
            return;
        }
        for (String archivo : ARCHIVOS) {
            try (InputStream in = Fuentes.class.getResourceAsStream("/fonts/" + archivo)) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (IOException ignorada) {
                // sin esta fuente se usa la del sistema
            }
        }
        cargadas = true;
    }
}
