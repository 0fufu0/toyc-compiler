package com.compiler.parser;

import com.compiler.ast.AstNode;
import com.compiler.ast.CompUnitNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * ToyC 前端解析入口（自定义 AST，不直接暴露 ANTLR ParseTree）。
 */
public final class ToyCFrontend {

    private ToyCFrontend() {
    }

    /**
     * 解析 ToyC 源码，返回 AST 根节点 {@link CompUnitNode}。
     *
     * @param source 完整源码字符串
     * @return AST 根节点
     * @throws ParseException 词法或语法错误
     */
    public static CompUnitNode parse(String source) {
        var lexer = new ToyCLexer(CharStreams.fromString(source));
        var tokens = new CommonTokenStream(lexer);
        var parser = new ToyCParser(tokens);

        var tree = parser.compUnit();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new ParseException("syntax error in ToyC source");
        }

        AstNode result = new AstBuilder().visitCompUnit(tree);
        return (CompUnitNode) result;
    }
}
