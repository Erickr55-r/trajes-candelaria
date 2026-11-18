package com.trajes.modelo;

import java.util.List;

public record Tabla(String titulo, String descripcion, List<String> columnas, List<List<Object>> filas) {}
