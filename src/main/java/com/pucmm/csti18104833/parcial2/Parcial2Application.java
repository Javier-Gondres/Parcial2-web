package com.pucmm.csti18104833.parcial2;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class Parcial2Application {
    public static void main(String[] args) {
        var app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.result("Hello World"));
        }).start(7070);
    }
}
