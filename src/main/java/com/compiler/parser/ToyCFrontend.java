package com.compiler.parser;

import com.compiler.ast.CompUnitNode;

/**
 * ToyC 前端解析入口（自定义 AST，不直接暴露 ANTLR ParseTree）。
 * <p>
 * 第 1 天：仅提供契约桩；第 2 天起由 {@link AstBuilder} 调用 ANTLR 生成的
 * {@link com.compiler.parser.ToyCParser} 并完成 AST 构建。
 */
public final class ToyCFrontend {

    private ToyCFrontend() {
    }

    /**
     * 解析 ToyC 源码，返回 AST 根节点 {@link CompUnitNode}。
     *
     * @param source 完整源码字符串
     * @return AST 根节点；当前为桩实现，固定返回 {@code null}
     */
    public static CompUnitNode parse(String source) {
        // TODO(day2): ToyCLexer + ToyCParser(ANTLR) + AstBuilder
        return null;
    }
}
