package com.fedeiatech.sistemagestionpyme.service;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;

public class LoggingConfig {

    private static boolean inicializado = false;

    public static synchronized void inicializar() {
        if (inicializado) return;
        inicializado = true;

        try {
            java.io.File carpetaLogs = new java.io.File("logs");
            if (!carpetaLogs.exists()) carpetaLogs.mkdirs();

            FileHandler fileHandler = new FileHandler("logs/app.log", 1_000_000, 3, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            LogManager.getLogManager().reset();
            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("No se pudo inicializar el logging a archivo: " + e.getMessage());
        }
    }
}
