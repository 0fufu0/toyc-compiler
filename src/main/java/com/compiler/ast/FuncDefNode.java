package com.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 函数定义节点。
 */
public class FuncDefNode extends AstNode {

    public final ValueType returnType;
    public final String name;
    public final List<ParamNode> params;
    public final BlockStmtNode body;

    public FuncDefNode(ValueType returnType, String name, List<ParamNode> params, BlockStmtNode body) {
        this.returnType = returnType;
        this.name = name;
        this.params = Collections.unmodifiableList(new ArrayList<>(params));
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitFuncDef(this);
    }
}
