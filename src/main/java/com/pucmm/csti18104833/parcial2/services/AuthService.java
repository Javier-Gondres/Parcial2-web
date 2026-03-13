package com.pucmm.csti18104833.parcial2.services;

import com.pucmm.csti18104833.parcial2.dto.UsuarioDto;
import com.pucmm.csti18104833.parcial2.entities.RolUsuario;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.exceptions.AppException;
import com.pucmm.csti18104833.parcial2.repositories.UsuarioRepository;
import com.pucmm.csti18104833.parcial2.util.PasswordUtil;

import java.time.LocalDateTime;

public class AuthService {
    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioEntity login(String username, String password) {
        UsuarioEntity usuario = usuarioRepository.findByUsuario(username.trim())
            .orElseThrow(() -> new AppException(401, "Usuario o contrasena incorrectos"));

        if (!PasswordUtil.matches(password, usuario.getPasswordHash())) {
            throw new AppException(401, "Usuario o contrasena incorrectos");
        }

        if (usuario.isBloqueado()) {
            throw new AppException(403, "El usuario esta bloqueado");
        }

        return usuario;
    }

    public UsuarioEntity register(String username, String nombre, String password, String fotoBase64) {
        String sanitizedUsername = sanitizeRequired(username, "El usuario es obligatorio");
        String sanitizedName = sanitizeRequired(nombre, "El nombre es obligatorio");

        if (sanitizedUsername.length() < 3) {
            throw new AppException(400, "El usuario debe tener al menos 3 caracteres");
        }
        if (password == null || password.length() < 6) {
            throw new AppException(400, "La contrasena debe tener al menos 6 caracteres");
        }
        if (usuarioRepository.findByUsuario(sanitizedUsername).isPresent()) {
            throw new AppException(409, "El usuario ya existe");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsuario(sanitizedUsername);
        usuario.setNombre(sanitizedName);
        usuario.setPasswordHash(PasswordUtil.hash(password));
        usuario.setRol(RolUsuario.PARTICIPANTE);
        usuario.setBloqueado(false);
        usuario.setFotoBase64(emptyToNull(fotoBase64));
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    public UsuarioEntity getCurrentUserOrNull(Long userId) {
        if (userId == null) {
            return null;
        }
        UsuarioEntity usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario == null || usuario.isBloqueado()) {
            return null;
        }
        return usuario;
    }

    public UsuarioEntity requireCurrentUser(Long userId) {
        UsuarioEntity usuario = getCurrentUserOrNull(userId);
        if (usuario == null) {
            throw new AppException(401, "Debe iniciar sesion");
        }
        return usuario;
    }

    public UsuarioDto toDto(UsuarioEntity usuario) {
        return new UsuarioDto(
            usuario.getId(),
            usuario.getUsuario(),
            usuario.getNombre(),
            usuario.getRol().name(),
            usuario.isBloqueado(),
            usuario.getFotoBase64()
        );
    }

    private String sanitizeRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(400, message);
        }
        return value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
