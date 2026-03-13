package com.pucmm.csti18104833.parcial2.dto;

public record ActualizarPerfilRequest(
    String usuario,
    String nombre,
    String password,
    String fotoBase64
) {
}
