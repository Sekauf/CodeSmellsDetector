# Repository Guidelines

## Project Structure & Module Organization
- Root `pom.xml` defines the Maven archetype; settings live in `.mvn/`.
- Template sources: `src/main/resources/archetype-resources/src/main/java/App.java` and supporting packages; keep archetype placeholders like `$org.example` intact.
- Template tests: `src/main/resources/archetype-resources/src/test/java/AppTest.java`.
- Archetype metadata: `src/main/resources/META-INF/maven/archetype.xml` controls generated layout.

## Build, Test, and Development Commands
- `mvn clean verify`: validate the archetype and run template tests.
- `mvn archetype:install`: install the archetype to your local catalog for manual testing.
- `mvn archetype:generate -DarchetypeCatalog=local -DarchetypeArtifactId=CodeSmellsDetector -DarchetypeGroupId=org.example -DgroupId=com.acme -DartifactId=demo`: generate a sample project to smoke-test changes; adjust coordinates as needed.

## Coding Style & Naming Conventions
- Java sources use 4-space indentation, PascalCase types, camelCase fields/methods, and uppercase constants.
- Keep template variables (`$org.example`, `$artifactId`) unchanged in the archetype; add new placeholders rather than hardcoding values.
- Prefer small, focused classes; keep imports ordered and remove unused ones.

## Testing Guidelines
- Unit tests live beside the template sources under `.../src/test/java`; name files `*Test`.
- Use the bundled JUnit 3 style for consistency unless you upgrade the archetype and tests together.
- Add at least one test for each new template feature; run `mvn test` before pushing.

## Commit & Pull Request Guidelines
- Use imperative, concise commit messages (e.g., `Add smell detector skeleton`, `Tighten archetype metadata`).
- Include a short PR description of changes, rationale, and manual verification (commands run or sample project generated).
- Link issues or tasks when available and include screenshots or logs if behavior changes.

## Security & Configuration Tips
- Do not commit secrets or personal access tokens; keep credentials out of example code.
- Ensure `JAVA_HOME` points to a supported JDK on contributors machines; align Maven version across team.
