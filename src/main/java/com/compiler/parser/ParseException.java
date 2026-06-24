package com.compiler.parser;

/**
 * 词法/语法分析失败时抛出，携带源码行列号。
 */
public class ParseException extends RuntimeException {

    public final int line;
    public final int column;

    public ParseException(int line, int column, String message) {
        super(format(line, column, message));
        this.line = line;
        this.column = column;
    }

    public ParseException(String message) {
        this(-1, -1, message);
    }

    private static String format(int line, int column, String message) {
        if (line > 0 && column > 0) {
            return "parse error at " + line + ":" + column + ": " + message;
        }
        return "parse error: " + message;
    }
}
