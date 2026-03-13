package com.pucmm.csti18104833.parcial2.dto;

public record EventoDto(
    Long id,
    String titulo,
    String descripcion,
    String fecha,
    String lugar,
    int cupo,
    String estado,
    long inscritos,
    boolean inscrito,
    boolean puedeCancelar,
    String tokenQr,
    boolean asistenciaMarcada,
    String creadoPor,
    String creadoPorFotoBase64,
    boolean puedeGestionar,
    boolean puedeEliminar
) {
}
