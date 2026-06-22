package com.compiler.ast;

/**
 * 空语句节点，对应 {@code ;}。
 */
public class EmptyStmtNode extends StmtNode {

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitEmptyStmt(this);
    }
}
