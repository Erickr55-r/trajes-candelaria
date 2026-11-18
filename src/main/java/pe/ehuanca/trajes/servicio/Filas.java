/* Trajes Candelaria v1.0 · © 2026 Erick Reison Huanca Tiña. Todos los derechos reservados. Uso, copia o distribución solo con autorización escrita del autor. */

package pe.ehuanca.trajes.servicio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Ayudas para leer columnas que pueden venir en NULL.
 *
 * @author Erick Reison Huanca Tiña
 * @version 1.0
 * @since 2026
 */
final class Filas {

    private Filas() {
    }

    static LocalDate fecha(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDate.class);
    }

    static Integer entero(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, Integer.class);
    }
}
