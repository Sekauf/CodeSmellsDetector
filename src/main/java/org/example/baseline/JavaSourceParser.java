package org.example.baseline;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSourceParser {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("\\bpackage\\s+([\\w\\.]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\\b(class|interface|enum)\\s+([A-Za-z_\\$][\\w\\$]*)");

    public List<ParsedType> parseFiles(List<Path> javaFiles) throws IOException {
        List<ParsedType> results = new ArrayList<>();
        for (Path file : javaFiles) {
            results.addAll(parseFile(file));
        }
        return results;
    }

    public List<ParsedType> parseFile(Path javaFile) throws IOException {
        if (isJavaParserAvailable()) {
            return parseWithJavaParser(javaFile);
        }
        return parseWithFallback(javaFile);
    }

    private boolean isJavaParserAvailable() {
        try {
            Class.forName("com.github.javaparser.StaticJavaParser");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private List<ParsedType> parseWithJavaParser(Path javaFile) throws IOException {
        String source = Files.readString(javaFile, StandardCharsets.UTF_8);
        try {
            Class<?> staticParserClass = Class.forName("com.github.javaparser.StaticJavaParser");
            Method parseMethod = staticParserClass.getMethod("parse", String.class);
            Object compilationUnit = parseMethod.invoke(null, source);

            Method getPackageDeclaration = compilationUnit.getClass().getMethod("getPackageDeclaration");
            Optional<?> packageOptional = (Optional<?>) getPackageDeclaration.invoke(compilationUnit);
            String packageName = "";
            if (packageOptional.isPresent()) {
                Object packageDecl = packageOptional.get();
                Method getNameAsString = packageDecl.getClass().getMethod("getNameAsString");
                packageName = (String) getNameAsString.invoke(packageDecl);
            }

            Method findAll = compilationUnit.getClass().getMethod("findAll", Class.class);
            Class<?> typeDeclClass = Class.forName("com.github.javaparser.ast.body.TypeDeclaration");
            List<?> typeDecls = (List<?>) findAll.invoke(compilationUnit, typeDeclClass);

            List<ParsedType> parsed = new ArrayList<>();
            for (Object typeDecl : typeDecls) {
                String kind = resolveKind(typeDecl);
                if (kind == null) {
                    continue;
                }
                String fqn = resolveFullyQualifiedName(typeDecl, typeDeclClass, packageName);
                parsed.add(new ParsedType(fqn, kind, javaFile));
            }

            parsed.sort(Comparator.comparing(ParsedType::getFullyQualifiedName));
            return parsed;
        } catch (Exception ex) {
            throw new IOException("Failed to parse with JavaParser: " + javaFile, ex);
        }
    }

    private String resolveKind(Object typeDecl) throws Exception {
        String simple = typeDecl.getClass().getSimpleName();
        if ("EnumDeclaration".equals(simple)) {
            return "enum";
        }
        if ("ClassOrInterfaceDeclaration".equals(simple)) {
            Method isInterface = typeDecl.getClass().getMethod("isInterface");
            boolean isIface = (boolean) isInterface.invoke(typeDecl);
            return isIface ? "interface" : "class";
        }
        return null;
    }

    private String resolveFullyQualifiedName(Object typeDecl, Class<?> typeDeclClass, String packageName)
            throws Exception {
        try {
            Method getFullyQualifiedName = typeDecl.getClass().getMethod("getFullyQualifiedName");
            Optional<?> fqnOptional = (Optional<?>) getFullyQualifiedName.invoke(typeDecl);
            if (fqnOptional.isPresent()) {
                return (String) fqnOptional.get();
            }
        } catch (NoSuchMethodException ignored) {
            // Fall through to manual computation.
        }

        Method getNameAsString = typeDecl.getClass().getMethod("getNameAsString");
        String name = (String) getNameAsString.invoke(typeDecl);

        List<String> outers = new ArrayList<>();
        Method getParentNode = typeDecl.getClass().getMethod("getParentNode");
        Optional<?> parent = (Optional<?>) getParentNode.invoke(typeDecl);
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

    private List<ParsedType> parseWithFallback(Path javaFile) throws IOException {
        String source = Files.readString(javaFile, StandardCharsets.UTF_8);
        String packageName = "";
        Matcher packageMatcher = PACKAGE_PATTERN.matcher(source);
        if (packageMatcher.find()) {
            packageName = packageMatcher.group(1);
        }

        int[] depthAt = new int[source.length() + 1];
        int depth = 0;
        for (int i = 0; i < source.length(); i++) {
            depthAt[i] = depth;
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            }
        }

        List<ParsedType> parsed = new ArrayList<>();
        Deque<TypeEntry> stack = new ArrayDeque<>();
        String strippedSource = SourceUtils.stripCommentsAndStrings(source);
        Matcher typeMatcher = TYPE_PATTERN.matcher(strippedSource);
        while (typeMatcher.find()) {
            String kind = typeMatcher.group(1).toLowerCase(Locale.ROOT);
            String name = typeMatcher.group(2);
            int braceIndex = source.indexOf('{', typeMatcher.end());
            if (braceIndex < 0) {
                continue;
            }
            int openDepth = depthAt[braceIndex];
            while (!stack.isEmpty() && stack.peek().depth >= openDepth) {
                stack.pop();
            }
            String fqn = buildFqn(packageName, stack, name);
            parsed.add(new ParsedType(fqn, kind, javaFile));
            stack.push(new TypeEntry(name, openDepth));
        }

        parsed.sort(Comparator.comparing(ParsedType::getFullyQualifiedName));
        return parsed;
    }

    private String buildFqn(String packageName, Deque<TypeEntry> stack, String name) {
        List<String> parts = new ArrayList<>();
        for (TypeEntry entry : stack) {
            parts.add(0, entry.name);
        }
        parts.add(name);
        String joined = String.join(".", parts);
        if (packageName == null || packageName.isEmpty()) {
            return joined;
        }
        return packageName + "." + joined;
    }

    private static class TypeEntry {
        private final String name;
        private final int depth;

        private TypeEntry(String name, int depth) {
            this.name = name;
            this.depth = depth;
        }
    }
}
