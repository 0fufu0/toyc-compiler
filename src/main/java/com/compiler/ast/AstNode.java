package com.compiler.ast;

/**
 * AST 节点基类。所有节点支持 Visitor 双分派，并预留源码位置信息供调试。
 */
public abstract class AstNode {

    public int line = -1;
    public int column = -1;

    public abstract <T> T accept(AstVisitor<T> visitor);
}
