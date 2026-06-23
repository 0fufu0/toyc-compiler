package com.compiler.parser;

/**
 * 词法/语法分析失败时抛出。
 */
public class ParseException extends RuntimeException {

    public ParseException(String message) {
        super(message);
    }
}
