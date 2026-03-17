package org.example.jdeodorant;

import org.example.baseline.CandidateDTO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JDeodorantImporterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void import_emptyFile_returnsEmptyList() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-empty").toPath();
        Path csvPath = dir.resolve("jdeo.csv");
        Files.writeString(csvPath, "", StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(0, result.size());
    }

    @Test
    public void import_headerOnly_returnsEmptyList() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-header-only").toPath();
        Path csvPath = dir.resolve("jdeo.csv");
        Files.writeString(csvPath, "Class,Smell\n", StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(0, result.size());
    }

    @Test
    public void import_missingFile_returnsEmptyList() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-missing-file").toPath();
        Path csvPath = dir.resolve("missing.csv");

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(0, result.size());
    }

    @Test
    public void import_goldenCsv_matchesExpectedOrder() throws IOException {
        Path csvPath = Path.of("src", "test", "resources", "jdeodorant", "jdeodorant_golden.csv");

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        List<String> expected = Arrays.asList(
                "com.example.alpha.AlphaFacade",
                "com.example.beta.BetaCore"
        );

        assertEquals(expected.size(), result.size());
        assertEquals(expected.get(0), result.get(0).getFullyQualifiedClassName());
        assertEquals(expected.get(1), result.get(1).getFullyQualifiedClassName());
    }

    @Test
    public void import_withCustomSmellType_filtersNonDefaultSmell() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-custom-smell").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.CustomOne,Feature Envy\n");
        builder.append("com.example.SkipGod,God Class\n");
        builder.append("com.example.CustomTwo,FeatureEnvy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString(), "Feature Envy");

        assertEquals(2, result.size());
        assertEquals("com.example.CustomOne", result.get(0).getFullyQualifiedClassName());
        assertEquals("com.example.CustomTwo", result.get(1).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
        assertEquals(true, result.get(1).isJdeodorantFlag());
    }

    @Test
    public void import_deduplicatesAndSortsGodClasses() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-basic").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("class,smell\n");
        builder.append("com.example.Beta,God Class\n");
        builder.append("com.example.Alpha,God Class\n");
        builder.append("com.example.Beta,God Class\n");
        builder.append("com.example.Skip,Feature Envy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(2, result.size());
        assertEquals("com.example.Alpha", result.get(0).getFullyQualifiedClassName());
        assertEquals("com.example.Beta", result.get(1).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
        assertEquals(true, result.get(1).isJdeodorantFlag());
    }

    @Test
    public void import_commaWithQuotes_parsesGodClassRows() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-comma").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("class,smell\n");
        builder.append("\"com.example.A,Inner\",God Class\n");
        builder.append("com.example.Skip,Feature Envy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.A,Inner", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_semicolonWithQuotes_parsesGodClassRows() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-semi").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("class;smell\n");
        builder.append("\"com.example.B;Inner\";Monster Class\n");
        builder.append("com.example.Skip;Long Method\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.B;Inner", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_semicolonDelimiter_filtersGodClassRows() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-semi-plain").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class;Smell\n");
        builder.append("com.example.GodSemi;God Class\n");
        builder.append("com.example.SkipSemi;Long Method\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.GodSemi", result.get(0).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
    }

    @Test
    public void import_tabDelimited_parsesGodClassRows() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-tab").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("class\tsmell\n");
        builder.append("com.example.Tab\tGodClass\n");
        builder.append("com.example.Skip\tData Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.Tab", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_standardCommaCsv_filtersAndMapsGodClass() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-standard").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.GodOne,God Class\n");
        builder.append("com.example.SkipOne,Long Method\n");
        builder.append("com.example.GodTwo,GodClass\n");
        builder.append("com.example.SkipTwo,Feature Envy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(2, result.size());
        assertEquals("com.example.GodOne", result.get(0).getFullyQualifiedClassName());
        assertEquals("com.example.GodTwo", result.get(1).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
        assertEquals(true, result.get(1).isJdeodorantFlag());
    }

    @Test
    public void import_headerVariants_qualifiedNameAndSmellType() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-header-variants").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("QualifiedName,Smell Type\n");
        builder.append("com.example.VariantOne,GodClass\n");
        builder.append("com.example.VariantSkip,Feature Envy\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.VariantOne", result.get(0).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
    }

    @Test
    public void import_quotedValuesContainingDelimiter_filtersCorrectly() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-quoted-delimiter").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("\"com.example.With,Comma\",God Class\n");
        builder.append("\"com.example.Skip,Comma\",Long Method\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.With,Comma", result.get(0).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
    }

    @Test
    public void import_missingSmellHeader_throwsHelpfulError() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-missing-smell").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class\n");
        builder.append("com.example.GodOne\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        try {
            importer.importJDeodorantCsv(csvPath.toString());
        } catch (IllegalArgumentException ex) {
            assertEquals(true, ex.getMessage().contains("Missing smell column"));
            assertEquals(true, ex.getMessage().contains("Expected one of"));
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException for missing smell column.");
    }

    // --- Inner-class collapsing tests ---

    @Test
    public void import_innerClassCollapsedToOuterType() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-inner").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.Outer.Inner,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.Outer", result.get(0).getFullyQualifiedClassName());
        assertEquals(true, result.get(0).isJdeodorantFlag());
    }

    @Test
    public void import_multipleInnerClassesMergeToSameOuter() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-multi-inner").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.Builder.FieldFormatter,God Class\n");
        builder.append("com.example.Builder.Separator,God Class\n");
        builder.append("com.example.Builder,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.Builder", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_deeplyNestedInnerClassCollapsed() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-deep").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.alpha.Outer.Mid.Inner,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.alpha.Outer", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_topLevelClassUnchanged() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-toplevel").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.TopLevel,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("com.example.TopLevel", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_noPackageInnerClassCollapsed() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-nopkg").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("Outer.Inner,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(1, result.size());
        assertEquals("Outer", result.get(0).getFullyQualifiedClassName());
    }

    @Test
    public void import_mixedInnerAndTopLevel_correctDedup() throws IOException {
        Path dir = temporaryFolder.newFolder("jdeo-mixed").toPath();
        Path csvPath = dir.resolve("jdeo.csv");

        StringBuilder builder = new StringBuilder();
        builder.append("Class,Smell\n");
        builder.append("com.example.Alpha,God Class\n");
        builder.append("com.example.Beta.Inner,God Class\n");
        builder.append("com.example.Gamma,God Class\n");
        builder.append("com.example.Alpha.Nested,God Class\n");
        Files.writeString(csvPath, builder.toString(), StandardCharsets.UTF_8);

        JDeodorantImporter importer = new JDeodorantImporter();
        List<CandidateDTO> result = importer.importJDeodorantCsv(csvPath.toString());

        assertEquals(3, result.size());
        assertEquals("com.example.Alpha", result.get(0).getFullyQualifiedClassName());
        assertEquals("com.example.Beta", result.get(1).getFullyQualifiedClassName());
        assertEquals("com.example.Gamma", result.get(2).getFullyQualifiedClassName());
    }

    @Test
    public void collapseInnerClass_variousCases() {
        assertEquals("com.example.Outer", JDeodorantImporter.collapseInnerClass("com.example.Outer.Inner"));
        assertEquals("com.example.Outer", JDeodorantImporter.collapseInnerClass("com.example.Outer.Mid.Inner"));
        assertEquals("com.example.Outer", JDeodorantImporter.collapseInnerClass("com.example.Outer"));
        assertEquals("Outer", JDeodorantImporter.collapseInnerClass("Outer.Inner"));
        assertEquals("Outer", JDeodorantImporter.collapseInnerClass("Outer"));
        assertEquals("", JDeodorantImporter.collapseInnerClass(""));
        assertEquals(null, JDeodorantImporter.collapseInnerClass(null));
    }
}
