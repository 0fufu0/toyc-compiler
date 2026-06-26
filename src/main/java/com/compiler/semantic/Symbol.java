package com.compiler.semantic;

import java.util.ArrayList;
import java.util.List;

import com.compiler.ast.ValueType;

/**
 * 符号种类。 ToyC 里没有 class，所以不保留 CLASS。
 */
enum SymbolType {
    VARIABLE, // 普通变量
    CONSTANT, // const 常量
    FUNCTION, // 函数
    PARAMETER   // 函数形参
}

public class Symbol {

    public String name;          // 符号名
    public ValueType type;       // 变量/常量/参数：int；函数：返回类型 int/void
    public String irName;

    public SymbolType symbolType;

    public boolean isConst;      // 是否为 const 常量
    public boolean isGlobal;     // 是否为全局符号
    public Integer constValue;   // 常量的编译期值，非 const 时为 null

    // 函数专用：参数类型列表
    public List<ValueType> paramTypes;

    /**
     * 占位构造器：给 AST 节点的 symbolRef 等地方使用。
     */
    public Symbol(String name) {
        this(name, SymbolType.VARIABLE, ValueType.INT);
    }

    /**
     * 通用构造器。
     */
    public Symbol(String name, SymbolType symbolType, ValueType type) {
        this.name = name;
        this.symbolType = symbolType;
        this.type = type;
        this.irName = name;
        this.isConst = symbolType == SymbolType.CONSTANT;
        this.isGlobal = false;
        this.constValue = null;

        this.paramTypes = new ArrayList<>();
    }

    /**
     * 创建变量符号。
     */
    public static Symbol variable(String name, boolean isGlobal) {
        Symbol symbol = new Symbol(name, SymbolType.VARIABLE, ValueType.INT);
        symbol.isGlobal = isGlobal;
        return symbol;
    }

    /**
     * 创建常量符号。
     */
    public static Symbol constant(String name, boolean isGlobal, int constValue) {
        Symbol symbol = new Symbol(name, SymbolType.CONSTANT, ValueType.INT);
        symbol.isGlobal = isGlobal;
        symbol.isConst = true;
        symbol.constValue = constValue;
        return symbol;
    }

    /**
     * 创建形参符号。
     */
    public static Symbol parameter(String name) {
        return new Symbol(name, SymbolType.PARAMETER, ValueType.INT);
    }

    /**
     * 创建函数符号。
     */
    public static Symbol function(String name, ValueType returnType, List<ValueType> paramTypes) {
        Symbol symbol = new Symbol(name, SymbolType.FUNCTION, returnType);
        symbol.isGlobal = true;

        if (paramTypes != null) {
            symbol.paramTypes = new ArrayList<>(paramTypes);
        }

        return symbol;
    }
}
