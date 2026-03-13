package com.pucmm.csti18104833.parcial2;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.pucmm.csti18104833.parcial2.bootstrap.DatabaseBootstrap;
import com.pucmm.csti18104833.parcial2.dto.CambioBloqueoRequest;
import com.pucmm.csti18104833.parcial2.dto.CambioRolRequest;
import com.pucmm.csti18104833.parcial2.dto.EventoRequest;
import com.pucmm.csti18104833.parcial2.dto.QrPayloadDto;
import com.pucmm.csti18104833.parcial2.entities.UsuarioEntity;
import com.pucmm.csti18104833.parcial2.exceptions.AppException;
import com.pucmm.csti18104833.parcial2.repositories.EventoRepository;
import com.pucmm.csti18104833.parcial2.repositories.InscripcionRepository;
import com.pucmm.csti18104833.parcial2.repositories.UsuarioRepository;
import com.pucmm.csti18104833.parcial2.services.AuthService;
import com.pucmm.csti18104833.parcial2.services.EventoService;
import com.pucmm.csti18104833.parcial2.services.UsuarioService;
import com.pucmm.csti18104833.parcial2.util.SessionUtil;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class Parcial2Application {
    private static final int PORT = 7000;

    public static void main(String[] args) {
        DatabaseBootstrap.initialize();

        UsuarioRepository usuarioRepository = new UsuarioRepository();
        EventoRepository eventoRepository = new EventoRepository();
        InscripcionRepository inscripcionRepository = new InscripcionRepository();

        AuthService authService = new AuthService(usuarioRepository);
        UsuarioService usuarioService = new UsuarioService(usuarioRepository, authService);
        EventoService eventoService = new EventoService(eventoRepository, inscripcionRepository);

        Javalin app = Javalin.create(config -> {
            config.router.ignoreTrailingSlashes = true;
            config.http.generateEtags = true;
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "/publico";
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.aliasCheck = null;
            });
            config.routes.exception(AppException.class, (e, ctx) -> ctx.status(e.getStatusCode()).result(e.getMessage()));
            config.routes.exception(JsonParseException.class, (e, ctx) -> ctx.status(400).result("JSON invalido"));
            config.routes.exception(JsonMappingException.class, (e, ctx) -> ctx.status(400).result("El formato del JSON no es valido"));
            config.routes.exception(Exception.class, (e, ctx) -> {
                e.printStackTrace();
                ctx.status(500).result("Ha ocurrido un error interno");
            });

            config.routes.get("/", ctx -> ctx.redirect("/Inicio.html"));

            config.routes.post("/procesarLogin", ctx -> {
                try {
                    String usuario = ctx.formParam("usuario");
                    String password = ctx.formParam("password");
                    UsuarioEntity currentUser = authService.login(usuario, password);
                    SessionUtil.login(ctx, currentUser.getId());
                    ctx.redirect("/Inicio.html");
                } catch (AppException e) {
                    ctx.redirect("/Login.html?error=1");
                }
            });

            config.routes.post("/Registro", ctx -> {
                try {
                    String usuario = ctx.formParam("usuario");
                    String nombre = ctx.formParam("nombre");
                    String password = ctx.formParam("password");
                    String confirmPassword = ctx.formParam("confirmPassword");
                    String fotoBase64 = ctx.formParam("fotoBase64");

                    if (password == null || !password.equals(confirmPassword)) {
                        ctx.redirect("/Registro.html?error=general");
                        return;
                    }

                    authService.register(usuario, nombre, password, fotoBase64);
                    ctx.redirect("/Login.html?registered=1");
                } catch (AppException e) {
                    if (e.getStatusCode() == 409) {
                        ctx.redirect("/Registro.html?error=usuario_existe");
                        return;
                    }
                    ctx.redirect("/Registro.html?error=general");
                }
            });

            config.routes.get("/logout", ctx -> {
                SessionUtil.logout(ctx);
                ctx.redirect("/Inicio.html");
            });

            config.routes.get("/api/usuario", ctx -> {
                UsuarioEntity currentUser = authService.getCurrentUserOrNull(SessionUtil.getCurrentUserId(ctx));
                if (currentUser == null) {
                    SessionUtil.logout(ctx);
                    ctx.status(401).result("No ha iniciado sesion");
                    return;
                }
                ctx.json(authService.toDto(currentUser));
            });

            config.routes.get("/api/usuarios", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                ctx.json(usuarioService.listAll(currentUser));
            });

            config.routes.put("/api/usuarios/{id}/rol", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                Long userId = Long.valueOf(ctx.pathParam("id"));
                CambioRolRequest request = ctx.bodyAsClass(CambioRolRequest.class);
                ctx.json(usuarioService.updateRole(currentUser, userId, request));
            });

            config.routes.put("/api/usuarios/{id}/bloqueo", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                Long userId = Long.valueOf(ctx.pathParam("id"));
                CambioBloqueoRequest request = ctx.bodyAsClass(CambioBloqueoRequest.class);
                ctx.json(usuarioService.updateBlocked(currentUser, userId, request));
            });

            config.routes.get("/api/eventos", ctx -> {
                UsuarioEntity currentUser = authService.getCurrentUserOrNull(SessionUtil.getCurrentUserId(ctx));
                ctx.json(eventoService.listEventos(currentUser));
            });

            config.routes.get("/api/eventos/{id}", ctx -> {
                UsuarioEntity currentUser = authService.getCurrentUserOrNull(SessionUtil.getCurrentUserId(ctx));
                ctx.json(eventoService.getEvento(Long.valueOf(ctx.pathParam("id")), currentUser));
            });

            config.routes.post("/api/eventos", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                EventoRequest request = ctx.bodyAsClass(EventoRequest.class);
                ctx.status(201).json(eventoService.createEvento(currentUser, request));
            });

            config.routes.put("/api/eventos/{id}", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                EventoRequest request = ctx.bodyAsClass(EventoRequest.class);
                ctx.json(eventoService.updateEvento(currentUser, Long.valueOf(ctx.pathParam("id")), request));
            });

            config.routes.delete("/api/eventos/{id}", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                eventoService.deleteEvento(currentUser, Long.valueOf(ctx.pathParam("id")));
                ctx.status(204);
            });

            config.routes.post("/api/eventos/{id}/inscribir", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                ctx.status(201).json(eventoService.inscribir(currentUser, Long.valueOf(ctx.pathParam("id"))));
            });

            config.routes.post("/api/eventos/{id}/cancelar", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                eventoService.cancelarInscripcion(currentUser, Long.valueOf(ctx.pathParam("id")));
                ctx.status(204);
            });

            config.routes.get("/api/eventos/{id}/mi-qr", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                ctx.json(eventoService.getMyQr(currentUser, Long.valueOf(ctx.pathParam("id"))));
            });

            config.routes.post("/api/eventos/asistencia", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                QrPayloadDto qrPayload = ctx.bodyAsClass(QrPayloadDto.class);
                eventoService.marcarAsistencia(currentUser, qrPayload);
                ctx.result("Asistencia registrada");
            });

            config.routes.get("/api/eventos/{id}/estadisticas", ctx -> {
                UsuarioEntity currentUser = authService.requireCurrentUser(SessionUtil.getCurrentUserId(ctx));
                ctx.json(eventoService.estadisticas(currentUser, Long.valueOf(ctx.pathParam("id"))));
            });
        }).start(PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseBootstrap::shutdown));
    }
}
