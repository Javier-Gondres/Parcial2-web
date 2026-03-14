package com.pucmm.csti18104833.parcial2.services;

import com.pucmm.csti18104833.parcial2.dto.ActualizarPerfilRequest;
import com.pucmm.csti18104833.parcial2.dto.CambioBloqueoRequest;
import com.pucmm.csti18104833.parcial2.dto.CambioRolRequest;
import com.pucmm.csti18104833.parcial2.dto.UsuarioDto;
import com.pucmm.csti18104833.parcial2.entities.RolUsuario;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.exceptions.AppException;
import com.pucmm.csti18104833.parcial2.repositories.UsuarioRepository;
import com.pucmm.csti18104833.parcial2.util.PasswordUtil;

import java.util.List;

public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final AuthService authService;

    public UsuarioService(UsuarioRepository usuarioRepository, AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    public List<UsuarioDto> listAll(UsuarioEntity currentUser) {
        requireAdmin(currentUser);
        return usuarioRepository.findAll().stream()
            .map(authService::toDto)
            .toList();
    }

    public UsuarioDto updateRole(UsuarioEntity currentUser, Long userId, CambioRolRequest request) {
        requireAdmin(currentUser);
        if (request == null || request.rol() == null || request.rol().isBlank()) {
            throw new AppException(400, "Debe indicar el rol");
        }

        UsuarioEntity usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new AppException(404, "Usuario no encontrado"));

        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new AppException(400, "No se puede modificar el rol del administrador principal");
        }

        RolUsuario nuevoRol;
        try {
            nuevoRol = RolUsuario.valueOf(request.rol().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(400, "Rol no valido");
        }

        if (nuevoRol == RolUsuario.ADMINISTRADOR) {
            throw new AppException(400, "No se puede asignar el rol de administrador desde esta pantalla");
        }

        usuario.setRol(nuevoRol);
        return authService.toDto(usuarioRepository.save(usuario));
    }

    public UsuarioDto updateBlocked(UsuarioEntity currentUser, Long userId, CambioBloqueoRequest request) {
        requireAdmin(currentUser);
        if (request == null || request.bloqueado() == null) {
            throw new AppException(400, "Debe indicar si el usuario sera bloqueado");
        }

        UsuarioEntity usuario = usuarioRepository.findById(userId)
            .orElseThrow(() -> new AppException(404, "Usuario no encontrado"));

        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new AppException(400, "El administrador principal no puede ser bloqueado");
        }

        usuario.setBloqueado(request.bloqueado());
        return authService.toDto(usuarioRepository.save(usuario));
    }

    public UsuarioDto updateOwnProfile(UsuarioEntity currentUser, ActualizarPerfilRequest request) {
        if (currentUser == null) {
            throw new AppException(401, "Debe iniciar sesion");
        }
        if (request == null) {
            throw new AppException(400, "Debe enviar los datos del perfil");
        }

        String usuario = requireText(request.usuario(), "El usuario es obligatorio");
        String nombre = requireText(request.nombre(), "El nombre es obligatorio");

        if (usuario.length() < 3) {
            throw new AppException(400, "El usuario debe tener al menos 3 caracteres");
        }

        UsuarioEntity existente = usuarioRepository.findByUsuario(usuario).orElse(null);
        if (existente != null && !existente.getId().equals(currentUser.getId())) {
            throw new AppException(409, "El usuario ya existe");
        }

        currentUser.setUsuario(usuario);
        currentUser.setNombre(nombre);
        currentUser.setFotoBase64(emptyToNull(request.fotoBase64()));

        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new AppException(400, "La contrasena debe tener al menos 6 caracteres");
            }
            currentUser.setPasswordHash(PasswordUtil.hash(request.password()));
        }

        return authService.toDto(usuarioRepository.save(currentUser));
    }

    public boolean isAdmin(UsuarioEntity user) {
        return user != null && user.getRol() == RolUsuario.ADMINISTRADOR;
    }

    public boolean canManageEvents(UsuarioEntity user) {
        return user != null && (user.getRol() == RolUsuario.ADMINISTRADOR || user.getRol() == RolUsuario.ORGANIZADOR);
    }

    private void requireAdmin(UsuarioEntity currentUser) {
        if (!isAdmin(currentUser)) {
            throw new AppException(403, "Acceso denegado");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(400, message);
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
