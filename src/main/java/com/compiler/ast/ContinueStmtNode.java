package com.compiler.ast;

/**
 * continue 语句节点。
 */
public class ContinueStmtNode extends StmtNode {

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitContinueStmt(this);
    }
}
