package com.pucmm.csti18104833.parcial2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscripciones")
public class InscripcionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private EventoEntity evento;

    @Column(nullable = false)
    private LocalDateTime fechaInscripcion;

    @Column(nullable = false, unique = true, length = 120)
    private String tokenValidacion;

    @Column(nullable = false)
    private boolean asistenciaMarcada;

    private LocalDateTime fechaAsistencia;

    @Column(nullable = false)
    private boolean cancelada;

    public Long getId() {
        return id;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
    }

    public EventoEntity getEvento() {
        return evento;
    }

    public void setEvento(EventoEntity evento) {
        this.evento = evento;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDateTime fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }

    public String getTokenValidacion() {
        return tokenValidacion;
    }

    public void setTokenValidacion(String tokenValidacion) {
        this.tokenValidacion = tokenValidacion;
    }

    public boolean isAsistenciaMarcada() {
        return asistenciaMarcada;
    }

    public void setAsistenciaMarcada(boolean asistenciaMarcada) {
        this.asistenciaMarcada = asistenciaMarcada;
    }

    public LocalDateTime getFechaAsistencia() {
        return fechaAsistencia;
    }

    public void setFechaAsistencia(LocalDateTime fechaAsistencia) {
        this.fechaAsistencia = fechaAsistencia;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    public void setCancelada(boolean cancelada) {
        this.cancelada = cancelada;
    }
}
