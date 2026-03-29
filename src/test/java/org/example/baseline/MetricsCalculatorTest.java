package org.example.baseline;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MetricsCalculatorTest {

    @Test
    public void javadocWithClassKeywordDoesNotProduceArtefact() {
        String source = ""
                + "package org.joda.time.field;\n"
                + "/**\n"
                + " * The value should class as valid.\n"
                + " */\n"
                + "public class ValidField {\n"
                + "  private int value;\n"
                + "  public void process() {}\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals("org.joda.time.field.ValidField", metrics.getFullyQualifiedName());
        assertEquals(1, metrics.getMethodCount());
    }

    @Test
    public void lineCommentWithClassKeywordIgnored() {
        String source = ""
                + "package com.example;\n"
                + "// this class should be refactored\n"
                + "public class Actual {\n"
                + "  public void run() {}\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals("com.example.Actual", metrics.getFullyQualifiedName());
    }

    @Test
    public void stringLiteralWithClassKeywordIgnored() {
        String source = ""
                + "package com.example;\n"
                + "public class Real {\n"
                + "  private String msg = \"this class is fake\";\n"
                + "  public void run() {}\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals("com.example.Real", metrics.getFullyQualifiedName());
    }

    @Test
    public void countsMethodsFieldsAndImports() {
        String source = ""
                + "package com.example.metrics;\n"
                + "import java.util.List;\n"
                + "import java.util.Map;\n"
                + "import com.other.Dependency;\n"
                + "public class Sample {\n"
                + "  private int value;\n"
                + "  public void a() {}\n"
                + "  protected String b() { return \"x\"; }\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals("com.example.metrics.Sample", metrics.getFullyQualifiedName());
        assertEquals(2, metrics.getMethodCount());
        assertEquals(1, metrics.getFieldCount());
        assertEquals(3, metrics.getDependencyTypeCount());
    }

    @Test
    public void ignoresJavaLangOnlyTypes() {
        String source = ""
                + "package com.example.metrics;\n"
                + "public class LangOnly {\n"
                + "  private String name;\n"
                + "  public Object value() { return null; }\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals(0, metrics.getDependencyTypeCount());
    }

    @Test
    public void handlesGenericsAndArrays() {
        String source = ""
                + "package com.example.metrics;\n"
                + "import java.util.List;\n"
                + "public class GenericHolder {\n"
                + "  private List<String>[] values;\n"
                + "  public void set(List<String>[] values) { this.values = values; }\n"
                + "}\n";

        MetricsCalculator calculator = new MetricsCalculator();
        ClassMetrics metrics = calculator.calculateFromSource(source);

        assertEquals(1, metrics.getDependencyTypeCount());
        assertEquals(1, metrics.getFieldCount());
        assertEquals(1, metrics.getMethodCount());
    }
}
