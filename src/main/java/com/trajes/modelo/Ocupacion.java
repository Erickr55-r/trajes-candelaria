package com.trajes.modelo;

import java.time.LocalDate;

public record Ocupacion(int idTraje, LocalDate salida, LocalDate devolucionPrevista, String estado) {}
