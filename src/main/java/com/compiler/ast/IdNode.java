package com.compiler.ast;

import com.compiler.semantic.Symbol;

/**
 * 标识符表达式节点。B 在语义分析后填写 {@link #symbolRef}。
 */
public class IdNode extends ExprNode {

    public final String name;
    public Symbol symbolRef;

    public IdNode(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitId(this);
    }
}
