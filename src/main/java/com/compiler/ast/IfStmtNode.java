package com.compiler.ast;

/**
 * if-else 语句节点。
 */
public class IfStmtNode extends StmtNode {

    public final ExprNode condition;
    public final StmtNode thenBranch;
    /** 可为 null，表示无 else 分支。 */
    public final StmtNode elseBranch;

    public IfStmtNode(ExprNode condition, StmtNode thenBranch, StmtNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIfStmt(this);
    }
}
