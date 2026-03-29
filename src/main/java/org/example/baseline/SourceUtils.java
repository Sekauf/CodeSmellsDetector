package org.example.baseline;

/**
 * Utility methods for Java source text processing.
 */
class SourceUtils {

    private SourceUtils() {
        // utility class
    }

    /**
     * Returns a copy of {@code source} where block comments, line comments,
     * and string literals are replaced with whitespace characters of equal
     * length so that all character offsets remain stable.
     *
     * @param source raw Java source text
     * @return source text with comments and string literals blanked out
     */
    static String stripCommentsAndStrings(String source) {
        char[] chars = source.toCharArray();
        int i = 0;
        while (i < chars.length) {
            if (i + 1 < chars.length && chars[i] == '/' && chars[i + 1] == '*') {
                // Block comment (/* ... */ and /** ... */)
                chars[i] = ' ';
                chars[i + 1] = ' ';
                i += 2;
                while (i < chars.length) {
                    if (i + 1 < chars.length && chars[i] == '*' && chars[i + 1] == '/') {
                        chars[i] = ' ';
                        chars[i + 1] = ' ';
                        i += 2;
                        break;
                    }
                    if (chars[i] != '\n' && chars[i] != '\r') {
                        chars[i] = ' ';
                    }
                    i++;
                }
            } else if (i + 1 < chars.length && chars[i] == '/' && chars[i + 1] == '/') {
                // Line comment (// ...)
                chars[i] = ' ';
                chars[i + 1] = ' ';
                i += 2;
                while (i < chars.length && chars[i] != '\n' && chars[i] != '\r') {
                    chars[i] = ' ';
                    i++;
                }
            } else if (chars[i] == '"') {
                // String literal
                chars[i] = ' ';
                i++;
                while (i < chars.length && chars[i] != '"' && chars[i] != '\n') {
                    if (chars[i] == '\\' && i + 1 < chars.length) {
                        chars[i] = ' ';
                        i++;
                        chars[i] = ' ';
                    } else {
                        chars[i] = ' ';
                    }
                    i++;
                }
                if (i < chars.length && chars[i] == '"') {
                    chars[i] = ' ';
                    i++;
                }
            } else {
                i++;
            }
        }
        return new String(chars);
    }
}
