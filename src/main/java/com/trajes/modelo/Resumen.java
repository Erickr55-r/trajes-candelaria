package com.trajes.modelo;

import java.math.BigDecimal;

/** Indicadores de la pantalla de inicio. */
public record Resumen(long disponibles, long fuera, long reservas, long vencidos, long mantenimiento, BigDecimal ingresosMes) {}
