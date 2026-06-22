package com.compiler.ast;

/**
 * 表达式节点基类。B 在语义分析后可填写 {@link #constValue}。
 */
public abstract class ExprNode extends AstNode {

    /** 编译期常量折叠结果；null 表示非常量表达式。 */
    public Integer constValue = null;
}
