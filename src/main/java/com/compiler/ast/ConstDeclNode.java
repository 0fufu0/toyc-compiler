package com.compiler.ast;

/**
 * 常量声明节点，对应 {@code const int ID = Expr ;}。
 */
public class ConstDeclNode extends DeclNode {

    public final String name;
    public final ExprNode init;

    public ConstDeclNode(String name, ExprNode init) {
        this.name = name;
        this.init = init;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitConstDecl(this);
    }
}
