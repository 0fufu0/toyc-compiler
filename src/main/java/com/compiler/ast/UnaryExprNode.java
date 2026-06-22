package com.compiler.ast;

/**
 * 一元表达式节点。
 */
public class UnaryExprNode extends ExprNode {

    public final UnaryOp op;
    public final ExprNode operand;

    public UnaryExprNode(UnaryOp op, ExprNode operand) {
        this.op = op;
        this.operand = operand;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
