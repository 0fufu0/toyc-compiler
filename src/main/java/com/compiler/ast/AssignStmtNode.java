package com.compiler.ast;

import com.compiler.semantic.Symbol;

/**
 * 赋值语句节点，对应 {@code ID = Expr ;}。
 */
public class AssignStmtNode extends StmtNode {

    public final String name;
    public final ExprNode value;
    public Symbol symbolRef;

    public AssignStmtNode(String name, ExprNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitAssignStmt(this);
    }
}
