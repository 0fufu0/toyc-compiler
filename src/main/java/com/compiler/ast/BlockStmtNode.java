package com.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 语句块节点，对应 {@code { Stmt* }}。
 * 块内可包含语句或局部声明（{@link DeclNode}）。
 */
public class BlockStmtNode extends StmtNode {

    public final List<AstNode> stmts;

    public BlockStmtNode(List<AstNode> stmts) {
        this.stmts = Collections.unmodifiableList(new ArrayList<>(stmts));
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBlockStmt(this);
    }
}
