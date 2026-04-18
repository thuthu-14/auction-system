package util;

import java.io.*;
import java.nio.file.*;

public class LoggerUtil {
    private static final String LOG_FILE = "logs/application.log";
    private static final String LOG_DIR = "logs";


    private static void log(String level, String message) {
        String timestamp = String.format("%d", System.currentTimeMillis());
        String logMessage = String.format("[%s] %s: %s%n", timestamp, level, message);

        System.out.print(logMessage);

        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            Files.write(
                    Paths.get(LOG_FILE),
                    logMessage.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void warn(String message) {
        log("WARN", message);
    }

    public static void error(String message) {
        log("ERROR", message);
    }

    public static void debug(String message) {
        log("DEBUG", message);
    }
}
