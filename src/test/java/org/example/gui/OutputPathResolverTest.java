package org.example.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputPathResolverTest {

    private final OutputPathResolver resolver = new OutputPathResolver();

    @Test
    void resolvesCombinesOutputDirAndProjectName() {
        Path result = resolver.resolve("output", "my-project");
        assertEquals(Path.of("output", "my-project"), result);
    }

    @Test
    void resolveUsesDefaultsForBlankInputs() {
        Path result = resolver.resolve("", "");
        assertEquals(Path.of("output", "project"), result);
    }

    @Test
    void resolveUsesDefaultsForNullInputs() {
        Path result = resolver.resolve(null, null);
        assertEquals(Path.of("output", "project"), result);
    }

    @Test
    void resolveTrimsWhitespace() {
        Path result = resolver.resolve("  out  ", "  proj  ");
        assertEquals(Path.of("out", "proj"), result);
    }

    @Test
    void resolveSanitizesInvalidChars() {
        Path result = resolver.resolve("output", "my:project*name");
        String name = result.getFileName().toString();
        assertTrue(name.contains("_"), "Special chars should be replaced by underscore");
    }

    @Test
    void ensureExistsCreatesDirectory(@TempDir Path tmp) throws IOException {
        Path target = tmp.resolve("sub1").resolve("sub2");
        Path created = resolver.ensureExists(target);
        assertTrue(Files.isDirectory(created));
        assertEquals(target, created);
    }

    @Test
    void suggestProjectNameExtractsLastComponent() {
        String name = resolver.suggestProjectName("/home/user/my-project");
        assertEquals("my-project", name);
    }

    @Test
    void suggestProjectNameHandlesWindowsPath() {
        String name = resolver.suggestProjectName("C:\\Users\\user\\commons-collections");
        assertEquals("commons-collections", name);
    }

    @Test
    void suggestProjectNameReturnsEmptyForBlankInput() {
        assertEquals("", resolver.suggestProjectName(""));
        assertEquals("", resolver.suggestProjectName(null));
    }
}
