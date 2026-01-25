package org.example.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LoggingConfigurator {
    private LoggingConfigurator() {
    }

    public static void configure(Path outputDir, boolean enableFile) throws IOException {
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        root.setLevel(Level.INFO);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new StableFormatter());
        root.addHandler(consoleHandler);

        if (enableFile) {
            Path resolved = outputDir == null ? Path.of("output") : outputDir;
            Files.createDirectories(resolved);
            Path logPath = resolved.resolve("analysis.log");
            FileHandler fileHandler = new FileHandler(logPath.toString(), false);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setFormatter(new StableFormatter());
            root.addHandler(fileHandler);
        }
    }

    private static class StableFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(record.getLevel().getName())
                    .append(" ")
                    .append(record.getLoggerName())
                    .append(" - ")
                    .append(formatMessage(record))
                    .append("\n");
            if (record.getThrown() != null) {
                StringWriter writer = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(writer));
                builder.append(writer).append("\n");
            }
            return builder.toString();
        }
    }
}
