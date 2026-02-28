package org.example.labeling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelCsvImporterTest {

    private static final String HEADER =
            "fullyQualifiedClassName,baselineFlag,sonarFlag,jdeodorantFlag,methodCount,fieldCount,dependencyTypeCount,k1,k2,k3,k4,comment,finalLabel";

    private final LabelCsvImporter importer = new LabelCsvImporter();

    @TempDir
    Path tempDir;

    @Test
    void importValidCsvReturnsTwoEntries() throws IOException {
        String csv = HEADER + "\n"
                + "com.example.Foo,true,false,true,10,5,3,true,true,true,false,,\n"
                + "com.example.Bar,false,false,false,5,2,1,false,false,true,true,,\n";
        Path file = tempDir.resolve("labels.csv");
        Files.writeString(file, csv);

        Map<String, LabelDTO> result = importer.importLabels(file);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("com.example.Foo"));
        assertTrue(result.containsKey("com.example.Bar"));
    }

    @Test
    void importUsesOverrideWhenFinalLabelSet() throws IOException {
        // k1=false, k2=false, k3=false, k4=false → derive would give NO
        // but finalLabel column is GOD_CLASS → override must be preserved
        String csv = HEADER + "\n"
                + "com.example.Override,false,false,false,5,2,1,false,false,false,false,,GOD_CLASS\n";
        Path file = tempDir.resolve("override.csv");
        Files.writeString(file, csv);

        Map<String, LabelDTO> result = importer.importLabels(file);

        LabelDTO dto = result.get("com.example.Override");
        assertNotNull(dto);
        assertEquals(LabelDTO.FinalLabel.GOD_CLASS, dto.getFinalLabel());
    }

    @Test
    void importDerivesLabelWhenFinalLabelEmpty() throws IOException {
        // k1=true, k2=true, k3=true, k4=false → 3 true → GOD_CLASS
        String csv = HEADER + "\n"
                + "com.example.Derived,true,true,true,10,4,2,true,true,true,false,,\n";
        Path file = tempDir.resolve("derived.csv");
        Files.writeString(file, csv);

        Map<String, LabelDTO> result = importer.importLabels(file);

        LabelDTO dto = result.get("com.example.Derived");
        assertNotNull(dto);
        assertEquals(LabelDTO.FinalLabel.GOD_CLASS, dto.getFinalLabel());
    }

    @Test
    void importToleratesUnknownKValue() throws IOException {
        // k1 = "maybe" → treated as null, no exception thrown
        String csv = HEADER + "\n"
                + "com.example.Unknown,true,false,false,5,2,1,maybe,false,false,false,,\n";
        Path file = tempDir.resolve("unknown.csv");
        Files.writeString(file, csv);

        Map<String, LabelDTO> result = importer.importLabels(file);

        LabelDTO dto = result.get("com.example.Unknown");
        assertNotNull(dto);
        assertNull(dto.getK1());
        assertEquals(Boolean.FALSE, dto.getK2());
    }

    @Test
    void importSkipsBlankLines() throws IOException {
        String csv = HEADER + "\n"
                + "com.example.First,true,false,false,5,2,1,true,true,false,false,,\n"
                + "\n"
                + "com.example.Second,false,false,false,3,1,0,false,false,false,false,,\n";
        Path file = tempDir.resolve("blanks.csv");
        Files.writeString(file, csv);

        Map<String, LabelDTO> result = importer.importLabels(file);

        assertEquals(2, result.size());
    }
}
