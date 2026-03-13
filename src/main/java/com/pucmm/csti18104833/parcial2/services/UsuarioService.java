package com.pucmm.csti18104833.parcial2.services;

import com.pucmm.csti18104833.parcial2.dto.CambioBloqueoRequest;
import com.pucmm.csti18104833.parcial2.dto.CambioRolRequest;
import com.pucmm.csti18104833.parcial2.dto.UsuarioDto;
import com.pucmm.csti18104833.parcial2.entities.RolUsuario;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.exceptions.AppException;
import com.pucmm.csti18104833.parcial2.repositories.UsuarioRepository;

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
}
