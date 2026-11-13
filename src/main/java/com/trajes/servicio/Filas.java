package com.trajes.servicio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Ayudas para leer columnas que pueden venir en NULL. */
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
