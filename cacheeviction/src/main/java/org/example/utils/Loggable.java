package org.example.utils;

import java.io.File;
import java.io.FileWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Loggable {
    default Logger logger() {
        return LoggerFactory.getLogger(this.getClass().getName());
    }

    default void clearLog(String name) {
        try {
            File logFile = new File("cacheeviction\\src\\main\\resources\\logs\\" + name + ".log");
            if (logFile.exists()) {
                new FileWriter(logFile, false).close(); // Sobrescreve o arquivo com nada, limpando-o
            }
        } catch (Exception e) {
            System.out.println("Erro ao limpar o log: " + e.getMessage());
        }
    }

    default void info(String message) {
        System.out.println("\n" + message);
        logger().info(message);
        System.out.println("--------------------------------------------------------------");
    }

    default void message(String message) {
        logger().info(message);
    }

    default void erro(String message) {
        System.out.println("\n" + message);
        logger().error(message);
        System.out.println("--------------------------------------------------------------");
    }

    default void debug(String message) {
        System.out.println("\n" + message);
        logger().debug(message);
        System.out.println("--------------------------------------------------------------");
    }

    default void warn(String message) {
        System.out.println("\n" + message);
        logger().warn(message);
        System.out.println("--------------------------------------------------------------");
    }
}