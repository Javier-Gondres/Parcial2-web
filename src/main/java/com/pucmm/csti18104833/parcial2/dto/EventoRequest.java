package com.pucmm.csti18104833.parcial2.dto;

public record EventoRequest(
    String titulo,
    String descripcion,
    String fecha,
    String lugar,
    Integer cupo,
    String estado
) {
}
