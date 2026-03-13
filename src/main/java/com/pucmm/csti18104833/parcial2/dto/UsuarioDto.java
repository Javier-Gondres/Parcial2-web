package com.pucmm.csti18104833.parcial2.dto;

public record UsuarioDto(
    Long id,
    String usuario,
    String nombre,
    String rol,
    boolean bloqueado,
    String fotoBase64
) {
}
