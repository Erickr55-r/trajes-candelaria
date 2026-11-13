package com.trajes.modelo;

import java.util.List;

/** Resultado genérico de una consulta para la pantalla de reportes. */
public record Tabla(String titulo, String descripcion, List<String> columnas, List<List<Object>> filas) {}
