package com.compiler.ast;

/**
 * return 语句节点。{@code value} 为 null 表示 {@code return ;}（void 函数）。
 */
public class ReturnStmtNode extends StmtNode {

    public final ExprNode value;

    public ReturnStmtNode(ExprNode value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitReturnStmt(this);
    }
}
