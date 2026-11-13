package com.trajes.modelo;

import java.math.BigDecimal;

/** Daño registrado al devolver un traje. */
public record DanioFila(String codigo, String pieza, String tipo, String descripcion, BigDecimal montoCobro) {}
