package com.compiler.ast;

/**
 * break 语句节点。
 */
public class BreakStmtNode extends StmtNode {

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBreakStmt(this);
    }
}
