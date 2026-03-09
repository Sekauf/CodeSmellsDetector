package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectValidatorTest {

    private final ProjectValidator validator = new ProjectValidator();

    @Test
    void nullPathIsInvalid() {
        assertFalse(validator.validate(null).isValid());
    }

    @Test
    void nonExistentPathIsInvalid(@TempDir Path tmp) {
        assertFalse(validator.validate(tmp.resolve("does-not-exist")).isValid());
    }

    @Test
    void directoryWithoutBuildFileIsInvalid(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("Foo.java"), "class Foo {}");
        ProjectValidator.Result result = validator.validate(tmp);
        assertFalse(result.isValid());
        assertTrue(result.message().contains("pom.xml"));
    }

    @Test
    void directoryWithBuildFileButNoJavaIsInvalid(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("pom.xml"), "<project/>");
        ProjectValidator.Result result = validator.validate(tmp);
        assertFalse(result.isValid());
        assertTrue(result.message().contains(".java"));
    }

    @Test
    void validMavenProjectPassesValidation(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("pom.xml"), "<project/>");
        Path src = tmp.resolve("src/main/java");
        Files.createDirectories(src);
        Files.writeString(src.resolve("Foo.java"), "class Foo {}");
        ProjectValidator.Result result = validator.validate(tmp);
        assertTrue(result.isValid());
        assertEquals(ProjectValidator.Status.VALID, result.status());
    }

    @Test
    void validGradleProjectPassesValidation(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("build.gradle"), "");
        Files.writeString(tmp.resolve("Foo.java"), "class Foo {}");
        assertTrue(validator.validate(tmp).isValid());
    }

    @Test
    void gradleKtsIsAcceptedAsBuildFile(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("build.gradle.kts"), "");
        Files.writeString(tmp.resolve("Bar.java"), "class Bar {}");
        assertTrue(validator.validate(tmp).isValid());
    }
}
