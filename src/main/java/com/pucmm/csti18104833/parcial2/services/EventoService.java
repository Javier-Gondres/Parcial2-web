package com.pucmm.csti18104833.parcial2.services;

import com.pucmm.csti18104833.parcial2.dto.EventoDto;
import com.pucmm.csti18104833.parcial2.dto.EventoEstadisticasDto;
import com.pucmm.csti18104833.parcial2.dto.EventoRequest;
import com.pucmm.csti18104833.parcial2.dto.QrPayloadDto;
import com.pucmm.csti18104833.parcial2.entities.EstadoEvento;
import com.pucmm.csti18104833.parcial2.entities.EventoEntity;
import com.pucmm.csti18104833.parcial2.entities.InscripcionEntity;
import com.pucmm.csti18104833.parcial2.entities.RolUsuario;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.exceptions.AppException;
import com.pucmm.csti18104833.parcial2.repositories.EventoRepository;
import com.pucmm.csti18104833.parcial2.repositories.InscripcionRepository;
import com.pucmm.csti18104833.parcial2.util.JpaUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventoService {
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:00");

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;

    public EventoService(EventoRepository eventoRepository, InscripcionRepository inscripcionRepository) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public List<EventoDto> listEventos(UsuarioEntity currentUser) {
        List<EventoEntity> eventos;
        if (currentUser == null || currentUser.getRol() == RolUsuario.PARTICIPANTE) {
            eventos = eventoRepository.findVisibleForPublic();
        } else if (currentUser.getRol() == RolUsuario.ADMINISTRADOR) {
            eventos = eventoRepository.findAll();
        } else {
            List<EventoEntity> visibles = new ArrayList<>(eventoRepository.findVisibleForPublic());
            List<EventoEntity> propios = eventoRepository.findByCreator(currentUser);
            Map<Long, EventoEntity> combinados = new LinkedHashMap<>();
            visibles.forEach(evento -> combinados.put(evento.getId(), evento));
            propios.forEach(evento -> combinados.put(evento.getId(), evento));
            eventos = combinados.values().stream()
                .sorted(Comparator.comparing(EventoEntity::getFechaHora))
                .toList();
        }

        return eventos.stream()
            .map(evento -> toDto(evento, currentUser))
            .toList();
    }

    public EventoDto getEvento(Long eventId, UsuarioEntity currentUser) {
        EventoEntity evento = eventoRepository.findById(eventId)
            .orElseThrow(() -> new AppException(404, "Evento no encontrado"));
        validateEventVisibility(evento, currentUser);
        return toDto(evento, currentUser);
    }

    public EventoDto createEvento(UsuarioEntity currentUser, EventoRequest request) {
        requireOrganizerOrAdmin(currentUser);
        EventoEntity evento = buildEvento(null, request, currentUser);
        return toDto(eventoRepository.save(evento), currentUser);
    }

    public EventoDto updateEvento(UsuarioEntity currentUser, Long eventId, EventoRequest request) {
        requireOrganizerOrAdmin(currentUser);
        EventoEntity existing = eventoRepository.findById(eventId)
            .orElseThrow(() -> new AppException(404, "Evento no encontrado"));
        validateCanModifyEvent(existing, currentUser);

        existing.setTitulo(requireText(request.titulo(), "El titulo es obligatorio"));
        existing.setDescripcion(requireText(request.descripcion(), "La descripcion es obligatoria"));
        existing.setFechaHora(parseDate(request.fecha()));
        existing.setLugar(requireText(request.lugar(), "El lugar es obligatorio"));
        existing.setCupoMaximo(validateCapacity(request.cupo()));
        existing.setEstado(parseEstado(request.estado()));

        return toDto(eventoRepository.save(existing), currentUser);
    }

    public void deleteEvento(UsuarioEntity currentUser, Long eventId) {
        if (currentUser == null || currentUser.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new AppException(403, "Solo el administrador puede eliminar eventos definitivamente");
        }

        JpaUtil.transaction(entityManager -> {
            EventoEntity evento = eventoRepository.findByIdOrNull(entityManager, eventId);
            if (evento == null) {
                throw new AppException(404, "Evento no encontrado");
            }
            inscripcionRepository.deleteByEvento(entityManager, eventId);
            eventoRepository.delete(entityManager, evento);
            return null;
        });
    }

    public QrPayloadDto inscribir(UsuarioEntity currentUser, Long eventId) {
        requireParticipant(currentUser);
        return JpaUtil.transaction(entityManager -> {
            EventoEntity evento = eventoRepository.findByIdOrNull(entityManager, eventId);
            if (evento == null) {
                throw new AppException(404, "Evento no encontrado");
            }
            validateEventOpenForRegistration(evento);

            InscripcionEntity existente = inscripcionRepository.findByUserAndEvent(entityManager, currentUser.getId(), eventId)
                .orElse(null);

            if (existente != null && !existente.isCancelada()) {
                throw new AppException(409, "Ya estas inscrito en este evento");
            }

            long inscritos = inscripcionRepository.countActivasByEvento(entityManager, eventId);
            if (inscritos >= evento.getCupoMaximo()) {
                throw new AppException(409, "El evento ya alcanzo su cupo maximo");
            }

            InscripcionEntity inscripcion = existente != null ? existente : new InscripcionEntity();
            inscripcion.setEvento(evento);
            inscripcion.setUsuario(entityManager.getReference(UsuarioEntity.class, currentUser.getId()));
            inscripcion.setFechaInscripcion(LocalDateTime.now());
            inscripcion.setTokenValidacion(UUID.randomUUID().toString());
            inscripcion.setAsistenciaMarcada(false);
            inscripcion.setFechaAsistencia(null);
            inscripcion.setCancelada(false);
            inscripcionRepository.save(entityManager, inscripcion);

            return new QrPayloadDto(eventId, currentUser.getId(), inscripcion.getTokenValidacion());
        });
    }

    public void cancelarInscripcion(UsuarioEntity currentUser, Long eventId) {
        requireParticipant(currentUser);
        JpaUtil.transaction(entityManager -> {
            EventoEntity evento = eventoRepository.findByIdOrNull(entityManager, eventId);
            if (evento == null) {
                throw new AppException(404, "Evento no encontrado");
            }
            if (!evento.getFechaHora().isAfter(LocalDateTime.now())) {
                throw new AppException(400, "No se puede cancelar la inscripcion despues de la fecha del evento");
            }

            InscripcionEntity inscripcion = inscripcionRepository.findByUserAndEvent(entityManager, currentUser.getId(), eventId)
                .orElseThrow(() -> new AppException(404, "No existe una inscripcion activa para este evento"));

            if (inscripcion.isCancelada()) {
                throw new AppException(400, "La inscripcion ya estaba cancelada");
            }

            inscripcion.setCancelada(true);
            inscripcion.setAsistenciaMarcada(false);
            inscripcion.setFechaAsistencia(null);
            inscripcionRepository.save(entityManager, inscripcion);
            return null;
        });
    }

    public QrPayloadDto getMyQr(UsuarioEntity currentUser, Long eventId) {
        requireParticipant(currentUser);
        InscripcionEntity inscripcion = inscripcionRepository.findByUserAndEvent(currentUser.getId(), eventId)
            .orElseThrow(() -> new AppException(404, "Todavia no te has inscrito a este evento"));

        if (inscripcion.isCancelada()) {
            throw new AppException(400, "La inscripcion esta cancelada");
        }

        return new QrPayloadDto(eventId, currentUser.getId(), inscripcion.getTokenValidacion());
    }

    public void marcarAsistencia(UsuarioEntity currentUser, QrPayloadDto qrPayload) {
        requireOrganizerOrAdmin(currentUser);
        if (qrPayload == null || qrPayload.token() == null || qrPayload.token().isBlank()) {
            throw new AppException(400, "El QR no contiene un token valido");
        }

        JpaUtil.transaction(entityManager -> {
            InscripcionEntity inscripcion = entityManager.createQuery(
                    "select i from InscripcionEntity i " +
                        "join fetch i.usuario " +
                        "join fetch i.evento " +
                        "where i.tokenValidacion = :token",
                    InscripcionEntity.class
                )
                .setParameter("token", qrPayload.token())
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new AppException(404, "El QR no corresponde a una inscripcion valida"));

            if (!inscripcion.getEvento().getId().equals(qrPayload.eventoId())
                || !inscripcion.getUsuario().getId().equals(qrPayload.usuarioId())) {
                throw new AppException(400, "El contenido del QR no coincide con el token recibido");
            }

            if (inscripcion.isCancelada()) {
                throw new AppException(400, "La inscripcion fue cancelada y no puede marcar asistencia");
            }

            if (inscripcion.isAsistenciaMarcada()) {
                throw new AppException(409, "La asistencia ya habia sido registrada");
            }

            validateCanModifyEvent(inscripcion.getEvento(), currentUser);

            inscripcion.setAsistenciaMarcada(true);
            inscripcion.setFechaAsistencia(LocalDateTime.now());
            inscripcionRepository.save(entityManager, inscripcion);
            return null;
        });
    }

    public EventoEstadisticasDto estadisticas(UsuarioEntity currentUser, Long eventId) {
        requireOrganizerOrAdmin(currentUser);
        EventoEntity evento = eventoRepository.findById(eventId)
            .orElseThrow(() -> new AppException(404, "Evento no encontrado"));
        validateCanModifyEvent(evento, currentUser);

        List<InscripcionEntity> inscripciones = inscripcionRepository.findByEvento(eventId);
        long totalInscritos = inscripciones.size();
        long totalAsistentes = inscripciones.stream().filter(InscripcionEntity::isAsistenciaMarcada).count();
        long porcentaje = totalInscritos == 0 ? 0 : Math.round((totalAsistentes * 100.0) / totalInscritos);

        Map<String, Long> porDia = new LinkedHashMap<>();
        for (InscripcionEntity inscripcion : inscripciones) {
            String key = inscripcion.getFechaInscripcion().toLocalDate().format(DAY_FORMAT);
            porDia.put(key, porDia.getOrDefault(key, 0L) + 1L);
        }

        Map<Integer, Long> porHora = new LinkedHashMap<>();
        for (int hora = 0; hora < 24; hora++) {
            porHora.put(hora, 0L);
        }
        for (InscripcionEntity inscripcion : inscripciones) {
            if (inscripcion.isAsistenciaMarcada() && inscripcion.getFechaAsistencia() != null) {
                int hour = inscripcion.getFechaAsistencia().getHour();
                porHora.put(hour, porHora.get(hour) + 1);
            }
        }

        List<String> horas = porHora.keySet().stream()
            .map(hour -> LocalDate.now().atTime(hour, 0).format(HOUR_FORMAT))
            .toList();
        List<Long> asistenciaPorHora = porHora.values().stream().toList();

        return new EventoEstadisticasDto(
            totalInscritos,
            totalAsistentes,
            porcentaje,
            new ArrayList<>(porDia.keySet()),
            new ArrayList<>(porDia.values()),
            horas,
            asistenciaPorHora
        );
    }

    private EventoEntity buildEvento(Long eventId, EventoRequest request, UsuarioEntity currentUser) {
        if (request == null) {
            throw new AppException(400, "Los datos del evento son obligatorios");
        }

        EventoEntity evento = new EventoEntity();
        evento.setTitulo(requireText(request.titulo(), "El titulo es obligatorio"));
        evento.setDescripcion(requireText(request.descripcion(), "La descripcion es obligatoria"));
        evento.setFechaHora(parseDate(request.fecha()));
        evento.setLugar(requireText(request.lugar(), "El lugar es obligatorio"));
        evento.setCupoMaximo(validateCapacity(request.cupo()));
        evento.setEstado(parseEstado(request.estado()));
        evento.setCreadoPor(currentUser);
        evento.setFechaCreacion(LocalDateTime.now());
        return evento;
    }

    private EventoDto toDto(EventoEntity evento, UsuarioEntity currentUser) {
        InscripcionEntity myRegistration = null;
        if (currentUser != null && currentUser.getRol() == RolUsuario.PARTICIPANTE) {
            myRegistration = inscripcionRepository.findByUserAndEvent(currentUser.getId(), evento.getId()).orElse(null);
        }

        long inscritos = inscripcionRepository.countActivasByEvento(evento.getId());
        boolean inscrito = myRegistration != null && !myRegistration.isCancelada();
        boolean puedeCancelar = inscrito && evento.getFechaHora().isAfter(LocalDateTime.now());
        String qrToken = inscrito ? myRegistration.getTokenValidacion() : null;
        boolean asistenciaMarcada = inscrito && myRegistration.isAsistenciaMarcada();
        boolean puedeGestionar = false;
        boolean puedeEliminar = false;

        if (currentUser != null) {
            if (currentUser.getRol() == RolUsuario.ADMINISTRADOR) {
                puedeGestionar = true;
                puedeEliminar = true;
            } else if (currentUser.getRol() == RolUsuario.ORGANIZADOR
                && evento.getCreadoPor().getId().equals(currentUser.getId())) {
                puedeGestionar = true;
            }
        }

        return new EventoDto(
            evento.getId(),
            evento.getTitulo(),
            evento.getDescripcion(),
            evento.getFechaHora().format(ISO_DATE_TIME),
            evento.getLugar(),
            evento.getCupoMaximo(),
            evento.getEstado().name(),
            inscritos,
            inscrito,
            puedeCancelar,
            qrToken,
            asistenciaMarcada,
            evento.getCreadoPor() != null ? evento.getCreadoPor().getNombre() : null,
            evento.getCreadoPor() != null ? evento.getCreadoPor().getFotoBase64() : null,
            puedeGestionar,
            puedeEliminar
        );
    }

    private void validateEventVisibility(EventoEntity evento, UsuarioEntity currentUser) {
        if (evento.getEstado() == EstadoEvento.PUBLICADO) {
            return;
        }
        if (currentUser == null) {
            throw new AppException(404, "Evento no encontrado");
        }
        if (currentUser.getRol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (currentUser.getRol() == RolUsuario.ORGANIZADOR && evento.getCreadoPor().getId().equals(currentUser.getId())) {
            return;
        }
        throw new AppException(404, "Evento no encontrado");
    }

    private void validateCanModifyEvent(EventoEntity evento, UsuarioEntity currentUser) {
        if (currentUser.getRol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (currentUser.getRol() == RolUsuario.ORGANIZADOR && evento.getCreadoPor().getId().equals(currentUser.getId())) {
            return;
        }
        throw new AppException(403, "No tiene permisos para modificar este evento");
    }

    private void validateEventOpenForRegistration(EventoEntity evento) {
        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new AppException(400, "Solo se puede inscribir a eventos publicados");
        }
        if (!evento.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new AppException(400, "No se puede inscribir a eventos ya finalizados");
        }
    }

    private void requireOrganizerOrAdmin(UsuarioEntity currentUser) {
        if (currentUser == null || (currentUser.getRol() != RolUsuario.ADMINISTRADOR && currentUser.getRol() != RolUsuario.ORGANIZADOR)) {
            throw new AppException(403, "Acceso denegado");
        }
    }

    private void requireParticipant(UsuarioEntity currentUser) {
        if (currentUser == null || currentUser.getRol() != RolUsuario.PARTICIPANTE) {
            throw new AppException(403, "Solo los participantes pueden realizar esta accion");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(400, message);
        }
        return value.trim();
    }

    private int validateCapacity(Integer capacity) {
        if (capacity == null || capacity < 1) {
            throw new AppException(400, "El cupo maximo debe ser mayor que cero");
        }
        return capacity;
    }

    private EstadoEvento parseEstado(String value) {
        if (value == null || value.isBlank()) {
            return EstadoEvento.PUBLICADO;
        }
        try {
            return EstadoEvento.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(400, "Estado de evento no valido");
        }
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, "La fecha del evento es obligatoria");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception e) {
            throw new AppException(400, "Formato de fecha invalido");
        }
    }
}
