package com.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编译单元根节点，对应文法 {@code CompUnit}。
 */
public class CompUnitNode extends AstNode {

    /** 顶层声明与函数定义，保持源码顺序。元素为 {@link DeclNode} 或 {@link FuncDefNode}。 */
    public final List<AstNode> items;

    public CompUnitNode(List<AstNode> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCompUnit(this);
    }
}
