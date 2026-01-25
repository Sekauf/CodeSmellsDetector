package org.example.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.example.baseline.BaselineThresholds;
import org.example.jdeodorant.ProjectConfig;
import org.example.logging.LoggingConfigurator;
import org.example.orchestrator.AnalysisOrchestrator;
import org.example.sonar.SonarConfig;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            Arguments parsed = Arguments.parse(args);
            if (parsed.showHelp) {
                printUsage();
                return;
            }
            if (parsed.projectPath == null || parsed.projectPath.isBlank()) {
                System.err.println("Missing required --project <path>.");
                printUsage();
                System.exit(2);
            }

            Path outputDir = parsed.outputPath == null ? Path.of("output") : Path.of(parsed.outputPath);
            LoggingConfigurator.configure(outputDir, true);

            BaselineThresholds thresholds = new BaselineThresholds(
                    parsed.baselineMethodsFieldsThreshold,
                    parsed.baselineDependencyTypesThreshold
            );

            SonarConfig sonarConfig = parsed.runSonar
                    ? SonarConfig.fromEnv(resolveProjectKey(parsed.projectPath))
                    : null;
            ProjectConfig jdeodorantConfig = parsed.jdeodorantCsvPath == null
                    ? null
                    : ProjectConfig.forJdeodorantCsv(parsed.jdeodorantCsvPath);

            AnalysisOrchestrator orchestrator = new AnalysisOrchestrator();
            orchestrator.run(
                    Path.of(parsed.projectPath),
                    thresholds,
                    sonarConfig,
                    jdeodorantConfig,
                    outputDir
            );
        } catch (IllegalArgumentException ex) {
            System.err.println("Error: " + ex.getMessage());
            printUsage();
            System.exit(2);
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Analysis run failed.", ex);
            System.exit(1);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Unexpected error.", ex);
            System.exit(1);
        }
    }

    private static void printUsage() {
        String usage = ""
                + "Usage:\n"
                + "  java -jar target/CodeSmellsDetector.jar --project <path> [options]\n"
                + "\n"
                + "Options:\n"
                + "  --output <dir>                    Output directory (default: output/)\n"
                + "  --run-sonar                       Enable SonarQube analysis (config via env)\n"
                + "  --jdeodorant-csv <path>           Import JDeodorant CSV export\n"
                + "  --baseline-methods-fields <n>     Baseline size threshold (default: 40)\n"
                + "  --baseline-dependency-types <n>   Baseline dependency threshold (default: 5)\n"
                + "  --help                            Show this help\n";
        System.out.println(usage);
    }

    private static String resolveProjectKey(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            return "project";
        }
        Path path = Path.of(projectPath);
        Path name = path.getFileName();
        return name == null ? "project" : name.toString();
    }

    private static final class Arguments {
        private String projectPath;
        private String outputPath;
        private String jdeodorantCsvPath;
        private boolean runSonar;
        private boolean showHelp;
        private int baselineMethodsFieldsThreshold = 40;
        private int baselineDependencyTypesThreshold = 5;

        static Arguments parse(String[] args) {
            Arguments parsed = new Arguments();
            if (args == null || args.length == 0) {
                return parsed;
            }
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    parsed.showHelp = true;
                } else if ("--project".equals(arg)) {
                    parsed.projectPath = nextArg(args, ++i, "--project");
                } else if ("--output".equals(arg)) {
                    parsed.outputPath = nextArg(args, ++i, "--output");
                } else if ("--jdeodorant-csv".equals(arg)) {
                    parsed.jdeodorantCsvPath = nextArg(args, ++i, "--jdeodorant-csv");
                } else if ("--run-sonar".equals(arg)) {
                    parsed.runSonar = true;
                } else if ("--baseline-methods-fields".equals(arg)) {
                    String value = nextArg(args, ++i, "--baseline-methods-fields");
                    parsed.baselineMethodsFieldsThreshold = parsePositiveInt(value, "--baseline-methods-fields");
                } else if ("--baseline-dependency-types".equals(arg)) {
                    String value = nextArg(args, ++i, "--baseline-dependency-types");
                    parsed.baselineDependencyTypesThreshold = parsePositiveInt(value, "--baseline-dependency-types");
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return parsed;
        }

        private static String nextArg(String[] args, int index, String flag) {
            if (index >= args.length || args[index] == null || args[index].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return args[index];
        }

        private static int parsePositiveInt(String value, String flag) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < 0) {
                    throw new IllegalArgumentException(flag + " must be >= 0");
                }
                return parsed;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid integer for " + flag + ": " + value);
            }
        }
    }
}
