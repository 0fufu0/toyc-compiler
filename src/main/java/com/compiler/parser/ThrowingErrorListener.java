package com.compiler.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * ANTLR 错误监听器：遇到首个语法/词法错误时立即抛出 {@link ParseException}。
 */
final class ThrowingErrorListener extends BaseErrorListener {

    static final ThrowingErrorListener INSTANCE = new ThrowingErrorListener();

    private ThrowingErrorListener() {
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        throw new ParseException(line, charPositionInLine + 1, msg);
    }
}
