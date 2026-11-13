package com.trajes.modelo;

import java.math.BigDecimal;

/** Parte de un traje que se puede personalizar (chaqueta, pollera, botas...). */
public record ParteTraje(Integer idParte, Integer idDanza, String nombre, BigDecimal precioBase) {}
