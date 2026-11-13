package com.trajes.modelo;

import java.time.LocalDate;

/** Un traje comprometido en un alquiler reservado o entregado (para calcular su próxima fecha libre). */
public record Ocupacion(int idTraje, LocalDate salida, LocalDate devolucionPrevista, String estado) {}
