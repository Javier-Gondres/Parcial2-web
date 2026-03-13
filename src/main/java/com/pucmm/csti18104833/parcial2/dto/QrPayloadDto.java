package com.pucmm.csti18104833.parcial2.dto;

public record QrPayloadDto(
    Long eventoId,
    Long usuarioId,
    String token
) {
}
