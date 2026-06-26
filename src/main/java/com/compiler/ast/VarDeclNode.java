package com.compiler.ast;

import com.compiler.semantic.Symbol;

/**
 * 变量声明节点，对应 {@code int ID = Expr ;}。
 */
public class VarDeclNode extends DeclNode {

    public final String name;
    public final ExprNode init;
    public Symbol symbolRef;

    public VarDeclNode(String name, ExprNode init) {
        this.name = name;
        this.init = init;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVarDecl(this);
    }

}
