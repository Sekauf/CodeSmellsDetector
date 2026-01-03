package org.example.baseline;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class CandidateCsvUtilTest {
    @Test
    public void escapesFieldsWithCommaQuoteAndNewline() {
        assertEquals("simple", CandidateCsvUtil.escapeCsvField("simple"));
        assertEquals("\"a,b\"", CandidateCsvUtil.escapeCsvField("a,b"));
        assertEquals("\"a\"\"b\"", CandidateCsvUtil.escapeCsvField("a\"b"));
        assertEquals("\"a\nb\"", CandidateCsvUtil.escapeCsvField("a\nb"));
    }

    @Test
    public void parsesQuotedAndUnquotedFields() {
        List<String> fields = CandidateCsvUtil.parseCsvLine("a,\"b,c\",d");
        assertEquals(3, fields.size());
        assertEquals("a", fields.get(0));
        assertEquals("b,c", fields.get(1));
        assertEquals("d", fields.get(2));
    }

    @Test
    public void parsesDoubledQuotes() {
        List<String> fields = CandidateCsvUtil.parseCsvLine("\"a\"\"b\"");
        assertEquals(1, fields.size());
        assertEquals("a\"b", fields.get(0));
    }

    @Test
    public void preservesTrailingEmptyField() {
        List<String> fields = CandidateCsvUtil.parseCsvLine("a,b,");
        assertEquals(3, fields.size());
        assertEquals("", fields.get(2));
    }
}
