# CodeSmellsDetector

## Logging & Export
This project uses JUL (java.util.logging). Configure console logging and optional file logging with
`org.example.logging.LoggingConfigurator`. File logs default to `output/analysis.log` with a stable format
and no timestamped filenames.

Deterministic exports are handled by `org.example.export.ResultExporter`. Default output structure:
- `output/results.csv`
- `output/results.json`
- `output/metrics.json` (optional, only when metrics are provided)

`results.csv` and `results.json` are sorted deterministically (primary: class name, secondary: stable tie
breaker). CSV uses a fixed column order, JSON uses stable field order, UTF-8 encoding, and consistent
line endings. Existing files are overwritten deterministically (no random suffixes).

## CLI
Minimal CLI entrypoint:
```
java -jar target/CodeSmellsDetector.jar --project <path> [options]
```

Options:
- `--output <dir>` Output directory (default: `output/`)
- `--run-sonar` Enable SonarQube analysis (config via env)
- `--jdeodorant-csv <path>` Import JDeodorant CSV export
- `--baseline-methods-fields <n>` Baseline size threshold (default: 40)
- `--baseline-dependency-types <n>` Baseline dependency threshold (default: 5)
- `--help` Show help
