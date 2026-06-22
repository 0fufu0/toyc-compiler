package com.compiler.ast;

/**
 * 表达式语句节点，对应 {@code Expr ;}。
 */
public class ExprStmtNode extends StmtNode {

    public final ExprNode expr;

    public ExprStmtNode(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitExprStmt(this);
    }
}
