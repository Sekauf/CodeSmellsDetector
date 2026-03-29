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
import static org.junit.Assert.assertTrue;

public class BaselineAnalyzerSlice4Test {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void detectsGodClassBySizeThreshold() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkg = mainJava.resolve("com").resolve("example").resolve("big");
        Files.createDirectories(pkg);

        StringBuilder builder = new StringBuilder();
        builder.append("package com.example.big;\n");
        builder.append("public class BigClass {\n");
        builder.append("  private int f1;\n");
        builder.append("  private int f2;\n");
        for (int i = 0; i < 50; i++) {
            builder.append("  public void m").append(i).append("() {}\n");
        }
        builder.append("}\n");
        Files.writeString(pkg.resolve("BigClass.java"), builder.toString(), StandardCharsets.UTF_8);

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(10, 5);
        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        assertEquals(1, candidates.size());
        CandidateDTO candidate = candidates.get(0);
        assertEquals("com.example.big.BigClass", candidate.getFullyQualifiedClassName());
        assertTrue(candidate.getReasons().contains("METHODS_PLUS_FIELDS>10"));
    }

    @Test
    public void ignoresSmallClass() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project2").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkg = mainJava.resolve("com").resolve("example").resolve("small");
        Files.createDirectories(pkg);

        String source = ""
                + "package com.example.small;\n"
                + "public class SmallClass {\n"
                + "  private int value;\n"
                + "  public void a() {}\n"
                + "  public void b() {}\n"
                + "}\n";
        Files.writeString(pkg.resolve("SmallClass.java"), source, StandardCharsets.UTF_8);

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(10, 5);
        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        assertEquals(0, candidates.size());
    }

    @Test
    public void analyzesMultiplePackagesWithoutMissingClasses() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project3").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkgOne = mainJava.resolve("com").resolve("example").resolve("one");
        Path pkgTwo = mainJava.resolve("com").resolve("example").resolve("two");
        Files.createDirectories(pkgOne);
        Files.createDirectories(pkgTwo);

        String classOne = ""
                + "package com.example.one;\n"
                + "public class First {\n"
                + "  private int a;\n"
                + "  public void a() {}\n"
                + "  public void b() {}\n"
                + "  public void c() {}\n"
                + "  public void d() {}\n"
                + "  public void e() {}\n"
                + "  public void f() {}\n"
                + "  public void g() {}\n"
                + "  public void h() {}\n"
                + "  public void i() {}\n"
                + "  public void j() {}\n"
                + "  public void k() {}\n"
                + "}\n";
        Files.writeString(pkgOne.resolve("First.java"), classOne, StandardCharsets.UTF_8);

        String classTwo = ""
                + "package com.example.two;\n"
                + "public class Second {\n"
                + "  private int a;\n"
                + "  private int b;\n"
                + "  private int c;\n"
                + "  private int d;\n"
                + "  private int e;\n"
                + "  private int f;\n"
                + "  private int g;\n"
                + "  private int h;\n"
                + "  private int i;\n"
                + "  private int j;\n"
                + "  private int k;\n"
                + "}\n";
        Files.writeString(pkgTwo.resolve("Second.java"), classTwo, StandardCharsets.UTF_8);

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(10, 100);
        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        assertEquals(2, candidates.size());
        assertEquals("com.example.one.First", candidates.get(0).getFullyQualifiedClassName());
        assertEquals("com.example.two.Second", candidates.get(1).getFullyQualifiedClassName());
    }

    @Test
    public void skipsInvalidTypeNames() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("artefact-test").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkg = mainJava.resolve("org").resolve("example");
        Files.createDirectories(pkg);

        String source = ""
                + "package org.example;\n"
                + "/**\n"
                + " * Values should class as valid input.\n"
                + " */\n"
                + "public class SmallValid {\n"
                + "  public void a() {}\n"
                + "}\n";
        Files.writeString(pkg.resolve("SmallValid.java"), source, StandardCharsets.UTF_8);

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(1, 1);
        List<CandidateDTO> candidates = analyzer.analyze(projectRoot, thresholds);

        for (CandidateDTO c : candidates) {
            assertTrue("FQN should start with uppercase simple name, got: " + c.getFullyQualifiedClassName(),
                    Character.isUpperCase(c.getFullyQualifiedClassName()
                            .substring(c.getFullyQualifiedClassName().lastIndexOf('.') + 1).charAt(0)));
        }
    }

    @Test
    public void producesMetadata() throws IOException {
        Path projectRoot = temporaryFolder.newFolder("project4").toPath();
        Path mainJava = projectRoot.resolve("src").resolve("main").resolve("java");
        Path pkg = mainJava.resolve("com").resolve("example").resolve("meta");
        Files.createDirectories(pkg);

        String source = ""
                + "package com.example.meta;\n"
                + "public class Meta {\n"
                + "  public void a() {}\n"
                + "}\n";
        Files.writeString(pkg.resolve("Meta.java"), source, StandardCharsets.UTF_8);

        BaselineAnalyzer analyzer = new BaselineAnalyzer();
        BaselineThresholds thresholds = new BaselineThresholds(1, 1);
        BaselineAnalysisResult result = analyzer.analyzeWithMetadata(projectRoot, thresholds);

        assertTrue(result.getMetadata().getJavaVersion() != null);
        assertTrue(result.getMetadata().getJavaParserVersion() != null);
        assertTrue(result.getMetadata().getTimestampIso() != null);
    }
}
