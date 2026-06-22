package com.compiler.ast;

/**
 * 二元表达式节点。
 */
public class BinaryExprNode extends ExprNode {

    public final BinOp op;
    public final ExprNode left;
    public final ExprNode right;

    public BinaryExprNode(BinOp op, ExprNode left, ExprNode right) {
        this.op = op;
        this.left = left;
        this.right = right;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
