package com.compiler.ast;

import com.compiler.semantic.Symbol;

/**
 * 函数形参节点，对应 {@code int ID}。
 */
public class ParamNode extends AstNode {

    public Symbol symbolRef;
    public final String name;

    public ParamNode(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitParam(this);
    }
}
