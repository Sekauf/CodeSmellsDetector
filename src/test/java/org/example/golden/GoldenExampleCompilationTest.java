package org.example.golden;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GoldenExampleCompilationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void compilesGoldenExampleSources() throws Exception {
        Path projectRoot = Path.of("src", "test", "resources", "golden", "GoldenExample");
        Path sourceRoot = projectRoot.resolve("src").resolve("main").resolve("java");

        List<File> sources = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .map(Path::toFile)
                .collect(Collectors.toList());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("JDK compiler not available.", compiler);

        Path outputDir = temporaryFolder.newFolder("golden-classes").toPath();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(sources);
            List<String> options = List.of("-d", outputDir.toString());
            boolean success = compiler.getTask(null, fileManager, null, options, null, units).call();
            assertTrue("GoldenExample compilation failed.", success);
        }
    }
}
