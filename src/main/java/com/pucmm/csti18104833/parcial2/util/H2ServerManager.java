package com.pucmm.csti18104833.parcial2.util;

import org.h2.tools.Server;

import java.sql.SQLException;

public final class H2ServerManager {
    private static Server tcpServer;

    private H2ServerManager() {
    }

    public static synchronized void start() {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            return;
        }
        try {
            tcpServer = Server.createTcpServer(
                "-tcp",
                "-tcpAllowOthers",
                "-tcpPort",
                "9092",
                "-ifNotExists"
            ).start();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo iniciar el servidor H2", e);
        }
    }

    public static synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
        }
    }
}
