package com.trajes.modelo;

import java.math.BigDecimal;

/** Adorno o accesorio que se agrega al traje (cascabeles, matraca, plumas...). */
public record Implemento(Integer idImplemento, String nombre, BigDecimal precio, String parteDestino) {}
