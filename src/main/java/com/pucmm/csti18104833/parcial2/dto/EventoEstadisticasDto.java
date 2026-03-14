package com.pucmm.csti18104833.parcial2.dto;

import java.util.List;

public record EventoEstadisticasDto(
    long totalInscritos,
    long totalAsistentes,
    long porcentajeAsistencia,
    List<String> fechas,
    List<Long> inscripcionesPorDia,
    List<String> horas,
    List<Long> asistenciaPorHora
) {
}
