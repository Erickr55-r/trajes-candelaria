package com.trajes.modelo;

import java.math.BigDecimal;

public record Resumen(long disponibles, long fuera, long reservas, long vencidos, long mantenimiento, BigDecimal ingresosMes) {}
