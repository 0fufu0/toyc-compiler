package com.compiler.semantic;

import java.util.ArrayList;
import java.util.List;

import com.compiler.ast.ValueType;

/**
 * 符号种类。
 * ToyC 里没有 class，所以不要保留 CLASS。
 */
enum SymbolType {
    VARIABLE,   // 普通变量
    CONSTANT,   // const 常量
    FUNCTION,   // 函数
    PARAMETER   // 函数形参
}

/**
 * 兼容旧代码用。
 * 后续建议统一使用 com.compiler.ast.ValueType。
 */
enum Type {
    INT,
    VOID
}

public class Symbol {

    // ===== 推荐以后主要使用这些字段 =====
    public String name;          // 符号名
    public ValueType type;       // 变量/常量/参数：int；函数：返回类型 int/void
    public boolean isConst;      // 是否为 const 常量
    public boolean isGlobal;     // 是否为全局符号
    public Integer constValue;   // 常量的编译期值，非 const 时为 null

    // ===== 符号分类 =====
    public SymbolType symbolType;

    // ===== 兼容你原来那份代码的字段 =====
    public String symbolName;
    public Type dataType;

    // ===== 函数专用：参数类型列表 =====
    public List<ValueType> paramTypes;

    /**
     * 占位构造器：保留 HEAD 版本的用法。
     */
    public Symbol(String name) {
        this(name, SymbolType.VARIABLE, ValueType.INT);
    }

    /**
     * 推荐使用的构造器。
     */
    public Symbol(String name, SymbolType symbolType, ValueType type) {
        this.name = name;
        this.symbolName = name;

        this.symbolType = symbolType;

        this.type = type;
        this.dataType = toLegacyType(type);

        this.isConst = symbolType == SymbolType.CONSTANT;
        this.isGlobal = false;
        this.constValue = null;

        this.paramTypes = new ArrayList<>();
    }

    /**
     * 兼容旧代码的构造器：
     * new Symbol(name, SymbolType.VARIABLE, Type.INT)
     */
    public Symbol(String symbolName, SymbolType symbolType, Type dataType) {
        this(symbolName, symbolType, toValueType(dataType));
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

    private static ValueType toValueType(Type type) {
        if (type == Type.VOID) {
            return ValueType.VOID;
        }
        return ValueType.INT;
    }

    private static Type toLegacyType(ValueType type) {
        if (type == ValueType.VOID) {
            return Type.VOID;
        }
        return Type.INT;
    }
}