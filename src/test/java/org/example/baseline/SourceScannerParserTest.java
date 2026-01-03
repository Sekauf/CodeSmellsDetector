package org.example.baseline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SourceScannerParserTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void scansParsesAndExcludesDeterministically() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkgOne = mainJava.resolve("com").resolve("example").resolve("one");
        Path pkgTwo = mainJava.resolve("com").resolve("example").resolve("two");
        Files.createDirectories(pkgOne);
        Files.createDirectories(pkgTwo);

        Files.writeString(pkgOne.resolve("A.java"),
                "package com.example.one;\n" +
                        "public class A {\n" +
                        "  class Inner {}\n" +
                        "}\n",
                StandardCharsets.UTF_8);
        Files.writeString(pkgTwo.resolve("B.java"),
                "package com.example.two;\n" +
                        "public interface B {}\n" +
                        "enum C { ONE }\n",
                StandardCharsets.UTF_8);

        Files.createDirectories(projectRoot.resolve("src").resolve("test").resolve("java"));
        Files.writeString(projectRoot.resolve("src").resolve("test").resolve("java").resolve("Fake.java"),
                "package com.example.test;\npublic class Fake {}\n",
                StandardCharsets.UTF_8);

        Files.createDirectories(projectRoot.resolve("target").resolve("src").resolve("main").resolve("java"));
        Files.writeString(projectRoot.resolve("target").resolve("src").resolve("main").resolve("java").resolve("Fake.java"),
                "package com.example.target;\npublic class Fake {}\n",
                StandardCharsets.UTF_8);

        SourceScanner scanner = new SourceScanner();
        List<Path> filesFirst = scanner.findProductionJavaFiles(projectRoot);
        List<Path> filesSecond = scanner.findProductionJavaFiles(projectRoot);
        assertEquals(filesFirst, filesSecond);

        JavaSourceParser parser = new JavaSourceParser();
        List<ParsedType> parsed = parser.parseFiles(filesFirst);
        assertEquals(4, parsed.size());

        assertEquals("com.example.one.A", parsed.get(0).getFullyQualifiedName());
        assertEquals("com.example.one.A.Inner", parsed.get(1).getFullyQualifiedName());
        assertEquals("com.example.two.B", parsed.get(2).getFullyQualifiedName());
        assertEquals("com.example.two.C", parsed.get(3).getFullyQualifiedName());

        for (Path path : filesFirst) {
            String normalized = path.toString().replace('\\', '/');
            assertFalse(normalized.contains("/src/test/"));
            assertFalse(normalized.contains("/target/"));
        }
    }
}
