package org.example.baseline;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsCalculator {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\s+([\\w\\.]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_\\$][\\w\\$]*)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+([\\w\\.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*(?:public|protected|private|static|final|abstract|synchronized|native|strictfp|default|\\s)+"
                    + "\\s*[\\w\\<\\>\\[\\], ?]+\\s+(\\w+)\\s*\\([^)]*\\)\\s*(\\{|;)");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "(?m)^[ \\t]*(?:public|protected|private|static|final|transient|volatile|\\s)+"
                    + "\\s*[\\w\\<\\>\\[\\], ?]+\\s+[a-zA-Z_][\\w]*\\s*(=|;)");
    private static final Pattern CAPITALIZED_IDENTIFIER = Pattern.compile("\\b([A-Z][A-Za-z0-9_]*)\\b");

    private static final Set<String> PRIMITIVES = new HashSet<>(Arrays.asList(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void"
    ));
    private static final Set<String> JAVA_LANG_TYPES = new HashSet<>(Arrays.asList(
            "String", "Object", "Integer", "Long", "Boolean", "Short", "Byte", "Character",
            "Double", "Float", "Void", "Class", "Enum"
    ));

    public ClassMetrics calculateFromAst(Object typeDeclaration, Object compilationUnit) throws IOException {
        if (!isJavaParserAvailable()) {
            throw new IllegalStateException("JavaParser not available on classpath.");
        }
        try {
            String fqn = resolveFullyQualifiedName(typeDeclaration, compilationUnit);
            int methodCount = countMethods(typeDeclaration);
            int fieldCount = countFields(typeDeclaration);
            int dependencyCount = countDependencies(typeDeclaration, compilationUnit);
            return new ClassMetrics(fqn, methodCount, fieldCount, dependencyCount);
        } catch (Exception ex) {
            throw new IOException("Failed to calculate metrics from AST.", ex);
        }
    }

    public ClassMetrics calculateFromSource(String source) {
        String packageName = extractPackage(source);
        String className = extractPrimaryTypeName(source);
        String fqn = packageName.isEmpty() ? className : packageName + "." + className;

        String classBody = extractPrimaryTypeBody(source);
        int methodCount = countMethodsFromSource(classBody, className);
        int fieldCount = countFieldsFromSource(classBody);
        int dependencyCount = countDependenciesFromSource(source, classBody, className);

        return new ClassMetrics(fqn, methodCount, fieldCount, dependencyCount);
    }

    private boolean isJavaParserAvailable() {
        try {
            Class.forName("com.github.javaparser.StaticJavaParser");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private String resolveFullyQualifiedName(Object typeDeclaration, Object compilationUnit) throws Exception {
        String packageName = "";
        if (compilationUnit != null) {
            Method getPackageDeclaration = compilationUnit.getClass().getMethod("getPackageDeclaration");
            Optional<?> packageOptional = (Optional<?>) getPackageDeclaration.invoke(compilationUnit);
            if (packageOptional.isPresent()) {
                Object packageDecl = packageOptional.get();
                Method getNameAsString = packageDecl.getClass().getMethod("getNameAsString");
                packageName = (String) getNameAsString.invoke(packageDecl);
            }
        }

        Method getNameAsString = typeDeclaration.getClass().getMethod("getNameAsString");
        String name = (String) getNameAsString.invoke(typeDeclaration);

        Class<?> typeDeclClass = Class.forName("com.github.javaparser.ast.body.TypeDeclaration");
        List<String> outers = new ArrayList<>();
        Method getParentNode = typeDeclaration.getClass().getMethod("getParentNode");
        Optional<?> parent = (Optional<?>) getParentNode.invoke(typeDeclaration);
        while (parent.isPresent()) {
            Object parentNode = parent.get();
            if (typeDeclClass.isInstance(parentNode)) {
                Method parentName = parentNode.getClass().getMethod("getNameAsString");
                outers.add((String) parentName.invoke(parentNode));
            }
            Method parentGetParent = parentNode.getClass().getMethod("getParentNode");
            parent = (Optional<?>) parentGetParent.invoke(parentNode);
        }

        StringBuilder builder = new StringBuilder();
        if (!packageName.isEmpty()) {
            builder.append(packageName).append(".");
        }
        for (int i = outers.size() - 1; i >= 0; i--) {
            builder.append(outers.get(i)).append(".");
        }
        builder.append(name);
        return builder.toString();
    }

    private int countMethods(Object typeDeclaration) throws Exception {
        Method getMembers = typeDeclaration.getClass().getMethod("getMembers");
        List<?> members = (List<?>) getMembers.invoke(typeDeclaration);
        int count = 0;
        for (Object member : members) {
            String simple = member.getClass().getSimpleName();
            if ("MethodDeclaration".equals(simple)) {
                count++;
            }
        }
        return count;
    }

    private int countFields(Object typeDeclaration) throws Exception {
        Method getMembers = typeDeclaration.getClass().getMethod("getMembers");
        List<?> members = (List<?>) getMembers.invoke(typeDeclaration);
        int count = 0;
        for (Object member : members) {
            String simple = member.getClass().getSimpleName();
            if ("FieldDeclaration".equals(simple)) {
                Method getVariables = member.getClass().getMethod("getVariables");
                List<?> variables = (List<?>) getVariables.invoke(member);
                count += variables.size();
            }
        }
        return count;
    }

    private int countDependencies(Object typeDeclaration, Object compilationUnit) throws Exception {
        Set<String> dependencies = new HashSet<>();

        if (compilationUnit != null) {
            Method getImports = compilationUnit.getClass().getMethod("getImports");
            List<?> imports = (List<?>) getImports.invoke(compilationUnit);
            for (Object importDecl : imports) {
                Method getNameAsString = importDecl.getClass().getMethod("getNameAsString");
                String fqn = (String) getNameAsString.invoke(importDecl);
                if (!fqn.startsWith("java.lang.")) {
                    dependencies.add(simpleName(fqn));
                }
            }
        }

        Method findAll = typeDeclaration.getClass().getMethod("findAll", Class.class);
        Class<?> classOrInterfaceType = Class.forName("com.github.javaparser.ast.type.ClassOrInterfaceType");
        List<?> types = (List<?>) findAll.invoke(typeDeclaration, classOrInterfaceType);
        for (Object type : types) {
            Method getNameAsString = type.getClass().getMethod("getNameAsString");
            String name = (String) getNameAsString.invoke(type);
            if (isDependencyCandidate(name)) {
                dependencies.add(name);
            }
        }

        return dependencies.size();
    }

    private String extractPackage(String source) {
        Matcher matcher = PACKAGE_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractPrimaryTypeName(String source) {
        Matcher matcher = TYPE_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "UnknownType";
    }

    private String extractPrimaryTypeBody(String source) {
        Matcher matcher = TYPE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return "";
        }
        int braceOpen = source.indexOf('{', matcher.end());
        if (braceOpen < 0) {
            return "";
        }
        int depth = 0;
        for (int i = braceOpen; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(braceOpen + 1, i);
                }
            }
        }
        return source.substring(braceOpen + 1);
    }

    private int countMethodsFromSource(String body, String className) {
        int count = 0;
        Matcher matcher = METHOD_PATTERN.matcher(body);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!name.equals(className)) {
                count++;
            }
        }
        return count;
    }

    private int countFieldsFromSource(String body) {
        int count = 0;
        Matcher matcher = FIELD_PATTERN.matcher(body);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private int countDependenciesFromSource(String source, String body, String className) {
        Set<String> dependencies = new HashSet<>();
        Matcher importMatcher = IMPORT_PATTERN.matcher(source);
        while (importMatcher.find()) {
            String fqn = importMatcher.group(1);
            if (!fqn.startsWith("java.lang.")) {
                dependencies.add(simpleName(fqn));
            }
        }

        Matcher typeMatcher = CAPITALIZED_IDENTIFIER.matcher(body);
        while (typeMatcher.find()) {
            String name = typeMatcher.group(1);
            if (name.length() == 1) {
                continue;
            }
            if (name.equals(className)) {
                continue;
            }
            if (JAVA_LANG_TYPES.contains(name)) {
                continue;
            }
            dependencies.add(name);
        }

        return dependencies.size();
    }

    private boolean isDependencyCandidate(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (PRIMITIVES.contains(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return !JAVA_LANG_TYPES.contains(name);
    }

    private String simpleName(String fqn) {
        int idx = fqn.lastIndexOf('.');
        if (idx >= 0 && idx + 1 < fqn.length()) {
            return fqn.substring(idx + 1);
        }
        return fqn;
    }
}
