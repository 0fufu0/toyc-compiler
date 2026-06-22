package com.compiler.ast;

/**
 * 整数字面量节点。
 */
public class IntLiteralNode extends ExprNode {

    public final int value;

    public IntLiteralNode(int value) {
        this.value = value;
        this.constValue = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIntLiteral(this);
    }
}
