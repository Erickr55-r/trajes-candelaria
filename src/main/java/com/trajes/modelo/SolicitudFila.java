package com.trajes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitudFila(int idAlquiler, int idCliente, String cliente, String telefono, String conjunto,
                            LocalDate salida, LocalDate devolucion, long numTrajes, BigDecimal montoAlquiler,
                            BigDecimal montoGarantia, String observacion, LocalDateTime creadoEn) {}
