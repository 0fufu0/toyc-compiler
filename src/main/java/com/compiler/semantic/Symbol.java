package com.compiler.semantic;

import com.compiler.ast.ValueType;

/**
 * 符号表条目占位定义，供 AST 节点 {@code symbolRef} 字段引用。
 * 成员 B 将在语义分析阶段扩展此类的完整字段与行为。
 */
public class Symbol {

    public String name;
    public ValueType type;
    public boolean isConst;
    public boolean isGlobal;
    public Integer constValue;

    public Symbol(String name) {
        this.name = name;
    }
}
