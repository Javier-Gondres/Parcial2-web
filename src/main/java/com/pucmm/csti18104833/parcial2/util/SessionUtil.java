package com.pucmm.csti18104833.parcial2.util;

import io.javalin.http.Context;

public final class SessionUtil {
    public static final String SESSION_USER_ID = "usuarioId";

    private SessionUtil() {
    }

    public static void login(Context ctx, Long userId) {
        ctx.sessionAttribute(SESSION_USER_ID, userId);
    }

    public static void logout(Context ctx) {
        if (ctx.req().getSession(false) != null) {
            ctx.req().getSession().invalidate();
        }
    }

    public static Long getCurrentUserId(Context ctx) {
        return ctx.sessionAttribute(SESSION_USER_ID);
    }
}
