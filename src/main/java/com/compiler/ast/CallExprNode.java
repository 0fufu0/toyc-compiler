package com.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 函数调用表达式节点，对应 {@code ID ( Expr* )}。
 */
public class CallExprNode extends ExprNode {

    public final String funcName;
    public final List<ExprNode> args;

    public CallExprNode(String funcName, List<ExprNode> args) {
        this.funcName = funcName;
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCallExpr(this);
    }
}
